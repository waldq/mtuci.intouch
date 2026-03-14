from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordRequestForm, OAuth2PasswordBearer
from sqlmodel import Session
from datetime import datetime, timedelta, timezone

from crud.user import *
from schemas.user import *
from core.security import hash_password, authenticate_user, create_access_token
from app.database import get_session
from app.dependencies import *

# Создание роутера (считайте внешний объект FastAPI()). 
# Все эндпоинты будут иметь префикс /auth и группироваться в документации с тегом 'auth'.
router = APIRouter(prefix='/auth', tags=['auth'])

# Эндпоинт регистрации. 
@router.post('/register', status_code=status.HTTP_201_CREATED)
async def register_user(user: UserCreate, 
                        session: Session = Depends(get_session)): # Получает данные из модели UserCreate и сессию базы данных (передавать не нужно).
    try: # Пробует создать экземпляр пользователя и сразу добавить в базу. 
        create_user(session=session,
                    username=user.username,
                    login=user.login,
                    hashed_password=hash_password(user.password)
                    )
    except ValueError as exc: # В случае ошибки (существование юзера с таким логином) выдаст код 409.
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail='Пользователь с такими данными уже существует.'
        )
        
# Эндпоинт для проверки существования юзера в бд по логину. 
# При отсутствии выдаст ошибку 404.
@router.post('/check_user', status_code=status.HTTP_200_OK)
async def check_user(user: UserCreate, # Получает данные из модели UserCreate
                     session: Session = Depends(get_session)):
    result = get_user_by_login(session, user.login) # Получает юзера по логину
    if result is not None:
        return 'Такой пользователь существует.'
    else:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail='Такого пользователя не существует.'
        )
    
# Эндпоинт для логина.  В качестве username используем login.
@router.post('/token', status_code=status.HTTP_200_OK)
async def login_user(form_data: OAuth2PasswordRequestForm = Depends(), # Получает данные из формы (username и password).
                     session: Session = Depends(get_session)) -> Token:
    # Пытается аутентифицировать (проверить юзера по данным) и плучить экземпляр юзера, в противном случае False.
    user = authenticate_user(session, form_data.username, form_data.password)
    if not user: # Если юзера нет, выдаст ошибку 401.
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED, 
            detail='Неверный логин или пароль.',
            header={'WWW-Authenticate': 'Bearer'},
            )
    # Создаёт время длительности токена.
    access_token_expires = timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_TIME)
    # Создаёт токен (строка из шестнадцатиричных цифр). Шифрует в нём логин и длительность токена.
    access_token = create_access_token(
        data={'sub': user.login}, expires_delta=access_token_expires
    )
    return Token(access_token=access_token, token_type='bearer')
