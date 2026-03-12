from app.models import User
from app.database import get_session
from sqlmodel import Session, select
from sqlalchemy.exc import IntegrityError

# Функция создания пользователя в бд.
def create_user(session: Session, username: str, login: str, hashed_password: str):
    user = User( # Принимает юзернейм, логин, хэшированный пароль
        username=username,
        login=login,
        hashed_password=hashed_password
    )
    try: # Пробуем добавить данные в бд и обновить их (получить недостающие дефолтные) в функции.
        session.add(user)
        session.commit()
        session.refresh(user)

    except IntegrityError: # В случае конфликта данных откатываем сессию и возвращаем ошибку.
        session.rollback()
        raise ValueError('Пользователь с такими данными уже существует.')
    
    except ValueError as e: # Возвращаем ошибки в непредвиденных случаях.
        raise e

# Функция получания юзера по логину, sql-запрос.
def get_user_by_login(session: Session, login: str):
    statement = select(User).where(login == User.login)
    results = session.exec(statement=statement)
    return results.first()