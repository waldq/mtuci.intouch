from fastapi.security import OAuth2PasswordBearer
from fastapi import Depends, HTTPException, status
from sqlmodel import Session
import jwt
from jwt.exceptions import InvalidTokenError

from crud.user import get_user_by_login
from core.config import settings
from schemas.user import TokenData
from app.database import get_session

# Объект, обращающийся к tokenUrl /auth/token, передаёт туда логин и пароль и возвращает токен.
# По факту самостоятельно он ничего не возвращает, но при успешной авторизации даёт права доступа к эндпоинтам.
oauth2_scheme = OAuth2PasswordBearer(tokenUrl='/auth/token')

# Функция, принимающая токен и возвращающая юзера. 
# Будет использоваться для предоставления доступа авторизованным пользователям, 
# идентифицирования получаемых и отправляемых данных.
async def get_current_user(
        session: Session = Depends(get_session), 
        token: str = Depends(oauth2_scheme)
        ):
    credentials_exception = HTTPException( # создана переменная с ошибкой, чтобы не повторяться потом.
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail='Пользователь не аутентифицирован.',
            headers={'WWW-Authenticate': 'Bearer'}
        )
    try:
        # Расшифровываем токен по секретному ключу из .env и алгоритму оттуда же.
        payload = jwt.decode(token, settings.SECRET_KEY, algorithms=[settings.ALGORITHM])
        # Получаем username (по факту логин) из токена.
        login = payload.get('sub')
        if login is None: # Возвращаем ошибку в случае отсуствия зашифрованного логина.
            raise credentials_exception
        token_data = TokenData(username=login) # Создаём объект класса с логином из токена.
    except InvalidTokenError: # Возвращаем ошибку в случае неверного токена.
        raise credentials_exception
    user = get_user_by_login(session, login=token_data.username) # Получаем пользователя по логину.
    if user is None: # Возвращаем ошибку в случае отсутствия пользователя по логину
        raise credentials_exception
    return user # Тут возвращаются полные данные о пользователе, включая хэшированный пароль, в будущем будет меняться.