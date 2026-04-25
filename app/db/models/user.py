from sqlalchemy import BigInteger, String
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column
from datetime import datetime, date, time
from enum import Enum
import uuid

from app.core.deps import gen_next_id
from app.db.base_class import Base

#Таблица пользователя.
class User(Base):
    __tablename__ = 'user'

    id: Mapped[int] = mapped_column(
        BigInteger, 
        default=gen_next_id, 
        primary_key=True, 
        index=True)
    username: Mapped[str] = mapped_column(String(64))
    tag: Mapped[str | None] = mapped_column(
        String(32),
        default=None, 
        unique=True, 
        index=True)
    birthday: Mapped[datetime | None] = mapped_column(default=None)
    login: Mapped[str] = mapped_column(
        String(64),
        unique=True, 
        nullable=False, 
        index=True)
    hashed_password: Mapped[str]
    bio: Mapped[str | None] = mapped_column(
        String(140),
        default=None)
    last_seen_date: Mapped[datetime] = mapped_column(default=datetime.now)
    register_date: Mapped[datetime] = mapped_column(default=datetime.now)
