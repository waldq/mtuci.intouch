from fastapi import APIRouter
from fastapi.encoders import jsonable_encoder

from app.db.database import socketio_get_db_session
from app.api.socket.crud import (
                        create_message,
                        update_message,
                        read_chat_messages,
                                )
        
from app.api.socket.server import sio
from app.api.users.crud import (
                        search_user_public_by_username_or_tag, 
                        update_user,
                        get_user_public_by_id
                                )

from app.api.users.schemas import UserUpdatePublic
from app.api.socket.schemas import MessageUpdate


router = APIRouter(prefix='/socket', tags=['socket'])


@sio.on('join_chat')  # TODO
async def join_chat_handler(sid, room_id):
    print("HANDLER CALLED", sid)
    user_data = await sio.get_session(sid)
    if not user_data or not user_data.get('user_id'):
        await sio.emit('error', {'message': 'User not authenticated'}, to=sid)
        return
    
    await sio.enter_room(sid, room_id)
    
    # Оповестить других участников
    await sio.emit('user_joined', {
        'user_id': str(user_data['user_id']),
        'username': user_data.get('username'),
        'room_id': str(room_id)
    }, room=str(room_id), skip_sid=sid)
    
    # Отправить подтверждение пользователю
    await sio.emit('joined_chat', {
        'room_id': str(room_id),
        'success': True
    }, to=sid)


@sio.on('leave_chat')  # TODO
async def leave_room_handler(sid, room_id):
    print("HANDLER CALLED", sid)
    user_data = await sio.get_session(sid)
    if not user_data or not user_data.get('user_id'):
        await sio.emit('error', {'message': 'User not authenticated'}, to=sid)
        return
    
    await sio.leave_room(sid, room_id)

    # Оповестить других участников
    await sio.emit('user_left', {
        'user_id': str(user_data['user_id']),
        'username': user_data.get('username'),
        'room_id': str(room_id)
    }, room=str(room_id), skip_sid=sid)

    # Предложить вернуться в чат
    await sio.emit('left_chat', {
        'room_id': str(room_id),
        'success': True
    }, to=sid)


@sio.on('send_message')
async def send_message_handler(sid, data):
    user_data = await sio.get_session(sid)

    if not user_data:
        return
    
    user_id = user_data.get('user_id')
    user_username = user_data.get('username')

    if not user_id:
        return
    
    chat_id = data.get('room_id')
    message = data.get('message', {})

    try:
        new_message = None
        async with socketio_get_db_session() as session:
            new_message = await create_message(
                sender_id=user_id,
                chat_id=chat_id,
                content=message.get('content'),
                msg_type=message.get('msg_type'),
                reply_to_id=message.get('reply_to_id'),
                session=session
            )

        data = {
            'id': str(new_message.id),
            'sender_id': str(new_message.sender_id),
            'sender_username': user_username,
            'chat_id': str(new_message.chat_id),
            'content': new_message.content,
            'msg_type': new_message.msg_type.value,
            'reply_to_id': str(new_message.reply_to_id) if new_message.reply_to_id else None,
            'created_at': new_message.created_at.isoformat()
        }

        await sio.emit(event='receive_message', data=data, room=str(chat_id))
        return
    except Exception as e:
        print(e)
        await sio.emit(event='receive_message', data={'results': 'Failure.'}, to=sid)
    

@sio.on('search_user')
async def search_user_handler(sid, data):
    username_or_tag = data.get('username_or_tag', '').strip()
    if username_or_tag:
        try:
            async with socketio_get_db_session() as session:
                results = await search_user_public_by_username_or_tag(session, username_or_tag)
                results_list = list(results)
                if results_list:
                    results_dict = jsonable_encoder(results_list)
                await sio.emit('search_user_results', results_dict, to=sid)
                return
        except Exception as e:
            print(e)
            await sio.emit('search_user_results', {'error': 'Internal server error.'}, to=sid)
            return
    await sio.emit('search_user_results', {'results': 'None.'}, to=sid)


@sio.on('update_user')
async def update_user_handler(sid, data: UserUpdatePublic):
    if not isinstance(data, UserUpdatePublic):
        await sio.emit('update_user_result', {'result': 'Failure. Invalid data.'}, to=sid)
    session_data = sio.get_session(sid)
    user_id = session_data.get('user_id')
    if data:
        async with socketio_get_db_session() as session:
            user = await get_user_public_by_id(user_id, session)
            if user:
                results = await update_user(data, user, session)
                await sio.emit('update_user_result', results, to=sid)
            await sio.emit('update_user_result', {'result': 'Failure. No such id.'}, to=sid)
    await sio.emit('update_user_result', {'result': 'Failure. No data.'}, to=sid)


@sio.on('edit_message')
async def edit_message_handler(sid, data: MessageUpdate):
    if not isinstance(data, MessageUpdate):
        await sio.emit('edit_message_result', {'result': 'Failure. Invalid data.'}, to=sid)
    session_data = sio.get_session(sid)
    user_id = session_data.get('user_id')
    user_username = session_data.get('username')
    room_id = data.get('chat_id')
    if data:
        async with socketio_get_db_session() as session:
            updated_message = await update_message(
                data.get('message_id'),
                data.get('new_content'),
                session
                )
            result_data = {
            'id': str(updated_message.id),
            'sender_id': str(updated_message.sender_id),
            'sender_username': user_username,
            'chat_id': str(updated_message.chat_id),
            'content': updated_message.content,
            'msg_type': updated_message.msg_type.value,
            'reply_to_id': str(updated_message.reply_to_id) if updated_message.reply_to_id else None,
            'created_at': updated_message.created_at.isoformat()
            }
            await sio.emit('edit_message_result', result_data, room=room_id)
    await sio.emit('edit_message_result', {'result': 'Failure. No data.'}, to=sid)


@sio.on('get_chat_messages')
async def get_chat_messages_handler(sid, data):
    chat_id = data.get('chat_id')
    if chat_id:
        async with socketio_get_db_session() as session:
            results = await read_chat_messages(chat_id, session)
            await sio.emit('chat_messages_result', results, to=sid)
    await sio.emit('chat_messages_result', {'result': 'Failure. Invalid data.'}, to=sid)