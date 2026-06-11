import React, { useState, useRef, useEffect, useCallback, useMemo } from 'react';
import { Helmet } from "react-helmet-async";
import Navigation from '../Navigation/Navigation';
import "./chats.css";
import { useSocket } from "./useSocket";
import {
    Smile as EmojiIcon,
    Send as SendIcon,
    Search as SearchIcon
} from 'lucide-react';
import EmojiPicker from 'emoji-picker-react';

const Chats = () => {
  const [chatMessages, setChatMessages] = useState({});
  const [activeChatId, setActiveChatId] = useState();
  const [messageText, setMessageText] = useState('');
  const [contacts, setContacts] = useState([]);
  
  const [groups, setGroups] = useState([
    { id: 101, name: 'Group1', chat_type: 'group', title: 'Group1', lastMessage: '', time: '' },
    { id: 102, name: 'Group2', chat_type: 'group', title: 'Group2', lastMessage: '', time: '' },
  ]);

  const [activeTab, setActiveTab] = useState('personal');
  const [showEmojiPicker, setShowEmojiPicker] = useState(false);
  
  const [showSearchWindow, setShowSearchWindow] = useState(false);
  const [searchTag, setSearchTag] = useState('');
  const [searchResults, setSearchResults] = useState([]);

  const currentUsername = localStorage.getItem('username') || 'User';

  useEffect(() => {
    const fetchChats = async () => {
      try {
        const token = localStorage.getItem('access_token');
        if (!token) return;
        
        const response = await fetch(`${process.env.REACT_APP_API_URL}/chats/user_chats/`, {
          headers: {'Authorization': `Bearer ${token}`}
        });
        
        if (response.ok) {
          const data = await response.json();
          console.log("Загруженные чаты:", data);
          setContacts(data);

          if (data.length > 0 && !activeChatId) {
            setActiveChatId(data[0].id);
          }
        }
      } catch(err) {
        console.error("Ошибка при получении чатов:", err);
      }
    };

    fetchChats();
  }, []);

  useEffect(() => {
    const fetchMessages = async () => {
      if (!activeChatId) return;
      try {
        const token = localStorage.getItem('access_token');
        if (!token) return;
        
        const response = await fetch(`${process.env.REACT_APP_API_URL}/chats/messages/${activeChatId}/`, {
          headers: { 'Authorization': `Bearer ${token}` }
        });
        
        if (response.ok) {
          const data = await response.json();
          setChatMessages(prev => ({
            ...prev,
            [activeChatId]: data
          }));
        }
      } catch (err) {
        console.error("Ошибка при получении сообщений:", err);
      }
    };

    fetchMessages();
  }, [activeChatId]);

  const onMessageReceived = useCallback((newMessage) => {
    console.log("Получено новое сообщение от сервера:", newMessage);
    if (!newMessage || !newMessage.content) return;
    
    const targetRoomId = newMessage.room_id || activeChatId; 
    if (!targetRoomId) return;

    const senderIdStr = String(newMessage.sender_id || '').toLowerCase();
    const senderNameStr = String(newMessage.sender_name || '').toLowerCase();
    const myUsernameStr = String(currentUsername).toLowerCase();

    const isOwnMessage = senderIdStr === myUsernameStr || senderNameStr === myUsernameStr;
    
    console.log(`Проверка на дубликат: Мой ник: ${myUsernameStr}, Отправитель ID: ${senderIdStr}, Отправитель Name: ${senderNameStr}. Это мое сообщение? -> ${isOwnMessage}`);

    const newLastMessage = newMessage.content.length > 30 
      ? newMessage.content.slice(0, 30) + '...' 
      : newMessage.content;
    
    const currentTime = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    setChatMessages(prev => {
      const currentRoomMessages = prev[targetRoomId] || [];

      if (!isOwnMessage) {
        return {
          ...prev,
          [targetRoomId]: [...currentRoomMessages, newMessage]
        };
      } else {
        const hasTempMessage = currentRoomMessages.some(msg => 
          (String(msg.sender_id).toLowerCase() === myUsernameStr || String(msg.sender_name).toLowerCase() === myUsernameStr) && 
          msg.content === newMessage.content && 
          String(msg.id).length > 10
        );

        if (hasTempMessage) {
          const updatedMessages = currentRoomMessages.map(msg => {
            if (
              (String(msg.sender_id).toLowerCase() === myUsernameStr || String(msg.sender_name).toLowerCase() === myUsernameStr) && 
              msg.content === newMessage.content && 
              String(msg.id).length > 10
            ) {
              return newMessage;
            }
            return msg;
          });
          return {
            ...prev,
            [targetRoomId]: updatedMessages
          };
        } else {
          return {
            ...prev,
            [targetRoomId]: [...currentRoomMessages, newMessage]
          };
        }
      }
    });
    
    setContacts(prev => prev.map(contact => 
      contact.id === targetRoomId 
        ? { ...contact, lastMessage: newLastMessage, time: currentTime }
        : contact
    ));
    
    setGroups(prev => prev.map(group => 
      group.id === targetRoomId 
        ? { ...group, lastMessage: newLastMessage, time: currentTime }
        : group
    ));
  }, [currentUsername]);
  
  const socket = useSocket(activeChatId, onMessageReceived);

  useEffect(() => {
    if (!socket) return;

    const handleSearchResults = (data) => {
      console.log("Получены результаты поиска:", data);
      if (data && data.results === 'None.') {
        setSearchResults([]);
      } else if (Array.isArray(data)) {
        setSearchResults(data);
      }
    };

    socket.on('search_user_results', handleSearchResults);

    return () => {
      socket.off('search_user_results', handleSearchResults);
    };
  }, [socket]);

  useEffect(() => {
    if (!searchTag.trim()) {
      setSearchResults([]);
      return;
    }

    const delayDebounceFn = setTimeout(() => {
      if (socket && socket.connected) {
        console.log(`Отправляем запрос на поиск: ${searchTag}`);
        socket.emit('search_user', { username_or_tag: searchTag });
      }
    }, 500);

    return () => {
      clearTimeout(delayDebounceFn);
    };
  }, [searchTag, socket]);

  const handleCreateOrOpenChat = (userId) => {
    console.log(`Попытка открыть или создать чат с пользователем. ID собеседника: ${userId}`);
    setShowSearchWindow(false);
    setSearchTag('');
  };

  const handleSendMessage = () => {
    if (messageText.trim() === '') return;

    if (!socket || !socket.connected) {
      console.error("ОШИБКА: Сокет не подключен! Отправка невозможна.");
      return;
    }

    const tempId = Date.now();
    const newLastMessage = messageText.length > 30 
      ? messageText.slice(0, 30) + '...' 
      : messageText;
    const currentTime = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    
    setChatMessages(prev => ({
      ...prev,
      [activeChatId]: [...(prev[activeChatId] || []), {
        id: tempId,
        content: messageText,
        sender_id: currentUsername,
        sender_name: currentUsername,
        created_at: new Date().toISOString()
      }]
    }));

    setContacts(prev => prev.map(c => c.id === activeChatId ? { ...c, lastMessage: newLastMessage, time: currentTime } : c));
    setGroups(prev => prev.map(g => g.id === activeChatId ? { ...g, lastMessage: newLastMessage, time: currentTime } : g));

    socket.emit("send_message", {
      room_id: activeChatId,
      temp_id: tempId,
      message: {
        content: messageText,
        msg_type: "text",
        reply_to_id: null
      }
    });

    setMessageText('');
    setShowEmojiPicker(false);
  };

  const messagesEndRef = useRef(null);
  const currentMessages = chatMessages[activeChatId] || [];

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [currentMessages]);

  const getInitial = (name) => {
    if (!name) return '?';
    return name.charAt(0).toUpperCase();
  };

  const getAvatarColor = (name) => {
    const colors = ['#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4', '#FFEAA7', '#DDA0DD', '#98D8C8', '#F7B731'];
    let hash = 0;
    for (let i = 0; i < name.length; i++) {
      hash = name.charCodeAt(i) + ((hash << 5) - hash);
    }
    return colors[Math.abs(hash) % colors.length];
  };

  const currentChatInfo = useMemo(() => {
    const fromContacts = contacts.find(c => c.id === activeChatId);
    if (fromContacts) {
      return {
        name: fromContacts.chat_type === 'group' 
          ? (fromContacts.title || `Группа ${String(fromContacts.id).slice(-4)}`)
          : (fromContacts.title || `user${String(fromContacts.id).slice(-4)}`),
        type: fromContacts.chat_type
      };
    }
    const fromGroups = groups.find(g => g.id === activeChatId);
    if (fromGroups) {
      return {
        name: fromGroups.name || fromGroups.title || `Группа ${String(fromGroups.id).slice(-4)}`,
        type: 'group'
      };
    }
    return { name: 'Выберите чат', type: null };
  }, [activeChatId, contacts, groups]);

  return (
  <>
    <Helmet>
      <title>Чаты</title>
      <meta name="description" content="Ваши чаты" />
    </Helmet>
    
    <div className="messenger-container">
      <Navigation />

      {/* ЛЕВАЯ ПАНЕЛЬ: СПИСОК ЧАТОВ */}
      <aside className="chats-sidebar">
        <div className="chats-header">
          <div className="header-top">
            <h2>Недавние сообщения</h2>
            <SearchIcon 
              className="search-trigger-btn"
              onClick={() => setShowSearchWindow(!showSearchWindow)}
              style={{cursor: 'pointer'}}
            />
          </div>
          <div className="tabs">
            <button className={`tab ${activeTab === 'personal' ? 'active' : ''}`} onClick={() => setActiveTab('personal')}>Чаты</button>
            <button className={`tab ${activeTab === 'groups' ? 'active' : ''}`} onClick={() => setActiveTab('groups')}>Группы</button>
          </div>
        </div>
        
        <div className="chats-list">
          {(activeTab === 'personal' ? contacts : groups).map((chat) => {
            let chatName = chat.title || chat.name || `chat${String(chat.id).slice(-4)}`;
            const displayLastMessage = chat.lastMessage ? chat.lastMessage : "Нет сообщений";
            
            return(
              <div
                key={chat.id}
                className={`chat-card ${activeChatId === chat.id ? 'active' : ''}`}
                onClick={() => setActiveChatId(chat.id)}
              >
                <div className="avatar-md avatar-initial" style={{ backgroundColor: getAvatarColor(chatName) }}>
                  {getInitial(chatName)}
                </div>
                <div className="chat-info">
                  <div className="chat-info-row">
                    <span className="user-name">{chatName}</span>
                    <span className="timestamp">{chat.time || ''}</span>
                  </div>
                  <p className="message-preview">{displayLastMessage}</p>
                </div>
              </div>
            );
          })}
        </div>
      </aside> 

      {/* МОДАЛКА ПОИСКА*/}
      {showSearchWindow && (
        <div className="search-people-modal">
          <div className="search-modal-header">
            <h3>Найти пользователей</h3>
          </div>
          <div className="search-modal-body">
            <div className="search-input-container">
              <span className="dog-prefix">@</span>
              <input 
                type="text" 
                placeholder="Введите тег или имя..." 
                className="tag-search-input"
                value={searchTag}
                onChange={(e) => setSearchTag(e.target.value)}
              />
            </div>
            <div className="search-results-list" style={{ marginTop: '15px' }}>
              {searchResults.length > 0 ? (
                searchResults.map((user) => (
                  <div 
                    key={user.id} 
                    className="search-result-card" 
                    style={{ display: 'flex', alignItems: 'center', padding: '10px', cursor: 'pointer', gap: '10px' }}
                    onClick={() => handleCreateOrOpenChat(user.id)}
                  >
                    <div className="avatar-md avatar-initial" style={{ backgroundColor: getAvatarColor(user.username) }}>
                      {getInitial(user.username)}
                    </div>
                    <div className="user-info">
                      <p className="user-name" style={{ margin: 0, fontWeight: 'bold' }}>{user.username}</p>
                      <p className="user-tag" style={{ margin: 0, color: '#888', fontSize: '14px' }}>{user.tag || `@${user.username.toLowerCase()}`}</p>
                    </div>
                  </div>
                ))
              ) : (
                searchTag.trim() !== '' && <p className="no-results" style={{ color: '#888', textAlign: 'center' }}>Ничего не найдено</p>
              )}
            </div>
          </div>
        </div>
      )}

      {/* ЦЕНТРАЛЬНОЕ ОКНО ЧАТА */}
      <main className="chat-window">
        <header className="chat-header">
          <div className="current-user">
            <div className="avatar-sm avatar-initial" style={{ backgroundColor: getAvatarColor(currentChatInfo.name) }}>
              {getInitial(currentChatInfo.name)}
            </div>
            <div>
              <p className="user-name">{currentChatInfo.name}</p>
              <p className="status-online">Онлайн</p>
            </div>
          </div>
        </header>

        {/* Область сообщений */}
        <div className="messages-area">
          {currentMessages.map((msg) => {
            const isOwnMessage = msg.sender_id === currentUsername || msg.sender_name === currentUsername;
            const displayName = isOwnMessage ? 'Вы' : (msg.sender_name || 'Пользователь');
            
            return (
              <div key={msg.id} className={`message-row ${isOwnMessage ? 'own-message' : ''}`}>
                <div className="avatar-xs avatar-initial" style={{ backgroundColor: getAvatarColor(msg.sender_name || 'User') }}>
                  {getInitial(msg.sender_name || 'U')}
                </div>
                <div className="message-content">
                  <p className="message-meta">
                    {displayName} 
                    <span>{msg.created_at ? new Date(msg.created_at).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : ''}</span>
                  </p>
                  <div className="bubble">
                    <p>{msg.content}</p>
                  </div>
                </div>
              </div>
            );
          })}
          <div ref={messagesEndRef} />
        </div>

        {/* Футер отправки */}
        <footer className="chat-footer">
          <div className="input-wrapper">
             {showEmojiPicker && (
                <div className="EmojiPickermenu">
                  <EmojiPicker 
                    onEmojiClick={(emojiData) => setMessageText(prev => prev + emojiData.emoji)}
                    theme="light"
                    searchDisabled={false}
                  />
                </div>
              )}
            <input 
              type="text" 
              placeholder="Напишите сообщение..." 
              className="chat-input"
              value={messageText}
              onChange={(e) => setMessageText(e.target.value)}
              onKeyPress={(e) => e.key === 'Enter' && handleSendMessage()}
            />
            <div className="input-actions">
              <button type='button' className='action-btn' onClick={() => setShowEmojiPicker(!showEmojiPicker)}>
                <EmojiIcon className={showEmojiPicker ? 'icon-active' : 'icon-muted'}/>
              </button>
              <SendIcon className="send-btn" onClick={handleSendMessage} style={{cursor: 'pointer'}} />
            </div>
          </div>
        </footer>
      </main>

      {/* ПРАВАЯ ПАНЕЛЬ: ПРОФИЛЬ */}
      <aside className="chats-info-sidebar">
        <div className="info-header">
          <h3>Новое</h3>
        </div>
        
        <div className="stories-row">
          {['Trudy', 'Jessie', 'Alex'].map(name => (
            <div key={name} className="story-item">
              <div className="story-ring">
                <div style={{ backgroundColor: getAvatarColor(name) }}>{getInitial(name)}</div>
              </div>
              <span className="story-name">{name}</span>
            </div>
          ))}
        </div>

        <div className="mini-profile">
          <h3 className="Profile">Профиль</h3>
          <div className="profile-avatar-container">
              <div className="avatar-lg avatar-initial" style={{ backgroundColor: getAvatarColor(currentUsername) }}>
                {getInitial(currentUsername)}
              </div>
              <div className="online-badge"></div>
          </div>
          <h3 className="profile-name">{currentUsername}</h3>
          <p className="profile-handle">@{currentUsername.toLowerCase()}</p>
        </div>
      </aside>
    </div>
  </>
  );
};

export default Chats;