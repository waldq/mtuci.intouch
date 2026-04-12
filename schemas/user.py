from pydantic import BaseModel, Field
import uuid
from datetime import datetime

#Модели для создания, чтения, обновления пользователей, для создания токена, для хранения данных по токену.
class UserCreate(BaseModel):
    username: str
    login: str
    password: str = Field(min_length=8)


class UserRead(BaseModel):
    id: uuid.UUID
    username: str
    tag: str | None
    birthday: datetime | None
    bio: str | None
    last_seen_date: datetime
    register_date: datetime

class UserUpdate(BaseModel):
    username: str | None
    tag: str | None
    birthday: datetime | None
    bio: str | None
    last_seen_date: datetime | None

class UserOut(BaseModel):
    username: str
    login: str

class Token(BaseModel):
    access_token: str
    token_type: str

class TokenData(BaseModel):
    username: str | None = None