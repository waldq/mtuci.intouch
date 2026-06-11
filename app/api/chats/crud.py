from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.exc import IntegrityError
from sqlalchemy import select, delete, update, and_
from sqlalchemy.orm import aliased

from app.api.chats.schemas import ChatGroupCreate, ChatDirectCreate, ChatUpdate
from app.db.models.chats import Chat, ChatMembersRoles, ChatMembers, ChatType
from app.db.models.user import User

#Функция создания группового чата.
async def create_group_chat(chat_data: ChatGroupCreate,
                      members_data: list[str],
                      current_user_id: str,
                      session: AsyncSession):
    new_chat = Chat(
        chat_type=chat_data.chat_type,
        title = chat_data.title
    )
    
    try:
        session.add(new_chat)
        await session.flush()

        creator = ChatMembers(
            chat_id=new_chat.id, 
            user_id=current_user_id, 
            role=ChatMembersRoles.CREATOR
        )
        session.add(creator)

        await add_chatmembers(chat_id=new_chat.id, members_data=members_data, session=session)
        await session.refresh(new_chat)

        return new_chat
    
    except IntegrityError:  # В случае конфликта данных откатываем сессию и возвращаем ошибку.
        await session.rollback()
        raise ValueError('Чат с таким id уже существует или ошибка связей.')

    except ValueError as e:  # Возвращаем ошибки в непредвиденных случаях.
        raise e

#Функция создания личного чата.
async def create_direct_chat(chat_data: ChatDirectCreate,
                             member_id: str,
                             current_user_id: str,
                             session: AsyncSession):
    if member_id == current_user_id:
        raise ValueError('Нельзя создать чат с самим собой')

    statement = select(ChatMembers.chat_id).where(
        and_(
            ChatMembers.user_id == current_user_id, 
            ChatMembers.role == ChatType.DIRECT
        )
    )
    result = await session.execute(statement)
    current_user_chats = {row[0] for row in result.fetchall()}

    if current_user_chats:
        statement = select(ChatMembers.chat_id).where(
            and_(
                ChatMembers.user_id == member_id, 
                ChatMembers.chat_id.in_(current_user_chats)
            )
        )

        result = await session.execute(statement)
        
        if result.fetchall():
            raise ValueError('Direct чат между пользователями уже существует')

    new_chat = Chat(
        chat_type=chat_data.chat_type,
        title = chat_data.title
    )
    try:
        session.add(new_chat)
        await session.flush()

        members = [
            ChatMembers(
                chat_id=new_chat.id, 
                user_id=current_user_id, 
                role=ChatMembersRoles.DIRECT.value), 
            ChatMembers(
                chat_id=new_chat.id, 
                user_id=member_id, 
                role=ChatMembersRoles.DIRECT.value)
            ]
        session.add_all(members)

        await session.commit()
        await session.refresh(new_chat)

        return new_chat
    
    except IntegrityError:  # В случае конфликта данных откатываем сессию и возвращаем ошибку.
        await session.rollback()
        raise ValueError('Чат с таким id уже существует или ошибка связей.')

    except ValueError as e:  # Возвращаем ошибки в непредвиденных случаях.
        raise e

#Функция изменения информации о чате.
async def update_chat_info(new_data: ChatUpdate, chat_id: str, session: AsyncSession):
    statement = update(Chat).where(Chat.id == chat_id)\
        .values(title=new_data.title).returning(Chat)
    result = await session.execute(statement)
    updated_chat_info = result.scalar_one()
    await session.commit()
    return updated_chat_info

#Функция удаления чата в целом (для создателя) и у себя (для остальных пользователей)
async def delete_chat(chat_id: str, user_id: str, session: AsyncSession): #TODO добавить удаление сообщений в чате
    statement_user = select(ChatMembers)\
        .where(ChatMembers.chat_id == chat_id)\
        .where(ChatMembers.user_id == user_id)
    result = await session.execute(statement_user)
    current_user = result.scalar_one_or_none()

    if not current_user:
        raise ValueError('Пользователь не найден в чате.')

    if current_user.role == ChatMembersRoles.CREATOR.value or current_user.role == ChatMembersRoles.DIRECT.value:
        await session.execute(
            delete(ChatMembers)\
                .where(ChatMembers.chat_id == chat_id)
                )
        await session.execute(
            delete(Chat).where(Chat.id == chat_id)
            )
        await session.commit()
        
    else:
        await kick_chatmember(chat_id, current_user.user_id, session)
    
    return {'result': 'Chat deleted successfully.'}

#Функция добавляения пользователя в чат.
async def add_chatmembers(chat_id: str, 
                          members_data: list[str],
                          session: AsyncSession):
    results = {'added': [], 'failed': []}
    for user_id in members_data:
        try:
            #TODO Надо добавить проверку настроек приватности.
            async with session.begin_nested():
                new_member = ChatMembers(chat_id=chat_id, user_id=user_id)
                session.add(new_member)
                await session.flush()
            results['added'].append(user_id)

        except IntegrityError as e:
            results['failed'].append(
                {
                    "user_id": user_id, 
                    "reason": str(e)
                        }
                    )    
    await session.commit()
    return results

#Функция изменения информации (роли) участника чата.
async def update_chatmember(chat_id: str,
                            member_id: str,
                            new_role: ChatMembersRoles,
                            session: AsyncSession): #TODO добавить проверку прав изменяющего пользователя (админ или создатель)
    statement = update(ChatMembers)\
        .where(ChatMembers.chat_id == chat_id)\
        .where(ChatMembers.user_id == member_id)\
        .values(role=new_role)\
        .returning(ChatMembers)
    result = await session.execute(statement)
    updated_chatmember = result.scalar_one_or_none()
    await session.commit()
    return updated_chatmember
    
#Функция кика пользователя из чата.
async def kick_chatmember(chat_id: str, user_id: str, session: AsyncSession): #TODO добавить проверку прав кикающего пользователя (админ или создатель)
    statement = delete(ChatMembers)\
        .where(ChatMembers.chat_id == chat_id)\
        .where(ChatMembers.user_id == user_id)
    await session.execute(statement)
    await session.commit()
    return {'result': 'Chat member deleted.'}

#Функция, возвращающая id чатов пользователя.
async def read_user_chats(user_id: str, session: AsyncSession):
    me = aliased(ChatMembers, name="me")
    other = aliased(ChatMembers, name="other")

    statement = select(
        Chat.id.label('id'),
        Chat.chat_type.label('chat_type'),
        Chat.title.label('title'),
        User.id.label('interlocutor_id'),
        User.username.label('interlocutor_username')
    )\
        .join(me, and_(me.chat_id == Chat.id, me.user_id == user_id))\
        .outerjoin(
            other,
            and_(
                other.chat_id == Chat.id,
                other.user_id != user_id,
                Chat.chat_type == ChatType.DIRECT.value
            )
        )\
        .outerjoin(User, User.id == other.user_id)\
        .distinct()
    results = await session.execute(statement)
    return results.mappings().all()
