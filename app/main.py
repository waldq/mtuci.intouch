import uvicorn
from fastapi import FastAPI, Depends
from contextlib import asynccontextmanager
from fastapi.middleware.cors import CORSMiddleware

from app.database import create_db, get_session

import api.auth as auth
import api.users as users
from app.dependencies import get_current_user
from app.redis_client import RedisClient

@asynccontextmanager
async def lifespan(app: FastAPI):
    await RedisClient.get_pool()
    print("✅ Redis pool initialized")
    
    yield

    if RedisClient._pool:
        await RedisClient._pool.disconnect()
        print("✅ Redis pool closed")

app = FastAPI(lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=['*'],
    allow_credentials=True,
    allow_methods=['*'],
    allow_headers=['*']
)

# Подключение модулей (роутеров).
app.include_router(auth.router)
app.include_router(users.router)

# Создание базы при запуске приложения.
@app.on_event(event_type='startup')
async def startup_actions():
    await create_db()

# Функция для запуска приложения без команды в терминале. 
# Либо можно написать uvicorn app.main:app --reload, если в терминале выбрана корневая папка.
# if __name__ == '__main__':
#     uvicorn.run('app.main:app', host='127.0.0.1', port='8000', reload=True)
