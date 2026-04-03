from fastapi import APIRouter, Depends, HTTPException, status, Response, Request
from fastapi.security import OAuth2PasswordRequestForm, OAuth2PasswordBearer
from sqlmodel import Session
from datetime import datetime, timedelta, timezone
import redis.asyncio as redis

from crud.user import *
from schemas.user import *
from core.security import *
from app.database import get_session
from app.dependencies import *
from app.redis_client import *

# Создание роутера (считайте внешний объект FastAPI()). 
# Все эндпоинты будут иметь префикс /auth и группироваться в документации с тегом 'auth'.
router = APIRouter(prefix='/auth', tags=['auth'])

# Эндпоинт регистрации. 
@router.post('/register', status_code=status.HTTP_201_CREATED, response_model=UserOut)
async def register_user(user: UserCreate, 
                        session: Session = Depends(get_session)): # Получает данные из модели UserCreate и сессию базы данных (передавать не нужно).
    try: # Пробует создать экземпляр пользователя и сразу добавить в базу. 
        create_user(session=session,
                    username=user.username,
                    login=user.login,
                    hashed_password=hash_password(user.password)
                    )
        return UserOut(username=user.username, login=user.login)
    except ValueError as exc: # В случае ошибки (существование юзера с таким логином) выдаст код 409.
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
                session: Session = Depends(get_session)
                ):
    # Пытается аутентифицировать (проверить юзера по данным) и плучить экземпляр юзера, в противном случае False.
    user = authenticate_user(session, form_data.username, form_data.password)
    if not user: # Если юзера нет, выдаст ошибку 401.
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED, 
            detail='Неверный логин или пароль.',
            header={'WWW-Authenticate': 'Bearer'},
            )
    
    session_id = str(uuid.uuid4())

    access_token_expires = timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_TIME)
    # Создаёт токен (строка из шестнадцатиричных цифр). Шифрует в нём логин и длительность токена.
    access_token = create_access_token(
        data={'sub': user.login, 'session_id': session_id, 'user_id': str(user.id)}, expires_delta=access_token_expires
    )
    refresh_token_expires = timedelta(minutes=settings.REFRESH_TOKEN_EXPIRE_TIME)
    # Создаёт токен (строка из шестнадцатиричных цифр). Шифрует в нём логин и длительность токена.
    refresh_token, refresh_jti = create_refresh_token(
        data={'sub': str(user.id), 'session_id': session_id}, expires_delta=refresh_token_expires
    )

    await store_tokens(redis_client, user.id, session_id, access_token, refresh_jti)

    response.set_cookie(
            key='refresh_token',
            value=refresh_token,
            httponly=True,
            secure=False,
            samesite='lax',
            max_age=7 * 24 * 60 * 60,
            path='/auth/refresh'
        )
    
    return Token(access_token=access_token, token_type='bearer')

# Эндпоинт для обновления access токена. Запрос подается из клиента
@router.post('/refresh', status_code=status.HTTP_200_OK, response_model=Token)
async def refresh_access_token(request: Request,
                               response: Response,
                               redis_client: redis.Redis = Depends(get_redis),
                               session: Session = Depends(get_session)):
    refresh_token = request.cookies.get('refresh_token')

    if not refresh_token:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Refresh token not found"
        )
    try:
        payload = jwt.decode(refresh_token, settings.REFRESH_SECRET_KEY, algorithms=[settings.ALGORITHM])
        if payload.get('type') != 'refresh':
            raise HTTPException(401, "Invalid token type")
        user_id = payload.get("sub")
        session_id = payload.get("session_id")
        jti = payload.get("jti")

        if not all([user_id, session_id, jti]):
            raise HTTPException(401, "Invalid token payload")
        
    except HTTPException as e:
        # Если токен невалидный, чистим cookie
        response.delete_cookie("refresh_token", path="/auth/refresh")
        raise e
    
    stored_data = await get_refresh_token_data(redis_client, jti)
    if not stored_data:
        response.delete_cookie("refresh_token", path="/auth/refresh")
        raise HTTPException(401, "Refresh token revoked or expired")
    
    stored_user_id, stored_session_id = stored_data
    if stored_user_id != user_id or stored_session_id != session_id:
        response.delete_cookie("refresh_token", path="/auth/refresh")
        raise HTTPException(401, "Token mismatch")
    
    # 4. Создаем НОВЫЙ access токен
    user = get_user_by_id(session, user_id)
    access_token_expires = timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_TIME)
    access_token = create_access_token(
        data={'sub': user.login, 'session_id': session_id, 'user_id': user_id}, expires_delta=access_token_expires
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
