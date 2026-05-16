from sqlalchemy.orm import DeclarativeBase
from sqlalchemy.types import TypeDecorator
from sqlalchemy import BigInteger

class Base(DeclarativeBase):
    pass

class SnowflakeString(TypeDecorator):
    impl = BigInteger
    chache_ok = True

    def process_result_value(self, value, dialect):
        if value is not None:
            return str(value)
        return value
    
    def process_bind_param(self, value, dialect):
        if value is not None:
            return int(value)
        return value