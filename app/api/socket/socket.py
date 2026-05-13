import socketio
from datetime import datetime
from socketio.exceptions import ConnectionError
from fastapi import APIRouter, Depends
from fastapi.requests import Request
from sqlalchemy.ext.asyncio import AsyncSession
from wireup import Injected

from app.api.auth.dependencies import get_current_user
from app.db.database import get_session
from app.api.socket.crud import create_message
from app.api.socket.schemas import MessageSend
from app.api.socket.server import sio


router = APIRouter(prefix='/socket', tags=['socket'])


@router.post('/chats/{chat_id}/messages')  # TODO
async def send_message_handler(message: MessageSend,
                               chat_id: int,
                               user_data: Injected[get_current_user],
                               session: Injected[get_session]):
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





@sio.on('join_chat')  # TODO
async def join_chat_handler(sid, room_id):
    sio.enter_room(sid, room_id)


@sio.on('leave_room')  # TODO
async def leave_room_handler(sid, room_id):
    sio.leave_room(sid, room_id)
