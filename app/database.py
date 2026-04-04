from sqlmodel import SQLModel, Session, create_engine
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession

from app.models import *
from core.config import settings

# Объект для взаимодействия с бд. echo=True - будет писать в терминале все sql-запросы, на фронт не влияет.
engine = create_async_engine(url=settings.DATABASE_URL, echo=True)


async_session_maker = async_sessionmaker(bind=engine, class_=AsyncSession, expire_on_commit=False)

# Функция, создающая базу данных со всеми таблицами (моделями). Модели должны быть импортированы в файл (3-я строка.)
async def create_db():
    async with engine.begin() as conn:
        await conn.run_sync(SQLModel.metadata.create_all)

# Функция, создающая сессию для взаимодействия с бдшкой. В каждом эндпоинте своя сессия.
# После выполнения кода сессия автоматически закрывается.
async def get_session():
    async with async_session_maker() as session:
        yield session
