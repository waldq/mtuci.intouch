import uuid
from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.auth.dependencies import get_current_user
from app.api.chats.crud import (create_group_chat, 
                                create_direct_chat, 
                                update_chat_info, 
                                delete_chat, 
                                add_chatmembers, 
                                kick_chatmember,
                                read_user_chats)
from app.api.chats.schemas import ChatGroupCreate, ChatDirectCreate, ChatUpdate
from app.db.database import get_session


router = APIRouter(prefix='/chats', tags=['chats'])

#Эндпоинт создания группового чата со списком людей, id создателя подтягивается автоматически.
@router.post('/create_group')
async def create_chat_group(chat_data: ChatGroupCreate, 
                            members_data: list[uuid.UUID],
                            current_user = Depends(get_current_user), 
                            session: AsyncSession = Depends(get_session)):
    current_user_id = current_user.get('user_id')
    try:
        new_chat = await create_group_chat(
            chat_data=chat_data, 
            members_data=members_data, 
            current_user_id=current_user_id, 
            session=session
            )
        return new_chat
    
    except ValueError as e:
        raise e
    
#Эндпоинт создания личного чата с одним человеком, id создателя подтягивается автоматически.
@router.post('/create_direct')
async def create_chat_direct(chat_data: ChatDirectCreate, 
                             member_id: uuid.UUID,
                             current_user = Depends(get_current_user), 
                             session: AsyncSession = Depends(get_session)):
    current_user_id = current_user.get('user_id')
    try:
        new_chat = await create_direct_chat(
            chat_data=chat_data,
            member_id=member_id,
            current_user_id=current_user_id,
            session=session
            )
        return new_chat
    
    except ValueError as e:
        raise e

#Эндпоинт изменения информации (роли) участника чата. 
@router.patch('/update_chat_info')
async def update_chat(new_data: ChatUpdate,
                      chat_id: uuid.UUID,
                      user_data = Depends(get_current_user),
                      session: AsyncSession = Depends(get_session)):
    user_id = user_data.get('user_id')
    try:
        new_chat_info = await update_chat_info(new_data=new_data, chat_id=chat_id, session=session)
        return new_chat_info
    
    except ValueError as e:
        raise e

#Эндпоинт удаления чата в целом (для создателя) и у себя (для остальных ролей).
@router.delete('/delete_chat')
async def remove_group_chat(chat_id: uuid.UUID,
                            user_data = Depends(get_current_user),
                            session: AsyncSession = Depends(get_session)):
    user_id = user_data.get('user_id')
    try:
        result = await delete_chat(chat_id=chat_id, user_id=user_id, session=session)
        return result
    
    except ValueError as e:
        raise e

#Эндпоинт приглашения пользователя в чат.
@router.post('/invite_user') #TODO добавить проверку настроек приватности пользователя
async def invite_chat_member(chat_id: uuid.UUID,
                          members_data: list[uuid.UUID] | None,
                          user_data = Depends(get_current_user),
                          session: AsyncSession = Depends(get_session)):
    user_id = user_data.get('user_id')
    try:
        results = await add_chatmembers(chat_id=chat_id, members_data=members_data, session=session)
        return results

    except ValueError as e:
        raise e

#Эндпоинт удаления участника чата.
@router.delete('/kick_user')
async def kick_chat_member(chat_id: uuid.UUID,
                           to_kick_id: uuid.UUID,
                           user_data = Depends(get_current_user),
                           session: AsyncSession = Depends(get_session)): #TODO Добавить проверку прав пользователя, который кикает.
    user_id = user_data.get('user_id')
    try:
        result = await kick_chatmember(chat_id=chat_id, user_id=to_kick_id, session=session)
        return result
    
    except ValueError as e:
        raise e
    
@router.get('/user_chats')
async def get_user_chats(user_id: uuid.UUID, session: AsyncSession = Depends(get_session)):
    try:
        results = await read_user_chats(user_id, session)
        return results
    except ValueError as e:
        raise e