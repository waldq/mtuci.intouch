from pydantic import BaseModel, Field

#Схемы создания, возвращения пользователя и токенов.
class UserCreate(BaseModel):
    username: str
    login: str
    password: str = Field(min_length=8)


class UserOut(BaseModel):
    username: str
    login: str


class Token(BaseModel):
    access_token: str
    token_type: str


class TokenData(BaseModel):
    username: str | None = None
