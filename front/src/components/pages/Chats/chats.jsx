import React, { useState, useRef, useEffect, useCallback, useMemo } from 'react';
import { Helmet } from "react-helmet-async";
import Navigation from '../Navigation/Navigation';
import "./chats.css";
import { useSocket } from "./useSocket";
import{
    Smile as EmojiIcon,
    Send as SendIcon,
    Search as SearchIcon
} from 'lucide-react';
import EmojiPicker from 'emoji-picker-react'

const Chats = () => {
  const [messages, setMessages] = useState([]);
  const [activeChatId, setActiveChatId] = useState();
  const [messageText, setMessageText] = useState('');
  const [contacts, setContacts] = useState([]);
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
          setMessages(data);
        }
      } catch (err) {
        console.error("Ошибка при получении сообщений:", err);
      }
    };
    fetchMessages();
  }, [activeChatId]);

  const [groups, setGroups] = useState([
    { id: 101, name: 'Group1', chat_type: 'group', title: 'Group1', lastMessage: 'message1', time: '10:00' },
    { id: 102, name: 'Group2', chat_type: 'group', title: 'Group2', lastMessage: 'message2', time: 'Yesterday' },
  ]);

  const [activeTab, setActiveTab] = useState('personal');
  const [showEmojiPicker, setShowEmojiPicker] = useState(false);
  const [showSearchWindow, setShowSearchWindow] = useState(false);
  const [searchTag, setSearchTag] = useState('');
  
  const onMessageReceived = useCallback((newMessage) => {
    console.log("Получено от сервера:", newMessage);
    if (!newMessage || !newMessage.content) return;
    setMessages((prev) => {
      const exists = prev.some(msg => msg.id === newMessage.id);
      if (exists) return prev;
      return [...prev, newMessage];
    });
  }, []); 
  
  const socket = useSocket(activeChatId, onMessageReceived);

  const handleSendMessage = () => {
    if (messageText.trim() === '') return;
    if (!socket || !socket.connected) {
      console.error("Socket not connected");
      return;
    }

    const tempId = Date.now();
    const username = localStorage.getItem('username') || 'User';
    
    setMessages(prev => [...prev, {
      id: tempId,
      content: messageText,
      sender_id: username,
      sender_name: username,
      created_at: new Date().toISOString()
    }]);

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
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

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
        type: fromContacts.chat_type,
        id: fromContacts.id
      };
    }
    const fromGroups = groups.find(g => g.id === activeChatId);
    if (fromGroups) {
      return {
        name: fromGroups.name || fromGroups.title || `Группа ${String(fromGroups.id).slice(-4)}`,
        type: 'group',
        id: fromGroups.id
      };
    }
    return { name: 'Выберите чат', type: null, id: null };
  }, [activeChatId, contacts, groups]);

  return (
    <>
      <Helmet><title>Чаты</title></Helmet>
      <div className="messenger-container">
        <Navigation />
        <aside className="chats-sidebar">
          <div className="chats-header">
            <div className="header-top">
              <h2>Недавние сообщения</h2>
              <SearchIcon onClick={() => setShowSearchWindow(!showSearchWindow)} style={{cursor: 'pointer'}}/>
            </div>
            <div className="tabs">
              <button className={`tab ${activeTab === 'personal' ? 'active' : ''}`} onClick={() => setActiveTab('personal')}>Чаты</button>
              <button className={`tab ${activeTab === 'groups' ? 'active' : ''}`} onClick={() => setActiveTab('groups')}>Группы</button>
            </div>
          </div>
          <div className="chats-list">
            {(activeTab === 'personal' ? contacts : groups).map((chat) => {
              let chatName = chat.title || chat.name || `chat${String(chat.id).slice(-4)}`;
              return(
                <div key={chat.id} className={`chat-card ${activeChatId === chat.id ? 'active' : ''}`} onClick={() => setActiveChatId(chat.id)}>
                  <div className="avatar-md avatar-initial" style={{ backgroundColor: getAvatarColor(chatName) }}>{getInitial(chatName)}</div>
                  <div className="chat-info">
                    <div className="chat-info-row"><span className="user-name">{chatName}</span><span className="timestamp">{chat.time}</span></div>
                    <p className="message-preview">{chat.lastMessage || "Нет сообщений"}</p>
                  </div>
                </div>
              );
            })}
          </div>
        </aside>

        {showSearchWindow && (
          <div className="search-people-modal">
            <div className="search-modal-header"><h3>Найти</h3></div>
            <div className="search-modal-body">
              <div className="search-input-container">
                <span className="dog-prefix">@</span>
                <input type="text" placeholder="Введите тег..." className="tag-search-input" value={searchTag} onChange={(e) => setSearchTag(e.target.value)} />
              </div>
            </div>
          </div>
        )}

        <main className="chat-window">
          <header className="chat-header">
            <div className="current-user">
              <div className="avatar-sm avatar-initial" style={{ backgroundColor: getAvatarColor(currentChatInfo.name) }}>{getInitial(currentChatInfo.name)}</div>
              <div><p className="user-name">{currentChatInfo.name}</p><p className="status-online">Онлайн</p></div>
            </div>
          </header>

          <div className="messages-area">
            {messages.map((msg) => {
              const isOwnMessage = msg.sender_id === currentUsername || msg.sender_name === currentUsername;
              const displayName = isOwnMessage ? currentUsername : (msg.sender_name || 'Пользователь');
              return (
                <div key={msg.id} className={`message-row ${isOwnMessage ? 'own-message' : ''}`}>
                  <div className="avatar-xs avatar-initial" style={{ backgroundColor: getAvatarColor(displayName) }}>{getInitial(displayName)}</div>
                  <div className="message-content">
                    <p className="message-meta">{displayName}<span>{msg.created_at ? new Date(msg.created_at).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : ''}</span></p>
                    <div className="bubble"><p>{msg.content}</p></div>
                  </div>
                </div>
              );
            })}
            <div ref={messagesEndRef} />
          </div>

          <footer className="chat-footer">
            <div className="input-wrapper">
              {showEmojiPicker && (
                <div className="EmojiPickermenu">
                  <EmojiPicker onEmojiClick={(emojiData) => setMessageText(prev => prev + emojiData.emoji)} theme="light" searchDisabled={false} />
                </div>
              )}
              <input type="text" placeholder="Напишите сообщение..." className="chat-input" value={messageText} onChange={(e) => setMessageText(e.target.value)} onKeyPress={(e) => e.key === 'Enter' && handleSendMessage()} />
              <div className="input-actions">
                <button type='button' className='action-btn' onClick={() => setShowEmojiPicker(!showEmojiPicker)}><EmojiIcon /></button>
                <SendIcon className="send-btn" onClick={handleSendMessage} style={{cursor: 'pointer'}} />
              </div>
            </div>
          </footer>
        </main>

        <aside className="chats-info-sidebar">
          <div className="info-header"><h3>Новое</h3></div>
          <div className="stories-row">
            {['Trudy', 'Jessie', 'Alex'].map(name => (
              <div key={name} className="story-item">
                <div className="story-ring"><div style={{ backgroundColor: getAvatarColor(name) }}>{getInitial(name)}</div></div>
                <span className="story-name">{name}</span>
              </div>
            ))}
          </div>
          <div className="mini-profile">
            <h3 className="Profile">Профиль</h3>
            <div className="profile-avatar-container">
              <div className="avatar-lg avatar-initial" style={{ backgroundColor: getAvatarColor(currentUsername) }}>{getInitial(currentUsername)}</div>
              <div className="online-badge"></div>
            </div>
            <h3 className="profile-name">{currentUsername}</h3>
            <p className="profile-handle">@{currentUsername?.toLowerCase()}</p>
          </div>
        </aside>
      </div>
    </>
  );
};

export default Chats;