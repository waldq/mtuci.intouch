from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.exc import IntegrityError

from app.api.auth.schemas import UserCreate, UserOut
from app.db.models.user import User

# Функция создания пользователя в бд.
async def create_user(user: UserCreate, hashed_password: str, session: AsyncSession):
    user = User(  # Принимает юзернейм, логин, хэшированный пароль
        username=user.username,
        login=user.login,
        hashed_password=hashed_password
    )
    # Пробуем добавить данные в бд и обновить их (получить недостающие дефолтные) в функции.
    try:
        session.add(user)
        await session.commit()
        await session.refresh(user)

        return UserOut(username=user.username, login=user.login)

    except IntegrityError:  # В случае конфликта данных откатываем сессию и возвращаем ошибку.
        await session.rollback()
        raise ValueError('Пользователь с такими данными уже существует.')

    except ValueError as e:  # Возвращаем ошибки в непредвиденных случаях.
        raise e
