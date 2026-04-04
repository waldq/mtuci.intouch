from app.models import User
from app.database import get_session
from sqlmodel import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.ext.asyncio import AsyncSession

# Функция создания пользователя в бд.
async def create_user(session: AsyncSession, username: str, login: str, hashed_password: str):
    user = User( # Принимает юзернейм, логин, хэшированный пароль
        username=username,
        login=login,
        hashed_password=hashed_password
    )
    try: # Пробуем добавить данные в бд и обновить их (получить недостающие дефолтные) в функции.
        session.add(user)
        await session.commit()
        await session.refresh(user)

    except IntegrityError: # В случае конфликта данных откатываем сессию и возвращаем ошибку.
        await session.rollback()
        raise ValueError('Пользователь с такими данными уже существует.')
    
    except ValueError as e: # Возвращаем ошибки в непредвиденных случаях.
        raise e

# Функция получания юзера по логину, sql-запрос.
async def get_user_by_login(session: AsyncSession, login: str):
    statement = select(User).where(login == User.login)
    results = await session.execute(statement=statement)
    return results.scalar_one_or_none()

async def get_user_by_id(session: AsyncSession, id: str):
    statement = select(User).where(id == User.id)
    results = await session.execute(statement=statement)
    return results.scalar_one_or_none()