import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
    Calendar as CalendarIcon,
    LayoutGrid as GridIcon,
    PlusCircle as PlusCircleIcon,
    Blocks
} from 'lucide-react';

const Navigation = () => {
    const navigate = useNavigate();
    const location = useLocation();

    const goToDashboard = () => navigate('/dashboard');
    const goToChats = () => navigate('/chats');
    const goToTimetable = () => navigate('/timetable');

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

    const username = localStorage.getItem('username') || 'User';

    return (
        <nav className="nav-sidebar">
            <div className="nav-logo">
                <Blocks />
            </div>
            <div className="nav-links">
                <div onClick={goToDashboard} className={`nav-item ${location.pathname === '/dashboard' ? 'active' : ''}`}>
                    <GridIcon />
                </div>
                <div onClick={goToTimetable} className={`nav-item ${location.pathname === '/timetable' ? 'active' : ''}`}>
                    <CalendarIcon />
                </div>
                <div onClick={goToChats} className={`nav-item ${location.pathname === '/chats' ? 'active' : ''}`}>
                    <PlusCircleIcon />
                </div>
            </div>
            <div className="nav-footer">
                <div 
                    className="user-avatar-initial"
                    style={{
                        width: 36,
                        height: 36,
                        borderRadius: '50%',
                        backgroundColor: getAvatarColor(username),
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: 'white',
                        fontWeight: 'bold',
                        fontSize: 14,
                        textTransform: 'uppercase'
                    }}
                >
                    {getInitial(username)}
                </div>
            </div>
        </nav>
    );
};

export default Navigation;