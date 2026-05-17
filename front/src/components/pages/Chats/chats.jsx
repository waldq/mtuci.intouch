import { Helmet } from "react-helmet";
import React, { useState, useRef, useEffect, useCallback } from 'react';
import "./chats.css";
import { useSocket } from "./useSocket";
import{
    MessageSquare as ChatIcon,
    LayoutGrid as GridIcon,
    Phone as CallIcon,
    PlusCircle as PlusCircleIcon,
    Bell as NotificationIcon,
    Settings as SettingsIcon,
    Edit3 as EditIcon,
    Video as VideoIcon,
    Smile as EmojiIcon,
    Mic as MicIcon,
    Paperclip as PaperclipIcon,
    Send as SendIcon
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

  const [groups, setGroups] = useState([
  { id: 101, name: 'Group1', lastMessage: 'message1', time: '10:00', online: true },
  { id: 102, name: 'Group2', lastMessage: 'messadge1', time: 'Yesterday', online: true },
  ]);

  const [activeTab,setActiveTab]=useState('personal')

  const [showEmojiPicker,setShowEmojiPicker]=useState(false);
  
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


  

  return (
  <div className="messenger-container">
    
    {/*панель навигации */}
    <nav className="nav-sidebar">
      <div className="nav-logo">
        <ChatIcon />
      </div>
      <div className="nav-links">
        <GridIcon />
        <CallIcon />
        <PlusCircleIcon />
      </div>
      <div className="nav-footer">
        <NotificationIcon />
        <SettingsIcon />
        <img src="avatar-url.jpg" className="user-avatar" alt="Profile" />
      </div>
    </nav>

    {/*Список чатов */}
    <aside className="chats-sidebar">
      <div className="chats-header">
        <div className="header-top">
          <h2>Недавние сообщения</h2>
          <EditIcon className="icon-muted" />
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
        {(activeTab === 'personal' ? contacts : groups).map((user) => (
          <div
            key={user.id}
            className={`chat-card ${activeChatId === user.id ? 'active' : ''}`}
            onClick={() => setActiveChatId(user.id)}
          >
          <img src="ope-avatar.jpg" className="avatar-md" alt={user.name} />
          <div className="chat-info">
            <div className="chat-info-row">
              <span className="user-name">{user.name}</span>
              <span className="timestamp">{user.time}</span>
            </div>
            <p className="message-preview">{user.lastMessage}</p>
          </div>
        </div>
        ))}
      </div>
    </aside>

    {/*Окно чата */}
    <main className="chat-window">
      <header className="chat-header">
        <div className="current-user">
          <img src="ope-avatar.jpg" className="avatar-sm" />
          <div>
            <p className="user-name">Ope</p>
            <p className="status-online">Онлайн</p>
          </div>
        </div>
        <div className="header-actions">
          <VideoIcon />
        </div>
      </header>

      <div className="messages-area">
        {messages.map((msg) => (
          <div 
            key={msg.id} 
            className={`message-row ${msg.sender_id === 'my_id' ? 'own-message' : ''}`}
          >
            <img src="ope-avatar.jpg" className="avatar-xs" alt="avatar" />
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
            <MicIcon />
            <PaperclipIcon />
            <SendIcon className="send-btn"
            onClick={handleSendMessage}
            style={{cursor: 'pointer'}} />
          </div>
        </div>
      </footer>
    </main>

    {/*Правая панель*/}
    <aside className="chats-info-sidebar">
      <div className="info-header">
        <h3>Новое</h3>
        <SettingsIcon className="icon-muted" />
      </div>
      
      <div className="stories-row">
        {['Trudy', 'Jessie', 'Alex'].map(name => (
          <div key={name} className="story-item">
            <div className="story-ring">
            </div>
            <span className="story-name">{name}</span>
          </div>
        ))}
      </div>

      <div className="mini-profile">
        <h3 className="Profile">Профиль</h3>
          <div className="profile-avatar-container">
          <img src="ope-avatar.jpg" className="avatar-lg" />
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
  );
};

export default Chats;