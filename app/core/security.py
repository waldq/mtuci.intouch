from pwdlib import PasswordHash
import jwt
import uuid
from sqlalchemy.ext.asyncio import AsyncSession
from datetime import datetime, timedelta, timezone

from app.core.config import settings
from app.db.database import get_session
from app.api.users.crud import get_user_by_login

# Объект-утилита для хэширования по рекомендованному протоколу (Argon2).
password_hash = PasswordHash.recommended()

# Хэш-затычка, чтобы нельзя было проверить наличие юзеров атакой по времени
# (если фукнция будет отрабатывать другое время, значит юзер отсутствует и наоборот.)
DUMMY_HASH = password_hash.hash('random_string')

# Функиця хэширования пароля.
def hash_password(password: str):
    return password_hash.hash(password)

# Функция проверки соответствия пароля и имеющегося хэшированного пароля.
def verify_password(plain_password: str, hashed_password: str):
    return password_hash.verify(plain_password, hashed_password)

# Функция аутентификации (проверки логина и пароля с бд.). Возвращает юзера или False.
async def authenticate_user(login: str, password: str, session: AsyncSession):
    user = await get_user_by_login(login, session)
    # даже если юзера нет, все равно проверяем пароли, чтобы время отрабатывания было +- одинаковое.
    if user is None:
        verify_password(password, DUMMY_HASH)
        return False
    if not verify_password(password, user.hashed_password):
        return False
    return user

# Функция, создающая токен.
# Принимает данные в виде словаря data.
def create_access_token(data: dict, expires_delta: timedelta | None = None):
    to_encode = data.copy()
    # если есть время длительности токена, используем его для вычисления времени истечения срока действия (часовой пояс utc).
    if expires_delta:
        expire = datetime.now(timezone.utc) + expires_delta
    else:  # если время длительности не передано, ставим 15 минут.
        expire = datetime.now(timezone.utc) + timedelta(minutes=15)
    # добавляем время истечения токена в словарь с данными.
    to_encode.update({"exp": expire, 'type': 'access'})
    # шифруем токен с данными.
    encoded_jwt = jwt.encode(
        to_encode, settings.ACCESS_SECRET_KEY, settings.ALGORITHM)
    return encoded_jwt


def create_refresh_token(data: dict, expires_delta: timedelta | None = None):
    to_encode = data.copy()
    # если есть время длительности токена, используем его для вычисления времени истечения срока действия (часовой пояс utc).
    if expires_delta:
        expire = datetime.now(timezone.utc) + expires_delta
    else:  # если время длительности не передано, ставим 15 минут.
        expire = datetime.now(timezone.utc) + timedelta(days=30)
    jti = str(uuid.uuid4())
    to_encode.update({"exp": expire, "jti": jti, 'type': 'refresh'})
    encoded_jwt = jwt.encode(
        to_encode, settings.REFRESH_SECRET_KEY, settings.ALGORITHM)
    return encoded_jwt, jti
