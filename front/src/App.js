import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { HelmetProvider } from 'react-helmet-async';
import './App.css';
import Register from './components/pages/Register/register';
import Login from './components/pages/Login/login';
import Chats from './components/pages/Chats/chats';
import Timetable from './components/pages/Timetable/Timetable';
import Dashboard from './components/pages/Dashboard/dashboard';

function App() {
    return (
        <HelmetProvider>
            <BrowserRouter>
                <Routes>
                    <Route path='/' element={<Register />} />
                    <Route path='/register' element={<Register />} />
                    <Route path='/login' element={<Login />} />
                    <Route path="/chats" element={<Chats />} />
                    <Route path="/timetable" element={<Timetable />} />
                    <Route path="/dashboard" element={<Dashboard />} />
                </Routes>
            </BrowserRouter>
        </HelmetProvider>
    );
}

export default App;