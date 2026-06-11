from datetime import datetime
from typing import Annotated
import redis.asyncio as redis
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.database import socketio_get_db_session
from app.api.auth.dependencies import validate_socket_token
from app.api.users.crud import get_user_public_by_id
from app.api.socket.server import sio
from app.redis_client import RedisClient


#Обработчик события "connect".
@sio.on('connect')  # TODO
async def connect_handler(sid, environ, auth):
    token = auth.get('token') if auth else None
    if not token:
        return False

    try:
        redis_client = await RedisClient.get_client()
        
        try:
            user_data = await validate_socket_token(redis_client, token)
        finally:
            await redis_client.aclose()

        if not user_data:
            return False

        user_id = user_data.get('user_id')

        if not user_id:
            return False

        async with socketio_get_db_session() as session:
            user = await get_user_public_by_id(user_id, session)

        session_payload = {
            'username': user.username,
            'user_id': user_id
        }
        await sio.save_session(sid, session_payload, namespace='/')

        return True

    except Exception as e:
        return False
    
@sio.on('disconnect')  # TODO
async def disconnect_handler(sid):
    try:
        async with sio.session(sid) as session_data:
            user_id = session_data.get('user_id')

        if user_id:
            async with socketio_get_db_session() as session:
                user = await get_user_public_by_id(user_id, session)
                user.last_seen_date = datetime.now()

                session.add(user)
                await session.commit()
    except Exception as e:
        raise e
