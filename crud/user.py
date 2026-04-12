import uuid
from datetime import datetime
from sqlmodel import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.ext.asyncio import AsyncSession

from app.models import User
from app.database import get_session
from schemas.user import UserCreate, UserOut, UserUpdate

# Функция создания пользователя в бд.
async def create_user(user: UserCreate, hashed_password: str, session: AsyncSession):
    user = User( # Принимает юзернейм, логин, хэшированный пароль
        username=user.username,
        login=user.login,
        hashed_password=hashed_password
    )
    try: # Пробуем добавить данные в бд и обновить их (получить недостающие дефолтные) в функции.
        session.add(user)
        await session.commit()
        await session.refresh(user)

        return UserOut(username=user.username, login=user.login)

    except IntegrityError: # В случае конфликта данных откатываем сессию и возвращаем ошибку.
        await session.rollback()
        raise ValueError('Пользователь с такими данными уже существует.')
    
    except ValueError as e: # Возвращаем ошибки в непредвиденных случаях.
        raise e

async def update_user(user_to_update: UserUpdate, user: User, session: AsyncSession):
    user_data = user_to_update.model_dump(exclude_unset=True)
    for key, value in user_data.items():
        setattr(user, key, value)

    session.add(user)
    await session.commit()
    await session.refresh(user)
    return user
    

# Функция получания юзера по логину, sql-запрос.
async def get_user_by_login(login: str, session: AsyncSession):
    statement = select(User).where(User.login == login)
    results = await session.execute(statement=statement)
    return results.scalar_one_or_none()

# Функция получания юзера по id, sql-запрос.
async def get_user_by_id(id: uuid.UUID, session: AsyncSession):
    statement = select(User).where(User.id == id)
    results = await session.execute(statement=statement)
    return results.scalar_one_or_none()