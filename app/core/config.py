from pydantic_settings import BaseSettings, SettingsConfigDict
import secrets

# Класс для получания переменных из .env.
class Settings(BaseSettings):
    DATABASE_URL: str
    ACCESS_SECRET_KEY: str
    REFRESH_SECRET_KEY: str
    ALGORITHM: str
    ACCESS_TOKEN_EXPIRE_TIME: int
    REFRESH_TOKEN_EXPIRE_TIME: int
    REDIS_URL: str
    TIMETABLE_URL: str
    ID_EPOCH: int
    MASTER_KEY: str
    PRIVATE_STATIC_KEY_1: str
    PRIVATE_STATIC_KEY_2: str
    PRIVATE_STATIC_KEY_3: str
    PRIVATE_STATIC_KEY_4: str
    PRIVATE_STATIC_KEY_5: str
    PRIVATE_KEYS_NUMBER: int

    def get_random_private_key(self) -> str:
        random_key_index = secrets.randbelow(self.PRIVATE_KEYS_NUMBER) + 1
        attr_name = f"PRIVATE_STATIC_KEY_{random_key_index}"
        return getattr(self, attr_name)

    model_config = SettingsConfigDict(env_file='.env', from_attributes=True)


settings = Settings()
