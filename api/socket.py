import socketio
import uuid
from datetime import datetime
from socketio.exceptions import ConnectionError
from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession
from fastapi.requests import Request

from app.dependencies import get_current_user
from app.database import get_session, engine
from crud.user import get_user_by_id
from crud.message import create_message
from schemas.message import MessageSend

sio = socketio.AsyncServer()

router = APIRouter(prefix='/socket', tags=['socket'])

@sio.on('connect')
async def connect_handler(sid, environ, auth):
    token = auth.get('token') if auth else None
    if not token:
        return False
    try:
        user_data = await get_current_user(token=token)
        user_id = user_data.get('user_id')
        async with get_session() as session:
            user = await get_user_by_id(user_id, session)

            await sio.save_session(sid, {'username': user.username, 'user_id': user_id})

    except Exception as e:
        return False

@router.post('/chats/{chat_id}/messages')
async def send_message_handler(message: MessageSend, 
                                chat_id: uuid.UUID,
                                user_data = Depends(get_current_user),
                                session: AsyncSession = Depends(get_session)):
    user_id = user_data.get('user_id')
    try:
            new_message = await create_message(sender_id=user_id, 
                                             chat_id=chat_id, 
                                             content=message.content, 
                                             msg_type=message.msg_type, 
                                             reply_to_id=message.reply_to_id, 
                                             session=session)
            
            data = {
                'id': str(new_message.id),
                'sender_id': str(new_message.sender_id), 
                'chat_id': str(new_message.chat_id),
                'content': new_message.content,
                'msg_type': new_message.msg_type.value,
                'reply_to_id': str(new_message.reply_to_id) if new_message.reply_to_id else None,
                'created_at': new_message.created_at.isoformat()
            }
            
            await sio.emit(event='receive_message', data=data, room=str(chat_id))
    except Exception as e:
        raise e

@sio.on('disconnect')
async def disconnect_handler(sid):
    async with sio.session(sid) as session_data:
        user_id = session_data.get('user_id')

    if user_id:
        async with get_session() as session:
            user = await get_user_by_id(user_id, session)
            user.last_seen_date = datetime.now()

            session.add(user)
            await session.commit()

@sio.on('join_chat')
async def join_chat_handler(sid, room_id):
    sio.enter_room(sid, room_id)

@sio.on('leave_room')
async def leave_room_handler(sid, room_id):
    sio.leave_room(sid, room_id)