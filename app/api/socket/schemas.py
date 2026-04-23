from pydantic import BaseModel
import uuid

from app.db.models.messages import MessageType

#Схемы для создания, изменения и отправления сообщений. 
class MessageCreate(BaseModel):
    sender_id: uuid.UUID
    chat_id: uuid.UUID
    content: str
    msg_type: MessageType
    reply_to_id: uuid.UUID | None


class MessageUpdate(BaseModel):
    new_content: str


class MessageSend(BaseModel):
    chat_id: uuid.UUID
    content: str
    msg_type: MessageType
    reply_to_id: uuid.UUID | None
