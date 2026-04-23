import uuid
from datetime import datetime
from sqlmodel import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models.user import User
from app.db.database import get_session
from app.api.users.schemas import UserUpdate

# Функция создания пользователя в бд.
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
