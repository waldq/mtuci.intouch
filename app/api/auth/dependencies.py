from fastapi.security import OAuth2PasswordBearer
from fastapi import Depends, HTTPException, status, Request
import jwt
from jwt.exceptions import InvalidTokenError
from wireup import Injected, inject_from_container
from typing import Annotated
import redis.asyncio as redis

from app.core.config import settings
from app.redis_client import *
from app.dep_inj import container

# Объект, обращающийся к tokenUrl /auth/login, передаёт туда логин и пароль и возвращает токен.
# По факту самостоятельно он ничего не возвращает, но при успешной авторизации даёт права доступа к эндпоинтам.
oauth2_scheme = OAuth2PasswordBearer(tokenUrl='/auth/login')

@inject_from_container(container)
async def get_current_user(
    redis_client: Annotated[redis.Redis, Injected[get_redis]],
    token: str = Depends(oauth2_scheme)
):
    """
    Зависимость для защищенных эндпоинтов.
    Проверяет access токен из заголовка Authorization.
    Возвращает user_id и session_id или кидает 401.
    """
    # 1. Декодируем токен
    try:
        payload = jwt.decode(token, settings.ACCESS_SECRET_KEY,
                             algorithms=[settings.ALGORITHM])

        # Проверяем, что это access токен, а не refresh
        if payload.get("type") != "access":
            raise HTTPException(401, "Invalid token type")

        user_id = payload.get('user_id')
        session_id = payload.get("session_id")

        if not user_id or not session_id:
            raise HTTPException(401, "Invalid token payload")

    except HTTPException as e:
        raise e

    except jwt.PyJWTError:
        raise HTTPException(401, 'Could not validate credentials.')

    # 2. Проверяем, что токен есть в Redis (не отозван)
    token_exists = await check_access_token_in_redis(
        redis_client, user_id, session_id
    )
    if not token_exists:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token has been revoked"
        )

    return {"user_id": user_id, "session_id": session_id}
