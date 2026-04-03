import uvicorn
from fastapi import FastAPI, Depends, WebSocket, WebSocketDisconnect
from contextlib import asynccontextmanager

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

# Подключение модулей (роутеров).
app.include_router(auth.router)
app.include_router(users.router)

# Создание базы при запуске приложения.
@app.on_event(event_type='startup')
async def startup_actions():
    create_db()

@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    await websocket.accept()  # подтверждаем соединение
    
    try:
        while True:
            # Ждем сообщение от клиента
            data = await websocket.receive_text()
            
            # Отправляем ответ
            await websocket.send_text(f"Echo: {data}")
            
    except WebSocketDisconnect:
        # Клиент отключился
        print("Client disconnected")

@app.websocket("/ws/chat")
async def websocket_chat(websocket: WebSocket):
    token = websocket.headers.get("sec-websocket-protocol", "").split(", ")[-1]
    await websocket.accept(subprotocol=token)

# Функция для запуска приложения без команды в терминале. 
# Либо можно написать uvicorn app.main:app --reload, если в терминале выбрана корневая папка.
# if __name__ == '__main__':
#     uvicorn.run('app.main:app', host='127.0.0.1', port='8000', reload=True)