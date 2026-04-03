import { BrowserRouter, Routes, Route } from 'react-router-dom';
import './App.css';
import Register from './components/pages/Register/register';
import Login from './components/pages/Login/login';

function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path='/' element={<Register />} />
                <Route path='/register' element={<Register />} />
                <Route path='/login' element={<Login />} />
            </Routes>
        </BrowserRouter>
    );
}

export default App;
