from sqlalchemy import select, update, or_, func
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models.user import UserAuth, UserPublic, User
from app.api.users.schemas import UserUpdatePublic

# Функция создания пользователя в бд.
async def update_user(user_to_update: UserUpdatePublic, user: UserPublic, session: AsyncSession):
    user_data = user_to_update.model_dump(exclude_unset=True)
    for key, value in user_data.items():
        setattr(user, key, value)

    session.add(user)
    await session.commit()
    await session.refresh(user)
    return user


# Функция получания юзера по логину, sql-запрос.
async def get_user_auth_by_login(login: str, session: AsyncSession):
    statement = select(UserAuth).where(UserAuth.login == login)
    results = await session.execute(statement=statement)
    return results.scalar_one_or_none()

# Функция получания юзера по id, sql-запрос.
async def get_user_base_by_id(user_id: str, session: AsyncSession):
    statement = select(User).where(User.id == user_id)
    results = await session.execute(statement=statement)
    return results.scalar_one_or_none()

async def get_user_auth_by_id(user_id: str, session: AsyncSession):
    statement = select(UserAuth).where(UserAuth.user_id == user_id)
    results = await session.execute(statement=statement)
    return results.scalar_one_or_none()

async def get_user_public_by_id(user_id: str, session: AsyncSession):
    statement = select(UserPublic).where(UserPublic.user_id == user_id)
    results = await session.execute(statement=statement)
    return results.scalar_one_or_none()

async def update_user_pub_key(user_id: str, public_key: str, session: AsyncSession):
    user = await session.execute(update(UserAuth)\
                                 .where(UserAuth.user_id == user_id)\
                                    .values(public_static_key=public_key)\
                                        .returning(User))
    if not user:
        raise ValueError
    return {'result': 'User public static key updated successfully.'}

async def search_user_public_by_username_or_tag(
        session: AsyncSession,
        username_or_tag: str | None = None, 
        ):
    if not username_or_tag or not username_or_tag.strip():
        return {'result': 'Empty request.'}
    
    search_term = f'{username_or_tag.strip().lower()}%'
    statement = select(UserPublic)\
                    .where(
                        or_(
                            func.lower(UserPublic.username).like(search_term),
                            func.lower(UserPublic.tag).like(search_term)
                        )
                    )
    results = await session.scalars(statement)
    return results
    

