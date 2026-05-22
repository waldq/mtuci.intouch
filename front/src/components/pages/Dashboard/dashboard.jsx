import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import "./dashboard.css";
import {
    Calendar as CalendarIcon
} from 'lucide-react';
import Navigation from '../Navigation/Navigation';  

const Dashboard = () => {
    const navigate = useNavigate();

    const handleClick = () => {
        navigate('/timetable');
    };

    return (
        <div className="dashboard-container">
            <Navigation />  

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