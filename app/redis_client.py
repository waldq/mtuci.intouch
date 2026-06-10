import redis.asyncio as redis
from typing import Any, AsyncGenerator

from app.core.config import settings


class RedisClient:
    _pool = None

    @classmethod
    async def get_pool(cls):
        if cls._pool is None:
            cls._pool = redis.ConnectionPool.from_url(
                settings.REDIS_URL, max_connections=20, decode_responses=True)
        return cls._pool

    @classmethod
    async def get_client(cls):
        pool = await cls.get_pool()
        return redis.Redis(connection_pool=pool, decode_responses=True)


async def get_redis() -> AsyncGenerator[redis.Redis, Any]:
    client = await RedisClient.get_client()
    try:
        yield client
    finally:
        await client.aclose()


def get_access_key(user_id: str, session_id: str) -> str:
    return f"access:{user_id}:{session_id}"


def get_refresh_key(jti: str) -> str:
    return f"refresh:{jti}"


def get_sessions_key(user_id: str) -> str:
    return f"sessions:{user_id}"

def get_client_public_static_key(session_id: str) -> str:
    return f"handshake:client:public:static:key:{session_id}"

def get_client_public_ephem_key(session_id: str) -> str:
    return f"handshake:client:public:ephem:key:{session_id}"

def get_server_private_static_key(session_id: str) -> str:
    return f"handshake:server:private:static:{session_id}"

def get_server_public_static_key(session_id: str) -> str:
    return f"handshake:server:public:static:{session_id}"

def get_server_private_ephem_key(session_id: str) -> str:
    return f"handshake:server:private:ephem:{session_id}"

def get_server_public_ephem_key(session_id: str) -> str:
    return f"handshake:server:public:ephem:{session_id}"

def get_handshake_start_key(session_id: str) -> str:
    return f"handshake:start:key:{session_id}"

def get_k1_key(session_id: str) -> str:
    return f"handshake:k1:{session_id}"

def get_sigma_b_bytes_hex(session_id: str) -> str:
    return f"handshake:sigmab:{session_id}"

async def store_tokens(
    redis_client: redis.Redis,
    user_id: str,
    session_id: str,
    access_token: str,
    refresh_jti: str
) -> None:

    access_key = get_access_key(user_id, session_id)
    await redis_client.setex(
        access_key,
        settings.ACCESS_TOKEN_EXPIRE_TIME * 60,
        access_token
    )

    refresh_key = get_refresh_key(refresh_jti)
    await redis_client.setex(
        refresh_key,
        settings.REFRESH_TOKEN_EXPIRE_TIME * 24 * 3600,
        f"{user_id}:{session_id}"
    )

    # Добавляем session_id в список сессий пользователя
    sessions_key = get_sessions_key(user_id)
    redis_client.sadd(sessions_key, session_id)
    await redis_client.expire(
        sessions_key, settings.REFRESH_TOKEN_EXPIRE_TIME * 24 * 3600
    )

async def store_client_public_static_key(
    redis_client: redis.Redis,
    session_id: str,
    client_public_static_key_bytes_hex: str
    ):
    public_key = get_client_public_static_key(session_id)
    await redis_client.setex(
        public_key,
        30,
        client_public_static_key_bytes_hex
    )

async def store_client_public_ephem_key(
    redis_client: redis.Redis,
    session_id: str,
    client_public_ephem_key_bytes_hex: str
    ):
    public_key = get_client_public_ephem_key(session_id)
    await redis_client.setex(
        public_key,
        30,
        client_public_ephem_key_bytes_hex
    )

async def store_server_private_static_key(
    redis_client: redis.Redis,
    session_id: str,
    server_private_static_key_hex: str
    ):
    private_key = get_server_private_static_key(session_id)
    await redis_client.setex(
        private_key,
        30,
        server_private_static_key_hex
    )

async def store_server_public_static_key(
    redis_client: redis.Redis,
    session_id: str,
    server_public_static_key_bytes_hex: str
    ):
    public_key = get_server_public_static_key(session_id)
    await redis_client.setex(
        public_key,
        30,
        server_public_static_key_bytes_hex
    )

async def store_server_private_ephem_key(
    redis_client: redis.Redis,
    session_id: str,
    server_private_ephem_key_hex: str
    ):
    private_key = get_server_public_ephem_key(session_id)
    await redis_client.setex(
        private_key,
        30,
        server_private_ephem_key_hex
    )

async def store_server_public_ephem_key(
    redis_client: redis.Redis,
    session_id: str,
    server_public_ephem_key_bytes_hex: str
    ):
    public_key = get_server_public_ephem_key(session_id)
    await redis_client.setex(
        public_key,
        30,
        server_public_ephem_key_bytes_hex
    )

async def store_handshake_first_key(
    redis_client: redis.Redis,
    session_id: str,
    handshake_first_key_bytes_hex: str    
    ):
    handshake_key = get_handshake_start_key(session_id)
    await redis_client.setex(
        handshake_key,
        30,
        handshake_first_key_bytes_hex
    )

async def store_k1_key(
    redis_client: redis.Redis,
    session_id: str,
    k1_bytes_hex: str    
    ):
    k1_key = get_k1_key(session_id)
    await redis_client.setex(
        k1_key,
        30,
        k1_bytes_hex
    )

async def store_sigma_b_bytes_hex(
    redis_client: redis.Redis,
    session_id: str,
    sigma_b_bytes_hex: str    
    ):
    sigma_b = get_sigma_b_bytes_hex(session_id)
    await redis_client.setex(
        sigma_b,
        30,
        sigma_b_bytes_hex
    )

async def get_refresh_token_data(
    redis_client: redis.Redis,
    jti: str
) -> tuple[str, str] | None:
    """По jti получает (user_id, session_id) из Redis"""
    refresh_key = get_refresh_key(jti)
    data = await redis_client.get(refresh_key)
    if data:
        parts = data.split(":")
        return parts[0], parts[1]
    return None


async def check_access_token_in_redis(
    redis_client: redis.Redis,
    user_id: str,
    session_id: str
) -> bool:
    """Проверяет, существует ли access токен в Redis (не отозван ли)"""
    access_key = get_access_key(user_id, session_id)
    return await redis_client.exists(access_key) == 1
