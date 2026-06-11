from sqlalchemy import BigInteger, String, LargeBinary, ForeignKey, Index, func, text
from sqlalchemy.orm import Mapped, mapped_column
from datetime import datetime

from app.core.deps import gen_next_id
from app.db.base_class import Base, SnowflakeString

# Базовая таблица пользователя.
class User(Base):
    __tablename__ = 'user'

    id: Mapped[str] = mapped_column(
        SnowflakeString, 
        default=gen_next_id, 
        primary_key=True, 
        index=True
        )
    username: Mapped[str] = mapped_column(String(64))
    
# Таблица пользователя с аутентификационными данными.
class UserAuth(Base):
    __tablename__ = 'userauth'

    user_id: Mapped[str] = mapped_column(
        SnowflakeString, 
        ForeignKey('user.id'), 
        primary_key=True,
        index=True
        )
    login: Mapped[str] = mapped_column(
        String(64),
        unique=True, 
        nullable=False, 
        index=True
        )
    hashed_password: Mapped[str]
    public_static_key: Mapped[bytes] = mapped_column(
        LargeBinary, 
        nullable=True, 
        default=None, 
        index=True
        )

# Таблица пользователя с публичными данными.
class UserPublic(Base):
    __tablename__ = 'userpublic'


    user_id: Mapped[str] = mapped_column(
        SnowflakeString, 
        ForeignKey('user.id'), 
        primary_key=True,
        index=True
        )
    username: Mapped[str] = mapped_column(String(64))
    tag: Mapped[str | None] = mapped_column(
        String(32),
        default=None, 
        unique=True, 
        index=True
        )
    birthday: Mapped[datetime | None] = mapped_column(default=None)
    bio: Mapped[str | None] = mapped_column(
        String(140),
        default=None
        )
    last_seen_date: Mapped[datetime] = mapped_column(default=datetime.now)
    register_date: Mapped[datetime] = mapped_column(default=datetime.now)

    __table_args__ = (
        Index('idx_users_tag_lower_unique', func.lower(text('tag')), unique=True),
        Index('idx_users_username_lower_prefix', func.lower(text('username')), postgresql_ops={'lower': 'text_pattern_ops'}),
        Index('idx_users_tag_lower_prefix', func.lower(text('tag')), postgresql_ops={'lower': 'text_pattern_ops'}),
    )

class UserKeys(Base):
    __tablename__ = 'userkeys'

    user_id: Mapped[str] = mapped_column(
        SnowflakeString, 
        ForeignKey('user.id'), 
        primary_key=True,
        index=True
        )
    key: Mapped[bytes] = mapped_column(
        LargeBinary,
        unique=True,
        nullable=True
    )
