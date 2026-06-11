import React, { useState, useEffect } from 'react';
import { Helmet } from "react-helmet-async";
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
    const [isInitialized, setIsInitialized] = useState(false);

    const userGroup = "БПИ2502";

    const getWeekNumber = (date) => {
        const currentDate = new Date(date);
        const dayOfWeek = currentDate.getDay();
        const mondayOffset = dayOfWeek === 0 ? 6 : dayOfWeek - 1;
        
        const monday = new Date(currentDate);
        monday.setDate(currentDate.getDate() - mondayOffset);
        monday.setHours(0, 0, 0, 0);
        
        const firstMonday = new Date(currentDate.getFullYear(), 0, 1);
        const firstDayOfWeek = firstMonday.getDay();
        const firstMondayOffset = firstDayOfWeek === 0 ? 6 : firstDayOfWeek - 1;
        firstMonday.setDate(1 - firstMondayOffset);
        
        const diffDays = Math.floor((monday - firstMonday) / (1000 * 60 * 60 * 24));
        const weekNumber = Math.floor(diffDays / 7) + 1;
        
        return weekNumber;
    };

    const getCurrentWeekParity = () => {
        const now = new Date();
        const weekNumber = getWeekNumber(now);
        return weekNumber % 2 === 0 ? 'чётная' : 'нечётная';
    };

    const getWeekParityForDate = (date) => {
        const weekNumber = getWeekNumber(date);
        return weekNumber % 2 === 0 ? 'чётная' : 'нечётная';
    };

    useEffect(() => {
        const updateWeek = () => {
            setCurrentWeek(getCurrentWeekParity());
        };
        
        updateWeek();
        
        const interval = setInterval(updateWeek, 3600000);
        
        return () => clearInterval(interval);
    }, []);

    const getTodayDate = () => {
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        return today;
    };

    const getDayIndex = (date) => {
        const dayOfWeek = date.getDay();
        return dayOfWeek === 0 ? 6 : dayOfWeek - 1;
    };

    const loadSavedData = () => {
        const savedDay = localStorage.getItem('selectedDay');
        const savedDate = localStorage.getItem('selectedDate');
        const savedCurrentDate = localStorage.getItem('currentDate');
        
        const hasValidSavedData = () => {
            if (!savedDate) return false;
            const savedDateObj = new Date(savedDate);
            const today = getTodayDate();
            const diffDays = Math.floor((today - savedDateObj) / (1000 * 60 * 60 * 24));
            return diffDays < 1;
        };

        if (hasValidSavedData()) {
            return {
                selectedDay: savedDay !== null ? parseInt(savedDay) : getDayIndex(getTodayDate()),
                selectedDate: savedDate !== null ? new Date(savedDate) : getTodayDate(),
                currentDate: savedCurrentDate !== null ? new Date(savedCurrentDate) : getTodayDate()
            };
        } else {
            const today = getTodayDate();
            return {
                selectedDay: getDayIndex(today),
                selectedDate: today,
                currentDate: today
            };
        }
    };

    const savedData = loadSavedData();

    const [selectedDay, setSelectedDay] = useState(savedData.selectedDay);
    const [currentDate, setCurrentDate] = useState(savedData.currentDate);
    const [selectedDate, setSelectedDate] = useState(savedData.selectedDate);

    const days = ['ПН', 'ВТ', 'СР', 'ЧТ', 'ПТ', 'СБ', 'ВС'];
    const fullDays = ['Понедельник', 'Вторник', 'Среда', 'Четверг', 'Пятница', 'Суббота', 'Воскресенье'];

    useEffect(() => {
        if (!isInitialized) {
            const today = getTodayDate();
            const isTodayInCurrentMonth = currentDate.getMonth() === today.getMonth() &&
                                         currentDate.getFullYear() === today.getFullYear();
            
            if (!isTodayInCurrentMonth) {
                setCurrentDate(today);
            }
            setIsInitialized(true);
        }
    }, [currentDate, isInitialized]);

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
        const month = currentDate.getMonth();
        fetchTimetable(userGroup, month);
    }, [currentDate]);

    useEffect(() => {
        if (selectedDate) {
            const weekParity = getWeekParityForDate(selectedDate);
            setCurrentWeek(weekParity);
        }
    }, [selectedDate]);

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
        <>
            <Helmet>
                <title>Расписание</title>
                <meta name="description" content="Расписание занятий" />
            </Helmet>
            
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
        </>
    );
};

export default Timetable;