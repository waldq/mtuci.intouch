from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.auth.dependencies import get_current_user
from app.db.database import get_session
from app.api.users.crud import get_user_by_id

# Создание роутера (считайте внешний объект FastAPI()).
# Все эндпоинты будут иметь префикс /users и группироваться в документации с тегом 'users'.
router = APIRouter(prefix='/users', tags=['users'])

# Эндпоинт, который выводит полную информацию о себе, чисто тестовый.
# При отсутствии авторизации выдаст ошибку 401.
@router.get('/me')
async def read_users_me(data=Depends(get_current_user),
                        session: AsyncSession = Depends(get_session)):
    user_id = data.get('user_id')
    user = await get_user_by_id(user_id, session)
    return user
