from pydantic_settings import BaseSettings, SettingsConfigDict

# Класс для получания переменных из .env.
class Settings(BaseSettings):
    DATABASE_URL: str
    SECRET_KEY: str
    ALGORITHM: str
    ACCESS_TOKEN_EXPIRE_TIME: int


    model_config = SettingsConfigDict(env_file='.env', from_attributes=True)

settings = Settings()
    
