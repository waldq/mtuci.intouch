import { BrowserRouter } from 'react-router-dom';
import './App.css';
import Register from './components/pages/Register/register';

function App() {
    return (
        <BrowserRouter>
            <Register />
        </BrowserRouter>
    );
}

export default App;
