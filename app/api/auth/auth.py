from fastapi import APIRouter, Depends, HTTPException, status, Response, Request
from fastapi.security import OAuth2PasswordRequestForm
from sqlalchemy.ext.asyncio import AsyncSession
from datetime import timedelta
import redis.asyncio as redis
import jwt

from app.core.config import settings
from app.api.auth.crud import create_user
from app.api.auth.schemas import UserCreate, UserOut, Token
from app.api.auth.dependencies import get_refresh_token_data, get_access_key
from app.core.security import hash_password, create_access_token, create_refresh_token, authenticate_user
from app.core.deps import gen_next_id
from app.db.database import fastapi_get_db_session
from app.redis_client import get_redis, store_tokens
from app.api.users.crud import get_user_auth_by_id

# Создание роутера (считайте внешний объект FastAPI()).
# Все эндпоинты будут иметь префикс /auth и группироваться в документации с тегом 'auth'.
router = APIRouter(prefix='/auth', tags=['auth'])

# Эндпоинт регистрации.
@router.post('/register', status_code=status.HTTP_201_CREATED, response_model=UserOut)
async def register_user(user: UserCreate, # Получает данные из модели UserCreate и сессию базы данных (передавать не нужно).
                        session: AsyncSession = Depends(fastapi_get_db_session)):
    hashed_pwd = hash_password(user.password)
    try:  # Пробует создать экземпляр пользователя и сразу добавить в базу.
        user_out = await create_user(user, hashed_pwd, session)
        return user_out
    # В случае ошибки (существование юзера с таким логином) выдаст код 409.
    except ValueError:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail='Пользователь с такими данными уже существует.'
        )

# Эндпоинт для авторизации.
# Сохраняет токены в Redis, отправляет токены в Cookie и в клиент
@router.post('/login', status_code=status.HTTP_200_OK, response_model=Token)
async def login_user(response: Response,
                     redis_client: redis.Redis = Depends(get_redis),
                     form_data: OAuth2PasswordRequestForm = Depends(),
                     session: AsyncSession = Depends(fastapi_get_db_session)):
    # Пытается аутентифицировать (проверить юзера по данным) и плучить экземпляр юзера, в противном случае False.
    user = await authenticate_user(form_data.username, form_data.password, session)
    if not user:  # Если юзера нет, выдаст ошибку 401.
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail='Неверный логин или пароль.',
            headers={'WWW-Authenticate': 'Bearer'},
        )

    session_id = str(gen_next_id())

    access_token_expires = timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_TIME)
    # Создаёт токен (строка из шестнадцатиричных цифр). Шифрует в нём логин и длительность токена.
    access_token = create_access_token(
        data={
            'sub': user.login,
            'session_id': session_id,
            'user_id': user.user_id,
        },
        expires_delta=access_token_expires,
    )
    refresh_token_expires = timedelta(minutes=settings.REFRESH_TOKEN_EXPIRE_TIME)
    # Создаёт токен (строка из шестнадцатиричных цифр). Шифрует в нём логин и длительность токена.
    refresh_token, refresh_jti = create_refresh_token(
        data={'sub': str(user.user_id), 'session_id': session_id},
        expires_delta=refresh_token_expires,
    )

    await store_tokens(
        redis_client, str(user.user_id), session_id, access_token, refresh_jti
    )

    response.set_cookie(
        key='refresh_token',
        value=refresh_token,
        httponly=True,
        secure=False,
        samesite='lax',
        max_age=settings.REFRESH_TOKEN_EXPIRE_TIME * 60,
        path='/auth/refresh'
    )

    return Token(access_token=access_token, token_type='bearer')

# Эндпоинт для обновления access токена. Запрос подается из клиента
@router.post('/refresh', status_code=status.HTTP_200_OK, response_model=Token)
async def refresh_access_token(request: Request,
                               response: Response,
                               redis_client: redis.Redis = Depends(get_redis),
                               session: AsyncSession = Depends(fastapi_get_db_session)):
    refresh_token = request.cookies.get('refresh_token')

    if not refresh_token:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Refresh token not found"
        )
    try:
        payload = jwt.decode(refresh_token, settings.REFRESH_SECRET_KEY, algorithms=[
                             settings.ALGORITHM])
        if payload.get('type') != 'refresh':
            raise HTTPException(status_code=401, detail="Invalid token type")
        user_id = payload.get("sub")
        session_id = payload.get("session_id")
        jti = payload.get("jti")

        if not all([user_id, session_id, jti]):
            raise HTTPException(status_code=401, detail="Invalid token payload")

    except HTTPException as e:
        # Если токен невалидный, чистим cookie
        response.delete_cookie("refresh_token", path="/auth/refresh")
        raise e

    stored_data = await get_refresh_token_data(redis_client, jti)
    if not stored_data:
        response.delete_cookie("refresh_token", path="/auth/refresh")
        raise HTTPException(status_code=401, detail="Refresh token revoked or expired")

    stored_user_id, stored_session_id = stored_data
    if stored_user_id != user_id or stored_session_id != session_id:
        response.delete_cookie("refresh_token", path="/auth/refresh")
        raise HTTPException(status_code=401, detail="Token mismatch")

    # 4. Создаем НОВЫЙ access токен
    user = await get_user_auth_by_id(user_id, session)
    access_token_expires = timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_TIME)
    access_token = create_access_token(
        data={
            'sub': user.login,
            'session_id': session_id,
            'user_id': user_id,
        },
        expires_delta=access_token_expires,
    )

    # 5. Обновляем access токен в Redis (перезаписываем)
    access_key = get_access_key(user_id, session_id)
    await redis_client.setex(
        access_key,
        settings.ACCESS_TOKEN_EXPIRE_TIME * 60,
        access_token
    )
    # 6. Возвращаем новый access токен
    return Token(access_token=access_token, token_type='bearer')