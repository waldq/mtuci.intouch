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

class FHMQVStep1In(BaseModel):
    client_public_static_key_bytes_hex: str # Публичный статичный ключ A Алисы (клиента), инициализирует протокол.

class FHMQVStep1Out(BaseModel):
    server_public_static_key_hash_bytes_hex: str # Хэш публичного статичного ключа B Боба (сервера), завершает первый этап протокола.

class FHMQVStep2In(BaseModel):
    client_public_static_key_hash_bytes_hex: str
    server_public_static_key_hash_bytes_hex: str
    client_public_ephem_key_bytes_hex: str
    aes_encrypted_keys_data_bytes_hex: str
    nonce: str

class FHMQVStep2Encrypt(BaseModel):
    server_public_static_key_hash_bytes_hex: str
    client_public_static_key_hash_bytes_hex: str
    server_public_ephem_key_bytes_hex: str

class FHMQVStep2Out(BaseModel):
    server_public_static_key_hash_bytes_hex: str
    client_public_static_key_hash_bytes_hex: str
    server_public_ephem_key_bytes_hex: str
    aes_encrypted_keys_data_bytes_hex: str
    nonce: str
    tb: str

class FHMQVStep3:
    client_public_static_key_hash_bytes_hex: str
    server_public_static_key_hash_bytes_hex: str
    client_public_ephem_key_hash_bytes_hex: str
    ta_bytes_hex: str
    aes_encrypted_keys_and_ta_bytes_hex: str
    nonce: str
    

class FHMQVStep4:
    pass
