from sqlmodel import SQLModel, Field
from datetime import datetime, date, time
import uuid

# Модель для создания таблицы.
class User(SQLModel, table=True):
    id: uuid.UUID = Field(default_factory=uuid.uuid4, primary_key=True)
    username: str = Field(max_length=64)
    tag: str | None = Field(default=None, unique=True, min_length=5, max_length=32)
    birthday: datetime | None = Field(default=None)
    login: str = Field(unique=True, max_length=64)
    hashed_password: str
    bio: str | None = Field(default=None, max_length=140)
    last_seen_date: datetime = Field(default_factory=datetime.now)
    register_date: datetime = Field(default_factory=datetime.now)