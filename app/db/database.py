from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession, AsyncAttrs
from typing import AsyncGenerator, Any
from contextlib import asynccontextmanager

from app.db.models.chats import *
from app.db.models.messages import *
from app.db.models.user import *
from app.db.base_class import Base
from app.core.config import settings

# Объект для взаимодействия с бд. echo=True - будет писать в терминале все sql-запросы, на фронт не влияет.
engine = create_async_engine(url=settings.DATABASE_URL, echo=True)


async_session_maker = async_sessionmaker(
    bind=engine, class_=AsyncSession, expire_on_commit=False)


# Функция, создающая базу данных со всеми таблицами (моделями). Модели должны быть импортированы в файл (3-я строка.)
async def create_db():
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)

# Функция, создающая сессию для взаимодействия с бдшкой. В каждом эндпоинте своя сессия.
# После выполнения кода сессия автоматически закрывается.
async def fastapi_get_db_session() -> AsyncGenerator[AsyncSession, Any]:
    async with async_session_maker() as session:
        yield session

@asynccontextmanager
async def socketio_get_db_session() -> AsyncGenerator[AsyncSession, Any]:
    async with async_session_maker() as session:
        yield session
