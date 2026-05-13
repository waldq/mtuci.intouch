from sqlalchemy import BigInteger, Enum as SQLEnum, ForeignKey
from sqlalchemy.orm import Mapped, mapped_column
from datetime import datetime
import enum

from app.core.deps import gen_next_id
from app.db.base_class import Base

#Модель с типами чатов.
class MessageType(str, enum.Enum):
    TEXT = 'text'
    IMAGE = 'image'
    FILE = 'file'

#Таблица сообщений.
class Message(Base):
    __tablename__ = 'message'

    id: Mapped[int] = mapped_column(
        BigInteger, 
        default=gen_next_id, 
        primary_key=True, 
        index=True)
    sender_id: Mapped[int] = mapped_column(
        BigInteger, 
        ForeignKey('user.id'), 
        index=True)
    chat_id: Mapped[int] = mapped_column(
        BigInteger, 
        ForeignKey('chat.id'), 
        index=True)
    content: Mapped[str]
    msg_type: Mapped[MessageType] = mapped_column(
        SQLEnum(MessageType), 
        default=MessageType.TEXT)
    reply_to_id: Mapped[int | None] = mapped_column(
        ForeignKey('message.id'), 
        default=None, 
        index=True)
    created_at: Mapped[datetime] = mapped_column(default=datetime.now)
    updated_at: Mapped[datetime] = mapped_column(default=datetime.now)
