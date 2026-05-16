from pydantic import BaseModel, Field
import uuid
from datetime import datetime

# Модели для чтения, обновления пользователей.
class UserRead(BaseModel):
    id: int
    username: str
    tag: str | None
    birthday: datetime | None
    bio: str | None
    last_seen_date: datetime
    register_date: datetime


class UserUpdatePublic(BaseModel):
    username: str | None
    tag: str | None
    birthday: datetime | None
    bio: str | None
    last_seen_date: datetime | None
