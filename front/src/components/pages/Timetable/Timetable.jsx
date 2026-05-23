import React, { useState, useEffect } from 'react';
import "./timetable.css";
import {
    Calendar as CalendarIcon,
    Clock as ClockIcon,
    ChevronLeft as ChevronLeftIcon,
    ChevronRight as ChevronRightIcon
} from 'lucide-react';
import Navigation from '../Navigation/Navigation';

const Timetable = () => {
    const [timetableData, setTimetableData] = useState(null);
    const [error, setError] = useState(null);
    const [currentWeek, setCurrentWeek] = useState('');

    const userGroup = "БПИ2502";

    const getCurrentWeek = () => {
        const now = new Date();
        const startOfYear = new Date(now.getFullYear(), 0, 1);
        const weekNumber = Math.ceil(((now - startOfYear) / 86400000 + startOfYear.getDay() + 1) / 7);
        return weekNumber % 2 === 0 ? 'чётная' : 'нечётная';
    };

    useEffect(() => {
        const updateWeek = () => {
            setCurrentWeek(getCurrentWeek());
        };
        
        updateWeek();
        
        const interval = setInterval(updateWeek, 3600000);
        
        return () => clearInterval(interval);
    }, []);

    const loadSavedData = () => {
        const savedDay = localStorage.getItem('selectedDay');
        const savedDate = localStorage.getItem('selectedDate');
        const savedCurrentDate = localStorage.getItem('currentDate');
        
        return {
            selectedDay: savedDay !== null ? parseInt(savedDay) : 0,
            selectedDate: savedDate !== null ? new Date(savedDate) : new Date(),
            currentDate: savedCurrentDate !== null ? new Date(savedCurrentDate) : new Date()
        };
    };

    const savedData = loadSavedData();

    const [selectedDay, setSelectedDay] = useState(savedData.selectedDay);
    const [currentDate, setCurrentDate] = useState(savedData.currentDate);
    const [selectedDate, setSelectedDate] = useState(savedData.selectedDate);

    const days = ['ПН', 'ВТ', 'СР', 'ЧТ', 'ПТ', 'СБ', 'ВС'];
    const fullDays = ['Понедельник', 'Вторник', 'Среда', 'Четверг', 'Пятница', 'Суббота', 'Воскресенье'];

    useEffect(() => {
        localStorage.setItem('selectedDay', selectedDay.toString());
    }, [selectedDay]);

    useEffect(() => {
        localStorage.setItem('selectedDate', selectedDate.toISOString());
    }, [selectedDate]);

    useEffect(() => {
        localStorage.setItem('currentDate', currentDate.toISOString());
    }, [currentDate]);

    const fetchTimetable = async (group, month) => {
        setError(null);
        
        try {
            const token = localStorage.getItem('access_token');
            if (!token) return;

            const response = await fetch(`${process.env.REACT_APP_API_URL}/misc/timetable`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({ group: group, month: month })
            });

            if (response.ok) {
                const data = await response.json();
                setTimetableData(data);
            } else {
                const errorData = await response.json();
                setError(errorData.detail || "Ошибка при получении расписания");
            }
        } catch (err) {
            setError("Ошибка соединения с сервером");
        }
    };

    useEffect(() => {
        const month = currentDate.getMonth() + 1;
        fetchTimetable(userGroup, month);
    }, [currentDate]);

    const currentSchedule = timetableData?.[currentWeek]?.[selectedDay] || [];
    const selectedFullSchedule = timetableData?.[currentWeek]?.[selectedDay] || [];
    const selectedDayName = fullDays[selectedDay];

    const getDaysInMonth = (date) => {
        return new Date(date.getFullYear(), date.getMonth() + 1, 0).getDate();
    };

    const getFirstDayOfMonth = (date) => {
        return new Date(date.getFullYear(), date.getMonth(), 1).getDay();
    };

    const changeMonth = (delta) => {
        const newDate = new Date(currentDate);
        newDate.setMonth(currentDate.getMonth() + delta);
        setCurrentDate(newDate);
    };

    const handleDayClick = (day) => {
        const newDate = new Date(currentDate);
        newDate.setDate(day);
        setSelectedDate(newDate);
        const dayOfWeek = newDate.getDay();
        const adjustedDay = dayOfWeek === 0 ? 6 : dayOfWeek - 1;
        setSelectedDay(adjustedDay);
    };

    const renderCalendar = () => {
        const daysInMonth = getDaysInMonth(currentDate);
        const firstDay = getFirstDayOfMonth(currentDate);
        const startDay = firstDay === 0 ? 6 : firstDay - 1;
        const daysArray = [];
        
        for (let i = 0; i < startDay; i++) {
            daysArray.push(<div key={`empty-${i}`} className="calendar-day empty"></div>);
        }
        
        for (let i = 1; i <= daysInMonth; i++) {
            const isToday = i === new Date().getDate() && 
                           currentDate.getMonth() === new Date().getMonth() &&
                           currentDate.getFullYear() === new Date().getFullYear();
            const isSelected = i === selectedDate.getDate() && 
                              currentDate.getMonth() === selectedDate.getMonth() &&
                              currentDate.getFullYear() === selectedDate.getFullYear();
            daysArray.push(
                <div 
                    key={i} 
                    className={`calendar-day ${isToday ? 'today' : ''} ${isSelected ? 'selected' : ''}`}
                    onClick={() => handleDayClick(i)}
                >
                    {i}
                </div>
            );
        }
        
        return daysArray;
    };

    const getTypeColor = (type) => {
        if (type === 'Лекция') return 'lecture';
        if (type === 'Практическое занятие') return 'practice';
        if (type === 'Лабораторная работа') return 'lab';
        if (type === 'Семинар') return 'seminar';
        return '';
    };

    const monthNames = ['Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь', 'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь'];

    return (
        <div className="timetable-container">
            <Navigation />

            <aside className="timetable-sidebar">
                <div className="timetable-header">
                    <div className="header-top">
                        <h2>Расписание</h2>
                    </div>
                    <div className="week-info">
                        <span className="week-number">Неделя: {currentWeek}</span>
                        {error && <span className="error">{error}</span>}
                    </div>
                </div>

                <div className="days-tabs">
                    {days.map((day, index) => (
                        <button
                            key={index}
                            className={`day-tab ${selectedDay === index ? 'active' : ''}`}
                            onClick={() => setSelectedDay(index)}
                        >
                            {day}
                        </button>
                    ))}
                </div>

                <div className="schedule-list">
                    {currentSchedule.length > 0 ? (
                        currentSchedule.map((lesson, idx) => (
                            <div key={idx} className="lesson-card">
                                <div className="lesson-time">
                                    <ClockIcon size={16} />
                                    <span>{lesson.time}</span>
                                </div>
                                <div className="lesson-subject">{lesson.subject}</div>
                            </div>
                        ))
                    ) : (
                        <div className="empty-schedule">
                            <p>В этот день пар нет</p>
                        </div>
                    )}
                </div>
            </aside>

            <aside className="timetable-info-sidebar">
                <div className="right-panel-content">
                    <div className="calendar-container">
                        <div className="calendar-large">
                            <div className="calendar-month-large">
                                <button onClick={() => changeMonth(-1)} className="month-nav-large">
                                    <ChevronLeftIcon size={20} />
                                </button>
                                <span className="month-title-large">{monthNames[currentDate.getMonth()]} {currentDate.getFullYear()}</span>
                                <button onClick={() => changeMonth(1)} className="month-nav-large">
                                    <ChevronRightIcon size={20} />
                                </button>
                            </div>
                            <div className="calendar-weekdays-large">
                                {['ПН', 'ВТ', 'СР', 'ЧТ', 'ПТ', 'СБ', 'ВС'].map(d => (
                                    <span key={d} className="weekday-large">{d}</span>
                                ))}
                            </div>
                            <div className="calendar-days-large">
                                {renderCalendar()}
                            </div>
                        </div>
                    </div>

                    <div className="detail-container">
                        <div className="detail-header">
                            <h4>{selectedDayName}</h4>
                        </div>
                        <div className="detail-list">
                            {selectedFullSchedule.length > 0 ? (
                                selectedFullSchedule.map((lesson, idx) => (
                                    <div key={idx} className="detail-card">
                                        <div className="detail-time">{lesson.time}</div>
                                        <div className="detail-subject">{lesson.subject}</div>
                                        <div className={`detail-type ${getTypeColor(lesson.type)}`}>{lesson.type}</div>
                                        <div className="detail-room">{lesson.room}</div>
                                        <div className="detail-teacher">{lesson.teacher}</div>
                                    </div>
                                ))
                            ) : (
                                <div className="empty-detail">
                                    <p>В этот день пар нет</p>
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            </aside>
        </div>
    );
};

export default Timetable;