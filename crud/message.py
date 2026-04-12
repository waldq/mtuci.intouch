import uuid
from datetime import datetime
from sqlalchemy.ext.asyncio import AsyncSession
from sqlmodel import select, update, delete

from app.models import Message, MessageType

# Функция создания сообщения в бд.
async def create_message(
        sender_id: uuid.UUID,
        chat_id: uuid.UUID,
        content: str,
        msg_type: MessageType,
        reply_to_id: uuid.UUID | None,
        session: AsyncSession):
    
    message = Message(sender_id=sender_id, 
                      chat_id=chat_id, 
                      content=content, 
                      msg_type=msg_type, 
                      reply_to_id=reply_to_id)
    try:
        session.add(message)
        await session.commit()
        await session.refresh(message)

    except Exception as e:
        await session.rollback()
        raise e
    
# Функция получания сообщения по id, sql-запрос.
async def get_message_by_id(id: uuid.UUID, session: AsyncSession):
    statement = select(Message).where(id == Message.id)
    results = await session.execute(statement=statement)
    return results.scalar_one_or_none()


async def update_message(message_id: uuid.UUID, new_content: str, session: AsyncSession):
    statement = update(Message).where(message_id == Message.id)\
        .values(content = new_content, updated_at = datetime.now()).returning(Message)
    result = await session.execute(statement)
    updated_message = result.scalar_one()
    await session.commit()
    return updated_message

async def delete_message(message_id: uuid.UUID, session: AsyncSession):
    statement = delete(Message).where(message_id == Message.id)
    await session.execute(statement)
    await session.commit()
    return {'message': 'Message deleted'}
