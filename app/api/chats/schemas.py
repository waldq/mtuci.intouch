from typing import Literal
from pydantic import BaseModel

from app.db.models.chats import ChatType


class ChatGroupCreate(BaseModel):
    chat_type: Literal[ChatType.GROUP] = ChatType.GROUP
    title: str

class ChatDirectCreate(BaseModel):
    chat_type: Literal[ChatType.DIRECT] = ChatType.DIRECT
    title: Literal[None] = None

class ChatUpdate(BaseModel):
    title: str