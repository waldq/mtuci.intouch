from pydantic import BaseModel
import uuid

from app.models import MessageType

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