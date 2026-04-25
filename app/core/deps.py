from snowflake import SnowflakeGenerator
from datetime import datetime
import base58

from sqlalchemy.orm import DeclarativeBase

from app.core.config import settings

id_gen = SnowflakeGenerator(instance=1, epoch=settings.ID_EPOCH)

def gen_next_id() -> int:
    return next(id_gen)

def get_date_from_id(id: int) -> datetime:
    ms = (id >> 22) + settings.ID_EPOCH
    return datetime.fromtimestamp(ms / 1000.0)

def cut_id(id: int) -> str:
    return base58.b58encode(id.to_bytes(8, 'big'))

def get_full_id_from_short(short_id: str) -> int:
    return int.from_bytes(base58.b58decode(short_id), 'big')

def get_date_from_short(short_id: str) -> datetime:
    return get_date_from_id(get_full_id_from_short(short_id))

class Base(DeclarativeBase):
    pass
