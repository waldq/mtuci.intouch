import uvicorn
from fastapi import FastAPI, Depends
from contextlib import asynccontextmanager
from fastapi.middleware.cors import CORSMiddleware
import socketio


from app.db.database import create_db
import app.api.auth.auth as auth
import app.api.users.users as users
import app.api.socket.socket as socket
import app.api.socket.connection
import app.api.miscellaneous.miscellaneous as miscellaneous
import app.api.chats.chats as chats
from app.redis_client import RedisClient


@asynccontextmanager
async def lifespan(app: FastAPI):
    await RedisClient.get_pool()
    print("✅ Redis pool initialized")

    # Создание базы данных при запуске приложения
    await create_db()

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
app.include_router(socket.router)
app.include_router(miscellaneous.router)
app.include_router(chats.router)

asgi_app = socketio.ASGIApp(socket.sio, app)

# Функция для запуска приложения без команды в терминале.
# Либо можно написать uvicorn app.main:app --reload, если в терминале выбрана корневая папка.
# if __name__ == '__main__':
#     uvicorn.run('app.main:asgi_app', host='127.00.1', port='8000', reload=True)
