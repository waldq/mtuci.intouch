import React, { useState, useRef, useEffect, useCallback} from 'react';
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
          console.log(data);
          setContacts(data);

          if (data.length > 0 && !activeChatId) {
            setActiveChatId(data[0].id);
            console.log(activeChatId)
          }
        }
      } catch(err) {
        console.error("Ошибка при получении чатов:", err);
      }
    };

    fetchChats();
  }, []);

  const [groups,setGroups] = useState([
  { id: 101, name: 'Group1', lastMessage: 'message1', time: '10:00', online: true },
  { id: 102, name: 'Group2', lastMessage: 'messadge1', time: 'Yesterday', online: true },
  ]);

  const [activeTab,setActiveTab]=useState('personal')

  const [showEmojiPicker,setShowEmojiPicker]=useState(false);

  const [showSearchWindow,setShowSearchWindow] = useState(false);

  const [searchTag, setSearchTag] = useState('');
  
  const onMessageReceived = useCallback((newMessage) => {
    console.log("Получено новое сообщение:", newMessage);
    setMessages((prev) => [...prev, newMessage]);
  }, []); 
  
  const socket = useSocket(activeChatId, onMessageReceived);

  const handleSendMessage = () => {
    if (messageText.trim() === '') return;

    if (!socket || !socket.connected) {
      console.error("ОШИБКА: Сокет не подключен! Отправка невозможна.");
      return
    }
    console.log('handleSendmsg',activeChatId)
    const messageData = {
      room_id: activeChatId,
      message: {
            content: messageText,
            msg_type: "text",
            reply_to_id: null
        }
    }

    if (socket){
      console.log(messageData)
      socket.emit("send_message", messageData);
    }

    setMessageText('');
    setShowEmojiPicker(false);
  };

  const messagesEndRef = useRef(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
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

  return (
  <>
    <Helmet>
      <title>Чаты</title>
      <meta name="description" content="Ваши чаты" />
    </Helmet>
    
    <div className="messenger-container">
    
    <Navigation />

    <aside className="chats-sidebar">
      <div className="chats-header">
        <div className="header-top">
          <h2>Недавние сообщения</h2>
          <SearchIcon 
          className="search-trigger-btn"
          onClick={() => setShowSearchWindow(!showSearchWindow)}
          style={{cursor: 'pointer'}}/>
        </div>
        <div className="tabs">
          <button className={`tab ${activeTab === 'personal' ? 'active' : ''}`}
            onClick={() => setActiveTab('personal')}
          >Чаты
          </button>
          <button className={`tab ${activeTab === 'groups' ? 'active' : ''}`}
            onClick={() => setActiveTab('groups')}
          >Группы
          </button>
        </div>
      </div>
      
      <div className="chats-list">
        {(activeTab === 'personal' ? contacts : groups).map((chat) => {
          let chatName="Noname"
          if (chat.chat_type === "group") {
            chatName= chat.title || String(chat.id).slice(-4)
          } else {
            chatName= `chat${String(chat.id).slice(-4)}`
          }
          return(
            <div
              key={chat.id}
              className={`chat-card ${activeChatId === chat.id ? 'active' : ''}`}
              onClick={() => setActiveChatId(chat.id)}
            >
            <div 
              className="avatar-md avatar-initial"
              style={{
                backgroundColor: getAvatarColor(chatName)
              }}
            >
              {getInitial(chatName)}
            </div>
            <div className="chat-info">
              <div className="chat-info-row">
                <span className="user-name">{chatName}</span>
                <span className="timestamp">{chat.time}</span>
              </div>
              <p className="message-preview">{chat.lastMessage || "null"}</p>
            </div>
          </div>
          );
        })}
      </div>
    </aside> 

    {showSearchWindow && (
      <div className="search-people-modal">
        <div className="search-modal-header">
          <h3>Найти</h3>
        </div>
        <div className="search-modal-body">
          <div className="search-input-container">
            <span className="dog-prefix">@</span>
            <input 
              type="text" 
              placeholder="Введите тег..." 
              className="tag-search-input"
              value={searchTag}
              onChange={(e) => setSearchTag(e.target.value)}
            />
          </div>
        </div>
      </div>
    )}

    <main className="chat-window">
      <header className="chat-header">
        <div className="current-user">
          <div 
            className="avatar-sm avatar-initial"
            style={{
              backgroundColor: getAvatarColor((() => {
                const currentChat = contacts.find(c => c.id === activeChatId);
                if (currentChat){
                  if (currentChat.chat_type === 'group') {
                    return currentChat.title;
                  } else {
                    return `user${String(currentChat.id).slice(-4)}`
                  }
                }
                return 'User';
              })())
            }}
          >
            {getInitial((() => {
              const currentChat = contacts.find(c => c.id === activeChatId);
              if (currentChat){
                if (currentChat.chat_type === 'group') {
                  return currentChat.title;
                } else {
                  return `user${String(currentChat.id).slice(-4)}`
                }
              }
              return 'U';
            })())}
          </div>
          <div>
            <p className="user-name">
              {(() => {
                const currentChat = contacts.find(c => c.id === activeChatId);
                if (currentChat){
                  if (currentChat.chat_type === 'group') {
                    return currentChat.title;
                  } else {
                    return `user${String(currentChat.id).slice(-4)}`
                  }
                }
              })()}
            </p>
            <p className="status-online">Онлайн</p>
          </div>
        </div>
        <div className="header-actions">
        </div>
      </header>

      <div className="messages-area">
        {messages.map((msg) => (
          <div 
            key={msg.id} 
            className={`message-row ${msg.sender_id === 'my_id' ? 'own-message' : ''}`}
          >
            <div 
              className="avatar-xs avatar-initial"
              style={{
                backgroundColor: getAvatarColor(msg.sender_name || 'User')
              }}
            >
              {getInitial(msg.sender_name || 'U')}
            </div>
            <div className="message-content">
              <p className="message-meta">
                {msg.sender_id === 'my_id' ? 'Вы' : 'Ope'} 
                <span>{new Date(msg.created_at).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
              </p>
              <div className="bubble">
                <p>{msg.content}</p>
              </div>
            </div>
          </div>
        ))}
        <div className="chat-image-wrapper">
          <img src="interior.jpg" alt="Interior" className="chat-image" />
        </div>
        <div ref={messagesEndRef} />
      </div>

      <footer className="chat-footer">
        <div className="input-wrapper">
           {showEmojiPicker && (
              <div className="EmojiPickermenu">
                <EmojiPicker 
                  onEmojiClick={(emojiData) => {
                    setMessageText(prev => prev + emojiData.emoji);
                  }}
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
            onChange={(e) => 
              setMessageText(e.target.value)}
            onKeyPress={(e) => e.key === 'Enter' && handleSendMessage()}
          />
          <div className="input-actions">
            <button
              type='button'
              className='action-btn'
              onClick={() => setShowEmojiPicker(!showEmojiPicker)}
            >
              <EmojiIcon className={showEmojiPicker ? 'icon-active' : 'icon-muted'}/>
            </button>
            <SendIcon className="send-btn"
            onClick={handleSendMessage}
            style={{cursor: 'pointer'}} />
          </div>
        </div>
      </footer>
    </main>

    <aside className="chats-info-sidebar">
      <div className="info-header">
        <h3>Новое</h3>
      </div>
      
      <div className="stories-row">
        {['Trudy', 'Jessie', 'Alex'].map(name => (
          <div key={name} className="story-item">
            <div className="story-ring">
              <div style={{
                backgroundColor: getAvatarColor(name)
              }}>
                {getInitial(name)}
              </div>
            </div>
            <span className="story-name">{name}</span>
          </div>
        ))}
      </div>

      <div className="mini-profile">
        <h3 className="Profile">Профиль</h3>
          <div className="profile-avatar-container">
            <div 
              className="avatar-lg avatar-initial"
              style={{
                backgroundColor: getAvatarColor('Ope')
              }}
            >
              {getInitial('Ope')}
            </div>
            <div className="online-badge"></div>
        </div>
        <h3 className="profile-name">Ope</h3>
        <p className="profile-handle">@_Manlikeope</p>
      </div>

      <div className="publications-section">
        <div className="section-header">
          <h4>Публикации</h4>
          <span className="chevron"></span>
        </div>
        <div className="grid-media">
          <div className="grid-item"></div>
          <div className="grid-item"></div>
          <div className="grid-item"></div>
        </div>
      </div>
    </aside>
  </div>
  </>
  );
};

export default Chats;