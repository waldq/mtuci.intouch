from fastapi import APIRouter, Depends
from sqlmodel import Session
from typing import Annotated

from app.dependencies import get_current_user
from app.models import User
from app.database import get_session   

# Создание роутера (считайте внешний объект FastAPI()). 
# Все эндпоинты будут иметь префикс /users и группироваться в документации с тегом 'users'.
router = APIRouter(prefix='/users', tags=['users'])

# Эндпоинт, который выводит полную информацию о себе, чисто тестовый.
# При отсутствии авторизации выдаст ошибку 401.
@router.get('/me')
async def read_users_me(current_user: User = Depends(get_current_user),
                        session: Session = Depends(get_session)
                        ):
    return current_user