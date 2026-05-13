from sqlalchemy import BigInteger, Enum as SQLEnum, ForeignKey
from sqlalchemy.orm import Mapped, mapped_column
from datetime import datetime
import enum

from app.core.deps import gen_next_id
from app.db.base_class import Base


#Модель для создания типов чатов.
class ChatType(str, enum.Enum):
    DIRECT = 'direct'
    GROUP = 'group'

#Таблица чатов.
class Chat(Base):
    __tablename__ = 'chat'

    id: Mapped[int] = mapped_column(
        BigInteger, 
        primary_key=True, 
        default=gen_next_id)
    chat_type: Mapped[ChatType] = mapped_column(
        SQLEnum(ChatType), 
        default=ChatType.DIRECT)
    title: Mapped[str | None] = mapped_column(default=None)

#Модель с ролями пользователей в чате.
class ChatMembersRoles(str, enum.Enum):
    CREATOR = 'creator'
    ADMIN = 'admin'
    MEMBER = 'member'
    DIRECT = 'direct'

#Таблица, связывающая id чата и пользователя.
class ChatMembers(Base):
    __tablename__ = 'chatmembers'

    chat_id: Mapped[int] = mapped_column(
        BigInteger, ForeignKey('chat.id'), primary_key=True, index=True)
    user_id: Mapped[int] = mapped_column(
        BigInteger, ForeignKey('user.id'), primary_key=True, index=True)
    role: Mapped[ChatMembersRoles] = mapped_column(
        SQLEnum(ChatMembersRoles), 
        default=ChatMembersRoles.MEMBER)
    joined_at: Mapped[datetime] = mapped_column(default=datetime.now)
