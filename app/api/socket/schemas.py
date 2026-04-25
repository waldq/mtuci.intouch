from pydantic import BaseModel

from app.db.models.messages import MessageType

#Схемы для создания, изменения и отправления сообщений. 
class MessageCreate(BaseModel):
    sender_id: int
    chat_id: int
    content: str
    msg_type: MessageType
    reply_to_id: int | None


class MessageUpdate(BaseModel):
    new_content: str


class MessageSend(BaseModel):
    chat_id: int
    content: str
    msg_type: MessageType
    reply_to_id: int | None
