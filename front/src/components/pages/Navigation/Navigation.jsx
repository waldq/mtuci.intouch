import React, { useEffect } from 'react';
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

    const getPageTitle = (path) => {
        const titles = {
            '/dashboard': 'Панель навигации',
            '/timetable': 'Расписание',
            '/chats': 'Чаты'
        };
        return titles[path] || 'Страница';
    };

    useEffect(() => {
        document.title = getPageTitle(location.pathname);
    }, [location.pathname]);

    const goToDashboard = () => navigate('/dashboard');
    const goToChats = () => navigate('/chats');
    const goToTimetable = () => navigate('/timetable');

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
                <div className="user-avatar-placeholder"></div>
            </div>
        </nav>
    );
};

export default Navigation;