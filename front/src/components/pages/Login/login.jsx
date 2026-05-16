import { Helmet } from "react-helmet";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import './login.css';

const Login = () => {
    const navigate = useNavigate();

    const [formData, setFormData] = useState({
        login: '',
        password: ''
    });
    const [errors, setErrors] = useState({});
    const [touched, setTouched] = useState({});

    const validateField = (name, value) => {
        switch (name) {
            case 'login':
                return value.trim() === '' ? 'Логин обязателен!' : '';
            case 'password':
                return value.trim() === '' ? 'Пароль обязателен!' : '';
            default:
                return '';
        }
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData({ ...formData, [name]: value });
    };

    const handleBlur = (e) => {
        const { name, value } = e.target;
        setTouched({ ...touched, [name]: true });
        const error = validateField(name, value);
        setErrors({ ...errors, [name]: error });
    };

    const handleClick = () => {
        navigate('/register');
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        try {
            const response = await fetch('http://127.0.0.1:8000/auth/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: new URLSearchParams({
                    username: formData.login,
                    password: formData.password
                })
            });

            if (response.status === 200) {
                const data = await response.json();
                console.log('Вход успешен:', data);
                localStorage.setItem('access_token', data.access_token);
                alert('Вход выполнен успешно!');
                navigate('/');
            } else if (response.status === 401) {
                alert('Неверный логин или пароль');
            } else {
                alert('Ошибка входа');
            }
        } catch (error) {
            console.error('Ошибка:', error);
            alert('Ошибка соединения с сервером');
        }
    };

    return (
        <>
            <Helmet>
                <title>Вход</title>
                <meta name="description" content="Войдите в аккаунт" />
                <meta name="theme-color" content="#372579" />
            </Helmet>
            <div className="log-container">
                <div className="log-card">
                    <h2 className="log-title">Вход</h2>

                    <form className="log-form" noValidate onSubmit={handleSubmit}>
                        <div className="log-forms">
                            <div className="log-input-wrapper">
                                <input
                                    type="text"
                                    placeholder="Логин"
                                    className={`log-form-input ${touched.login && errors.login ? 'error' : ''}`}
                                    name="login"
                                    value={formData.login}
                                    onChange={handleChange}
                                    onBlur={handleBlur}
                                    required
                                />
                                {touched.login && errors.login && (
                                    <span className="log-error-message">{errors.login}</span>
                                )}
                            </div>

                            <div className="log-input-wrapper">
                                <input
                                    type="password"
                                    placeholder="Пароль"
                                    className={`log-form-input ${touched.password && errors.password ? 'error' : ''}`}
                                    name="password"
                                    value={formData.password}
                                    onChange={handleChange}
                                    onBlur={handleBlur}
                                    required
                                />
                                {touched.password && errors.password && (
                                    <span className="log-error-message">{errors.password}</span>
                                )}
                            </div>
                        </div>

                        <button type="submit" className="log-submit-btn">
                            Войти
                        </button>
                    </form>

                    <button className="log-register-btn" onClick={handleClick}>
                        Создать аккаунт
                    </button>
                </div>
            </div>
        </>
    );
};

export default Login;