from wireup import create_async_container

from app import redis_client
from app.db import database


container = create_async_container(injectables=[
    redis_client,
    database,
    ])