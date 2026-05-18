import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import "./dashboard.css";
import {
    Calendar as CalendarIcon,
    LayoutGrid as GridIcon,
    Phone as CallIcon,
    PlusCircle as PlusCircleIcon,
    Bell,
    Settings as SettingsIcon,
    Blocks
} from 'lucide-react';

const Dashboard = () => {
    const navigate = useNavigate();

    const handleClick = () => {
        navigate('/timetable');
    };

    return (
        <div className="dashboard-container">
            <nav className="nav-sidebar">
                <div className="nav-logo">
                    <Blocks />
                </div>
                <div className="nav-links">
                    <GridIcon className="grid-icon"/>
                    <CallIcon className="call-icon"/>
                    <PlusCircleIcon className="plus-icon"/>
                </div>
                <div className="nav-footer">
                    <Bell />
                    <SettingsIcon />
                    <div className="user-avatar-placeholder"></div>
                </div>
            </nav>

            <main className="dashboard-main">
                <div className="section-bar" onClick={handleClick}>
                    <span className="section-bar-text">Расписание</span>
                    <CalendarIcon className="calendar-icon" />
                </div>
            </main>
        </div>
    );
};

export default Dashboard;