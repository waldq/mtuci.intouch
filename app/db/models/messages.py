from sqlmodel import SQLModel, Field
from datetime import datetime, date, time
from enum import Enum
import uuid

#Модель с типами чатов.
class MessageType(str, Enum):
    TEXT = 'text'
    IMAGE = 'image'
    FILE = 'file'

#Таблица сообщений.
class Message(SQLModel, table=True):
    id: uuid.UUID = Field(default_factory=uuid.uuid4, primary_key=True)
    sender_id: uuid.UUID = Field(foreign_key='user.id', index=True)
    chat_id: uuid.UUID = Field(foreign_key='chat.id', index=True)
    content: str
    msg_type: MessageType = Field(default=MessageType.TEXT)
    reply_to_id: uuid.UUID | None = Field(
        default=None, foreign_key='message.id', index=True)
    created_at: datetime = Field(default_factory=datetime.now)
    updated_at: datetime = Field(default_factory=datetime.now)
