from wireup import Injected
import redis.asyncio as redis
from cryptography.hazmat.primitives.asymmetric import ec
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.socket.server import sio
from app.api.socket.schemas import FHMQVStep1
from app.redis_client import get_redis, store_client_public_static_key, store_server_public_static_key
from app.core.config import settings
from app.db.database import get_session
from app.api.users.crud import update_user_pub_key


@sio.on('fhmqv_step_1')
async def step_1_handler(sid,
                         data: FHMQVStep1,
                         redis_client: Injected[get_redis],
                         session: Injected[get_session]):
    client_public_static_key = data.client_public_static_key
    try:
        ec.EllipticCurvePublicKey.from_encoded_point(ec.SECP256R1, client_public_static_key.encode())
        await store_client_public_static_key(redis_client, sid, client_public_static_key, )
        # save_key_result = await update_user_pub_key(user_id, client_public_static_key, session) В последнюю очередь.
        server_private_static_key = settings.get_random_private_key()
        server_public_static_key = ec.derive_private_key(
            int(server_private_static_key, 16), 
            ec.SECP256R1)
        await store_server_public_static_key(
            redis_client, 
            sid, 
            server_public_static_key)
    except:
        raise ValueError('Incorrect key.')