import { Helmet } from "react-helmet";
import React, { useState } from 'react';
import "./chats.css";
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
  const [activeChatId, setActiveChatId] = useState(1);

  const [messageText, setMessageText] = useState('');

  const [contacts, setContacts] = useState([
    {id: 1, name: 'Ope', lastMessage:"Gee, it's been good news all day...", time: "4:27", online: true },
    {id: 2, name: 'Oleg', lastMessage:"suck!!!", time: "12:45", online: false },
    {id: 3, name: 'Vlad', lastMessage:"fuck you", time: "yesterday", online: true },
  ]);

  const [groups, setGroups] = useState([
  { id: 101, name: 'Group1', lastMessage: 'message1', time: '10:00', online: true },
  { id: 102, name: 'Group2', lastMessage: 'messadge1', time: 'Yesterday', online: true },
  ]);

  const [activeTab,setActiveTab]=useState('personal')

  const [showEmojiPicker,setShowEmojiPicker]=useState(false);

  const handleSendMessage = () => {
    if (messageText.trim() === '') return;
    console.log("Отправлено сообщение:", messageText);
    setMessageText('');
  };


  

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
        <div className="message-row">
          <img src="ope-avatar.jpg" className="avatar-xs" />
          <div className="message-content">
            <p className="message-meta">Ope <span>4:27</span></p>
            <div className="bubble">
              <p>Gee, it's been good news all day. I met someone special today...</p>
            </div>
          </div>
        </div>
        <div className="chat-image-wrapper">
          <img src="interior.jpg" alt="Interior" className="chat-image" />
        </div>
      </div>

      <footer className="chat-footer">
        <div className="input-wrapper">
           {showEmojiPicker && (
              <div className="EmojiPickermenu">
                <EmojiPicker 
                  onEmojiClick={(emojiData) => {
                    setMessageText(prev => prev + emojiData.emoji);
                  }}
                  theme="light" // или "dark" под твой дизайн
                  searchDisabled={false} // можно включить поиск по смайлам
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
    <aside className="info-sidebar">
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