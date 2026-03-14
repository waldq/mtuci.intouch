from sqlmodel import SQLModel, Session, create_engine

from app.models import *
from core.config import settings

# Объект для взаимодействия с бд. echo=True - будет писать в терминале все sql-запросы, на фронт не влияет.
engine = create_engine(url=settings.DATABASE_URL, echo=True)

# Функция, создающая базу данных со всеми таблицами (моделями). Модели должны быть импортированы в файл (3-я строка.)
def create_db():
    SQLModel.metadata.create_all(engine)

# Функция, создающая сессию для взаимодействия с бдшкой. В каждом эндпоинте своя сессия.
# После выполнения кода сессия автоматически закрывается.
def get_session():
    with Session(engine) as session:
        yield session
