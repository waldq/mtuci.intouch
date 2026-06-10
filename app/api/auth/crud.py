from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.exc import IntegrityError

from app.api.auth.schemas import UserCreate, UserOut
from app.db.models.user import User, UserAuth, UserPublic, UserKeys

# Функция создания пользователя в бд.
async def create_user(user: UserCreate, hashed_password: str, session: AsyncSession):
    user_base = User(  # Принимает юзернейм, логин, хэшированный пароль
        username=user.username
    )
    session.add(user_base)
    await session.flush()
    await session.refresh(user_base)
    user_auth = UserAuth(
        user_id=user_base.id,
        login=user.login,
        hashed_password=hashed_password
    )
    user_public = UserPublic(
        user_id=user_base.id,
        username=user.username
    )
    # Пробуем добавить данные в бд и обновить их (получить недостающие дефолтные) в функции.
    try:
        
        session.add(user_auth)
        session.add(user_public)
        await session.commit()
        await session.refresh(user_public)

        return UserOut(username=user_public.username, login=user_auth.login)

    except IntegrityError:  # В случае конфликта данных откатываем сессию и возвращаем ошибку.
        await session.rollback()
        raise ValueError('Пользователь с такими данными уже существует.')

    except ValueError as e:  # Возвращаем ошибки в непредвиденных случаях.
        raise e
    
async def update_user_private_key(user_id: int, private_key: bytes, session: AsyncSession):
    try:
        new_user_key = UserKeys(user_id=user_id, key=private_key)
        session.add(new_user_key)
        await session.commit()
        return {'result': 'Success.'}

    except IntegrityError:
        await session.rollback()
        raise ValueError('Сгенерировался существующий ключ. Регистрация не удалась.')
    except ValueError as e:
        raise e
