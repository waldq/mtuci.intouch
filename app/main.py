import uvicorn
from app.database import create_db, get_session
from fastapi import FastAPI, Depends

import api.auth as auth
import api.users as users


app = FastAPI()

# Подключение модулей (роутеров).
app.include_router(auth.router)
app.include_router(users.router)

# Создание базы при запуске приложения.
@app.on_event(event_type='startup')
async def startup_actions():
    create_db()

# Функция для запуска приложения без команды в терминале. 
# Либо можно написать uvicorn app.main:app --reload, если в терминале выбрана корневая папка.
if __name__ == '__main__':
    uvicorn.run('app.main:app', host='127.0.0.1', port='8000', reload=True)