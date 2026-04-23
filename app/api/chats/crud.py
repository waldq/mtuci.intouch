import uuid
from sqlmodel import select, delete, update
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.exc import IntegrityError

from app.api.chats.schemas import ChatGroupCreate, ChatDirectCreate, ChatUpdate
from app.db.models.chats import Chat, ChatMembersRoles, ChatMembers

#Функция создания группового чата.
async def create_group_chat(chat_data: ChatGroupCreate,
                      members_data: list[uuid.UUID],
                      current_user_id: uuid.UUID,
                      session: AsyncSession):
    new_chat = Chat(
        chat_type=chat_data.chat_type,
        title = chat_data.title
    )
    
    try:
        session.add(new_chat)
        await session.flush()

        creator = ChatMembers(chat_id=new_chat.id, user_id=current_user_id, role=ChatMembersRoles.CREATOR)
        session.add(creator)

        await add_chatmembers(chat_id=new_chat.id, members_data=members_data, session=session)
        await session.commit()
        await session.refresh(new_chat)

        return new_chat
    
    except IntegrityError:  # В случае конфликта данных откатываем сессию и возвращаем ошибку.
        await session.rollback()
        raise ValueError('Чат с таким id уже существует.')

    except ValueError as e:  # Возвращаем ошибки в непредвиденных случаях.
        raise e

#Функция создания личного чата.
async def create_direct_chat(chat_data: ChatDirectCreate,
                             member_id: uuid.UUID,
                             current_user_id: uuid.UUID,
                             session: AsyncSession):
    new_chat = Chat(
        chat_type=chat_data.chat_type,
        title = chat_data.title
    )
    try:
        session.add(new_chat)
        await session.flush()

        user_1 = ChatMembers(chat_id=new_chat.id, user_id=current_user_id, role=ChatMembersRoles.DIRECT.value)
        session.add(user_1)

        user_2 = ChatMembers(chat_id=new_chat.id, user_id=member_id, role=ChatMembersRoles.DIRECT.value)
        session.add(user_2)

        await session.commit()
        await session.refresh(new_chat)

        return new_chat
    
    except IntegrityError:  # В случае конфликта данных откатываем сессию и возвращаем ошибку.
        await session.rollback()
        raise ValueError('Чат с таким id уже существует.')

    except ValueError as e:  # Возвращаем ошибки в непредвиденных случаях.
        raise e

#Функция изменения информации о чате.
async def update_chat_info(new_data: ChatUpdate, chat_id: uuid.UUID, session: AsyncSession):
    statement = update(Chat).where(Chat.id == chat_id)\
        .values(title=new_data.title).returning(Chat)
    result = await session.execute(statement)
    updated_chat_info = result.scalar_one()
    await session.commit()
    return updated_chat_info

#Функция удаления чата в целом (для создателя) и у себя (для остальных пользователей)
async def delete_chat(chat_id: uuid.UUID, user_id: uuid.UUID, session: AsyncSession): #TODO добавить удаление сообщений в чате
    statement_user = select(ChatMembers)\
        .where(ChatMembers.chat_id == chat_id)\
        .where(ChatMembers.user_id == user_id)
    result = await session.execute(statement_user)
    current_user = result.scalar_one_or_none()

    if not current_user:
        raise ValueError('Пользователь не найден в чате.')

    if current_user.role == ChatMembersRoles.CREATOR:
        await session.execute(
            delete(ChatMembers)\
                .where(ChatMembers.chat_id == chat_id)
                )
        await session.execute(
            delete(Chat).where(Chat.id == chat_id)
            )
        
    else:
        kick_chatmember(chat_id, current_user.user_id, session)
    
    await session.commit()
    return {'result': 'Chat deleted successfully'}

#Функция добавляения пользователя в чат.
async def add_chatmembers(chat_id: uuid.UUID, 
                          members_data: list[uuid.UUID],
                          session: AsyncSession):
    results = {'added': [], 'failed': []}
    for user_id in members_data:
        try:
            #TODO Надо добавить проверку настроек приватности.
            new_member = ChatMembers(chat_id=chat_id, user_id=user_id)
            session.add(new_member)
            await session.flush()
            results['added'].append(user_id)

        except IntegrityError as e:
            await session.rollback()
            results['failed'].append(
                {
                    "user_id": user_id, 
                    "reason": str(e)
                        }
                    )    
            continue
    await session.commit()
    return results

#Функция изменения информации (роли) участника чата.
async def update_chatmember(chat_id: uuid.UUID,
                            member_id: uuid.UUID,
                            new_role: ChatMembersRoles,
                            session: AsyncSession): #TODO добавить проверку прав изменяющего пользователя (админ или создатель)
    statement = update(ChatMembers)\
        .where(ChatMembers.chat_id == chat_id)\
        .where(ChatMembers.user_id == member_id)\
        .values(role=new_role)\
        .returning(ChatMembers)
    result = await session.execute(statement)
    updated_chatmember = result.scalar_one()
    await session.commit()
    return updated_chatmember
    
#Функция кика пользователя из чата.
async def kick_chatmember(chat_id: uuid.UUID, user_id: uuid.UUID, session: AsyncSession): #TODO добавить проверку прав кикающего пользователя (админ или создатель)
    statement = delete(ChatMembers)\
        .where(ChatMembers.chat_id == chat_id)\
        .where(ChatMembers.user_id == user_id)
    await session.execute(statement)
    await session.commit()
    return {'result': 'Chat member deleted.'}
