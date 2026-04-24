from sqlmodel import SQLModel, Field
from datetime import datetime, date, time
from enum import Enum
import uuid

#Модель для создания типов чатов.
class ChatType(str, Enum):
    DIRECT = 'direct'
    GROUP = 'group'

#Таблица чатов.
class Chat(SQLModel, table=True):
    id: uuid.UUID = Field(default_factory=uuid.uuid4, primary_key=True)
    chat_type: ChatType = Field(default=ChatType.DIRECT)
    title: str | None = Field(default=None)
    created_at: datetime = Field(default_factory=datetime.now)

#Модель с ролями пользователей в чате.
class ChatMembersRoles(str, Enum):
    CREATOR = 'creator'
    ADMIN = 'admin'
    MEMBER = 'member'
    DIRECT = 'direct'

#Таблица, связывающая id чата и пользователя.
class ChatMembers(SQLModel, table=True):
    chat_id: uuid.UUID = Field(
        foreign_key='chat.id', primary_key=True, index=True)
    user_id: uuid.UUID = Field(
        foreign_key='user.id', primary_key=True, index=True)
    role: ChatMembersRoles = Field(default=ChatMembersRoles.MEMBER)
    joined_at: datetime = Field(default_factory=datetime.now)
