from fastapi import APIRouter

from app.db.database import get_session
from app.api.socket.crud import create_message
from app.api.socket.server import sio
from app.api.users.crud import (
                        search_user_public_by_username_or_tag, 
                        update_user,
                        get_user_public_by_id
                                )

from app.api.users.schemas import UserUpdatePublic


router = APIRouter(prefix='/socket', tags=['socket'])


@sio.on('join_chat')  # TODO
async def join_chat_handler(sid, room_id):
    await sio.enter_room(sid, room_id)


@sio.on('leave_chat')  # TODO
async def leave_room_handler(sid, room_id):
   await sio.leave_room(sid, room_id)


@sio.on('send_message')
async def send_message_handler(sid, data):
    print("HANDLER CALLED", sid, data)
    user_data = await sio.get_session(sid)
    print("SESSION:", user_data)
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
        async for session in get_session():
            new_message = await create_message(
                sender_id=user_id,
                chat_id=chat_id,
                content=message.get('content'),
                msg_type=message.get('msg_type'),
                reply_to_id=message.get('reply_to_id'),
                session=session
            )
            break

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
    except Exception as e:
        raise e
    

@sio.on('search_user')
async def search_user_handler(sid, data):
    username_or_tag = data.get('username_or_tag')
    if username_or_tag:
        try:
            async with get_session() as session:
                results = await search_user_public_by_username_or_tag(session, username_or_tag)
                results_dict = [
                    {key: value for key, value in userpublic.__dict__.items() if key != '_sa_instance_state'} for userpublic in results
                ]
                sio.emit('search_user_results', results_dict, to=sid)
        except Exception as e:
            raise e
    await sio.emit('search_user_results', {'results': 'None.'}, to=sid)

@sio.on('update_user')
async def update_user_handler(sid, data: UserUpdatePublic):
    if not isinstance(data, UserUpdatePublic):
        await sio.emit('failed_update_user', {'result': 'Failure. Invalid data.'}, to=sid)
    session_data = sio.get_session(sid)
    user_id = session_data.get('user_id')
    if data:
        async with get_session() as session:
            user = await get_user_public_by_id(user_id, session)
            if user:
                results = await update_user(data, user, session)
                await sio.emit('success_update_user', results, to=sid)
            await sio.emit('failed_update_user', {'result': 'Failure. No such id.'}, to=sid)
    await sio.emit('failed_update_user', {'result': 'Failure. No data.'}, to=sid)
