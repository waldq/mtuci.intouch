import redis.asyncio as redis
from core.config import settings

class RedisClient:
    _pool = None

    @classmethod
    async def get_pool(cls):
        if cls._pool is None:
            cls._pool = redis.ConnectionPool.from_url(settings.REDIS_URL, max_connections=20, decode_responses=True)
        return cls._pool
    
    @classmethod
    async def get_client(cls):
        pool = await cls.get_pool()
        return redis.Redis(connection_pool=pool, decode_responses=True)

async def get_redis():
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
    await redis_client.sadd(sessions_key, session_id)
    await redis_client.expire(sessions_key, settings.REFRESH_TOKEN_EXPIRE_TIME * 24 * 3600)

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