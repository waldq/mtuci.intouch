from pydantic import BaseModel
import uuid
from datetime import datetime

#Модели для чтения, создания пользователей, для создания токена, для хранения данных по токену.
class UserRead(BaseModel):
    id: uuid.UUID
    username: str
    tag: str | None
    birthday: datetime | None
    bio: str | None
    last_seen_date: datetime
    register_date: datetime

class UserCreate(BaseModel):
    username: str
    login: str
    password: str

class Token(BaseModel):
    access_token: str
    token_type: str

class TokenData(BaseModel):
    username: str | None = None