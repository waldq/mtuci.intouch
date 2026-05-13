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

class FHMQVStep1(BaseModel):
    client_public_static_key: str # Публичный статичный ключ A Алисы (клиента), инициализирует протокол.

class FHMQVStep2(BaseModel):
    pass

class FHMQVStep3:
    pass

class FHMQVStep4:
    pass
