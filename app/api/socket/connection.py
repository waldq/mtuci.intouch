from datetime import datetime

from app.db.database import get_session
from app.api.auth.dependencies import get_current_user
from app.api.users.crud import get_user_base_by_id, get_user_auth_by_id, get_user_public_by_id
from app.api.socket.server import sio
from app.dep_inj import container


#Обработчик события "connect".
@sio.on('connect')  # TODO
async def connect_handler(sid, environ, auth):
    token = auth.get('token') if auth else None
    if not token:
        return False
    try:
        user_data = await container.get()
        user_id = user_data.get('user_id')
        async with get_session() as session:
            user = await get_user_public_by_id(user_id, session)

            await sio.save_session(sid, {'username': user.username, 'user_id': user_id})

    except Exception as e:
        return False
    
@sio.on('disconnect')  # TODO
async def disconnect_handler(sid):
    async with sio.session(sid) as session_data:
        user_id = session_data.get('user_id')

    if user_id:
        async with get_session() as session:
            user = await get_user_public_by_id(user_id, session)
            user.last_seen_date = datetime.now()

            session.add(user)
            await session.commit()