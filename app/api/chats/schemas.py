from typing import Literal, Optional, Union
from pydantic import BaseModel, Field

from app.db.models.chats import ChatType


class ChatGroupCreate(BaseModel):
    chat_type: Literal[ChatType.GROUP] = ChatType.GROUP
    title: str

class ChatDirectCreate(BaseModel):
    chat_type: Literal[ChatType.DIRECT] = ChatType.DIRECT
    title: Literal[None] = None

class ChatUpdate(BaseModel):
    title: str

class BaseChatResponse(BaseModel):
    id: str
    chat_type: ChatType
    title: Optional[str] = None

    class Config:
        from_attributes = True


class DirectChatResponse(BaseChatResponse):
    chat_type: Literal[ChatType.DIRECT] = ChatType.DIRECT
    interlocutor_id: Optional[str]
    interlocutor_username: Optional[str]


class GroupChatResponse(BaseChatResponse):
    chat_type: Literal[ChatType.GROUP] = ChatType.GROUP

# Общий тип для элемента списка
UserChatResponse = Union[DirectChatResponse, GroupChatResponse]