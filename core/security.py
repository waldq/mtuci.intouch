from pwdlib import PasswordHash
import jwt
from sqlmodel import Session
from datetime import datetime, timedelta, timezone

from core.config import settings
from app.database import get_session
from crud.user import get_user_by_login

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
def authenticate_user(session: Session, login: str, password: str):
    user = get_user_by_login(session, login)
    if user is None: #даже если юзера нет, все равно проверяем пароли, чтобы время отрабатывания было +- одинаковое.
        verify_password(password, DUMMY_HASH)
        return False
    if not verify_password(password, user.hashed_password):
        return False
    return user

# Функция, создающая токен.
def create_access_token(data: dict, expires_delta: timedelta | None = None): # Принимает данные в виде словаря data.
    to_encode = data.copy()
    if expires_delta: # если есть время длительности токена, используем его для вычисления времени истечения срока действия (часовой пояс utc).
        expire = datetime.now(timezone.utc) + expires_delta
    else: # если время длительности не передано, ставим 15 минут.
        expire = datetime.now(timezone.utc) + timedelta(minutes=15)
    to_encode.update({"exp": expire}) # добавляем время истечения токена в словарь с данными.
    encoded_jwt = jwt.encode(to_encode, settings.SECRET_KEY, algorithm=settings.ALGORITHM) # шифруем токен с данными.
    return encoded_jwt




