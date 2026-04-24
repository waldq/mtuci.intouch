import { BrowserRouter, Routes, Route } from 'react-router-dom';
import './App.css';
import Register from './components/pages/Register/register';
import Login from './components/pages/Login/login';
import Chats from './components/pages/Chats/chats';

function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path='/' element={<Register />} />
                <Route path='/register' element={<Register />} />
                <Route path='/login' element={<Login />} />
                <Route path="/chats" element={<Chats />} />
            </Routes>
        </BrowserRouter>
    );
}

export default App;
