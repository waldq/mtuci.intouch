import requests
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.miscellaneous.schemas import TimetableIn
from app.db.database import get_session
from app.core.config import settings

router = APIRouter(prefix='/misc', tags=['misc'])

#Эндпоинт, возвращающий расписание по группе за месяц в формате json. 
@router.post('/timetable')
async def timetable(
    data: TimetableIn, session: AsyncSession = Depends(get_session)
):
    url = settings.TIMETABLE_URL
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'X-Requested-With': 'XMLHttpRequest',
    }

    payload = {
        'VALUE': data.group,
        'MONTH': data.month,
        'TYPE': 'group',
        'SITE_ID': 's3',
        'sessid': '1'
    }

    try:
        response = requests.post(url, headers=headers, data=payload)
        if response.status_code == 200:
            # Сохраняем результат в наш общий словарь
            return response.json()
        else:
            raise HTTPException(
                status_code=response.status_code,
                detail=f'При поиске расписания для группы {data.group} произошла ошибка {response.status_code}.',
            )

    except Exception as e:
        print(f"Произошла ошибка с группой {data.group}: {e}")
