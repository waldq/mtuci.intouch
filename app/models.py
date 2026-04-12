from sqlmodel import SQLModel, Field
from datetime import datetime, date, time
from enum import Enum
import uuid

# Модели для создания таблицы.
class User(SQLModel, table=True):
    id: uuid.UUID = Field(default_factory=uuid.uuid4, primary_key=True)
    username: str = Field(max_length=64)
    tag: str | None = Field(default=None, unique=True, min_length=5, max_length=32, index=True)
    birthday: datetime | None = Field(default=None)
    login: str = Field(unique=True, nullable=False, max_length=64, index=True)
    hashed_password: str
    bio: str | None = Field(default=None, max_length=140)
    last_seen_date: datetime = Field(default_factory=datetime.now)
    register_date: datetime = Field(default_factory=datetime.now)

class ChatType(str, Enum):
    DIRECT = 'direct'
    GROUP = 'group'


class Chat(SQLModel, table=True):
    id: uuid.UUID = Field(default_factory=uuid.uuid4, primary_key=True)
    chat_type: ChatType = Field(default=ChatType.DIRECT)
    title: str | None = Field(default=None)
    created_at: datetime = Field(default_factory=datetime.now)

class ChatMembersRoles(str, Enum):
    CREATOR = 'creator'
    ADMIN = 'admin'
    MEMBER = 'member'

class ChatMembers(SQLModel, table=True):
    chat_id: uuid.UUID = Field(foreign_key='chat.id', primary_key=True, index=True)
    user_id: uuid.UUID = Field(foreign_key='user.id', primary_key=True, index=True)
    role: ChatMembersRoles = Field(default=ChatMembersRoles.MEMBER)
    joined_at: datetime = Field(default_factory=datetime.now)

class MessageType(str, Enum):
    TEXT = 'text'
    IMAGE = 'image'
    FILE = 'file'

class Message(SQLModel, table=True):
    id: uuid.UUID = Field(default_factory=uuid.uuid4, primary_key=True)
    sender_id: uuid.UUID = Field(foreign_key='user.id', index=True)
    chat_id: uuid.UUID = Field(foreign_key='chat.id', index=True)
    content: str
    msg_type: MessageType = Field(default=MessageType.TEXT)
    reply_to_id: uuid.UUID | None = Field(default=None, foreign_key='message.id', index=True)
    created_at: datetime = Field(default_factory=datetime.now)
    updated_at: datetime = Field(default_factory=datetime.now)

    
