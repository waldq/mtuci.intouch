from fastapi.security import OAuth2PasswordBearer
from fastapi import Depends, HTTPException, status, Request
from sqlmodel import Session
import jwt
from jwt.exceptions import InvalidTokenError

from crud.user import get_user_by_login
from core.config import settings
from schemas.user import TokenData
from app.database import get_session
from app.redis_client import *

# Объект, обращающийся к tokenUrl /auth/token, передаёт туда логин и пароль и возвращает токен.
# По факту самостоятельно он ничего не возвращает, но при успешной авторизации даёт права доступа к эндпоинтам.
oauth2_scheme = OAuth2PasswordBearer(tokenUrl='/auth/login')

# Функция, принимающая токен и возвращающая юзера. 
# Будет использоваться для предоставления доступа авторизованным пользователям, 
# идентифицирования получаемых и отправляемых данных.
# async def get_current_user(
#         session: Session = Depends(get_session), 
#         token: str = Depends(oauth2_scheme)
#         ):
#     credentials_exception = HTTPException( # создана переменная с ошибкой, чтобы не повторяться потом.
#             status_code=status.HTTP_401_UNAUTHORIZED,
#             detail='Пользователь не аутентифицирован.',
#             headers={'WWW-Authenticate': 'Bearer'}
#         )
#     try:
#         # Расшифровываем токен по секретному ключу из .env и алгоритму оттуда же.
#         payload = jwt.decode(token, settings.SECRET_KEY, algorithms=[settings.ALGORITHM])
#         # Получаем username (по факту логин) из токена.
#         login = payload.get('sub')
#         if login is None: # Возвращаем ошибку в случае отсуствия зашифрованного логина.
#             raise credentials_exception
#         token_data = TokenData(username=login) # Создаём объект класса с логином из токена.
#     except InvalidTokenError: # Возвращаем ошибку в случае неверного токена.
#         raise credentials_exception
#     user = get_user_by_login(session, login=token_data.username) # Получаем пользователя по логину.
#     if user is None: # Возвращаем ошибку в случае отсутствия пользователя по логину
#         raise credentials_exception
#     return user # Тут возвращаются полные данные о пользователе, включая хэшированный пароль, в будущем будет меняться.

async def get_current_user(
    request: Request,
    redis_client: redis.Redis = Depends(get_redis),
    token: str = Depends(oauth2_scheme)
):
    """
    Зависимость для защищенных эндпоинтов.
    Проверяет access токен из заголовка Authorization.
    Возвращает user_id и session_id или кидает 401.
    """
    # 1. Получаем токен из заголовка
    auth_header = request.headers.get("Authorization")
    if not auth_header or not auth_header.startswith("Bearer "):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing or invalid authorization header",
            headers={"WWW-Authenticate": "Bearer"},
        )
    # 2. Декодируем токен
    try:
        payload = jwt.decode(token, settings.ACCESS_SECRET_KEY, algorithms=[settings.ALGORITHM])
        
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
    
    # 3. Проверяем, что токен есть в Redis (не отозван)
    token_exists = await check_access_token_in_redis(
        redis_client, user_id, session_id
    )
    if not token_exists:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token has been revoked"
        )
    
    return {"user_id": user_id, "session_id": session_id}