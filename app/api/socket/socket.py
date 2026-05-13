from fastapi import APIRouter
from sqlalchemy.ext.asyncio import AsyncSession
from typing import Annotated

from app.db.database import get_session
from app.api.socket.crud import create_message
from app.api.socket.server import sio


router = APIRouter(prefix='/socket', tags=['socket'])



@sio.on('send_message')
async def send_message_handler(sid, data):
    print("HANDLER CALLED", sid, data)
    user_data = await sio.get_session(sid)
    print("SESSION:", user_data)
    if not user_data:
        return
    user_id = user_data.get('user_id')
    if not user_id:
        return
    chat_id = data.get('room_id')
    message = data.get('message', {})
    try:
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
            'chat_id': str(new_message.chat_id),
            'content': new_message.content,
            'msg_type': new_message.msg_type.value,
            'reply_to_id': str(new_message.reply_to_id) if new_message.reply_to_id else None,
            'created_at': new_message.created_at.isoformat()
        }

        await sio.emit(event='receive_message', data=data, room=str(chat_id))
    except Exception as e:
        raise e


@sio.on('join_chat')  # TODO
async def join_chat_handler(sid, room_id):
    await sio.enter_room(sid, room_id)


@sio.on('leave_chat')  # TODO
async def leave_room_handler(sid, room_id):
   await sio.leave_room(sid, room_id)
