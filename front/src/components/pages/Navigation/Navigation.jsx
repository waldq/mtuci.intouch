import React from 'react';
import { useNavigate } from 'react-router-dom';
import {
    Calendar as CalendarIcon,
    LayoutGrid as GridIcon,
    //Phone as CallIcon,
    PlusCircle as PlusCircleIcon,
    //Bell,
    //Settings as SettingsIcon,
    Blocks
} from 'lucide-react';

const Navigation = () => {
    const navigate = useNavigate();

    const goToDashboard = () => navigate('/dashboard');
    const goToChats = () => navigate('/chats');
    const goToTimetable = () => navigate('/timetable');

    return (
        <nav className="nav-sidebar">
            <div className="nav-logo">
                <Blocks />
            </div>
            <div className="nav-links">
                <div onClick={goToDashboard} className="nav-item">
                    <GridIcon />
                </div>
                <div onClick={goToTimetable} className="nav-item">
                    <CalendarIcon />
                </div>
                <div onClick={goToChats} className="nav-item">
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