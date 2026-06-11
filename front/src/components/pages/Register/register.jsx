import { Helmet } from "react-helmet-async";
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './register.css';

const Register = () => {
    const navigate = useNavigate();

    const [formData, setFormData] = useState({
        username: '',
        login: '',
        password: '',
        confirmPassword: ''
    });
    const [errors, setErrors] = useState({});
    const [touched, setTouched] = useState({});
    const [isLoading, setIsLoading] = useState(false);

    const validateField = (name, value) => {
        switch (name) {
            case 'login':
                return value.trim() === '' ? 'Логин обязателен!' : '';
            case 'username':
                return value.trim() === '' ? 'Имя пользователя обязательно!' : '';
            case 'password':
                return value.length < 8 ? 'Пароль должен содержать минимум 8 символов!' : '';
            case 'confirmPassword':
                return value !== formData.password ? 'Пароли не совпадают!' : '';
            default:
                return '';
        }
    };

    const hasFormErrors = () => {
        return Object.values(errors).some(error => error !== '') || 
               formData.password !== formData.confirmPassword ||
               !formData.username || !formData.login || !formData.password;
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData({ ...formData, [name]: value });
        if (touched[name]) {
            const error = validateField(name, value);
            setErrors({ ...errors, [name]: error });
        }
    };

    const handleBlur = (e) => {
        const { name, value } = e.target;
        setTouched({ ...touched, [name]: true });
        const error = validateField(name, value);
        setErrors({ ...errors, [name]: error });
    };

    const handleClick = () => {
        navigate('/login');
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        // Валидация всех полей перед отправкой
        const newErrors = {};
        Object.keys(formData).forEach(key => {
            const error = validateField(key, formData[key]);
            if (error) newErrors[key] = error;
        });

        if (Object.keys(newErrors).length > 0) {
            setErrors(newErrors);
            setTouched({
                username: true,
                login: true,
                password: true,
                confirmPassword: true
            });
            return;
        }

        setIsLoading(true);

        try {
            const response = await fetch(`${process.env.REACT_APP_API_URL}/auth/register`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    username: formData.username,
                    login: formData.login,
                    password: formData.password
                })
            });

            if (response.status === 201) {
                const data = await response.json();
                console.log('Регистрация успешна:', data);

                if (data.access_token) {
                    localStorage.setItem('access_token', data.access_token);
                }

                alert('Регистрация прошла успешно!');
                navigate('/chats');
            } else if (response.status === 409) {
                const error = await response.json();
                alert(error.detail || 'Пользователь с таким логином уже существует');
            } else {
                alert('Ошибка регистрации');
            }
        } catch (error) {
            console.error('Ошибка:', error);
            alert('Ошибка соединения с сервером');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <>
            <Helmet>
                <title>Регистрация</title>
                <meta name="description" content="Создайте аккаунт на нашем сайте" />
                <meta name="theme-color" content="#372579" />
            </Helmet>

            <div className="reg-container">
                <div className="reg-card">
                    <h2 className="reg-title">Создайте аккаунт</h2>

                    <form className="reg-form" noValidate onSubmit={handleSubmit}>
                        <div className="reg-forms">
                            <div className="reg-input-wrapper">
                                <input
                                    type="text"
                                    placeholder="Имя пользователя"
                                    className={`reg-form-input ${touched.username && errors.username ? 'error' : ''}`}
                                    name="username"
                                    value={formData.username}
                                    onChange={handleChange}
                                    onBlur={handleBlur}
                                />
                                {touched.username && errors.username && (
                                    <span className="reg-error-message">{errors.username}</span>
                                )}
                            </div>

                            <div className="reg-input-wrapper">
                                <input
                                    type="text"
                                    placeholder="Логин"
                                    className={`reg-form-input ${touched.login && errors.login ? 'error' : ''}`}
                                    name="login"
                                    value={formData.login}
                                    onChange={handleChange}
                                    onBlur={handleBlur}
                                />
                                {touched.login && errors.login && (
                                    <span className="reg-error-message">{errors.login}</span>
                                )}
                            </div>

                            <div className="reg-input-wrapper">
                                <input
                                    type="password"
                                    placeholder="Пароль"
                                    className={`reg-form-input ${touched.password && errors.password ? 'error' : ''}`}
                                    name="password"
                                    value={formData.password}
                                    onChange={handleChange}
                                    onBlur={handleBlur}
                                />
                                {touched.password && errors.password && (
                                    <span className="reg-error-message">{errors.password}</span>
                                )}
                            </div>

                            <div className="reg-input-wrapper">
                                <input
                                    type="password"
                                    placeholder="Подтвердите пароль"
                                    className={`reg-form-input ${touched.confirmPassword && errors.confirmPassword ? 'error' : ''}`}
                                    name="confirmPassword"
                                    value={formData.confirmPassword}
                                    onChange={handleChange}
                                    onBlur={handleBlur}
                                />
                                {touched.confirmPassword && errors.confirmPassword && (
                                    <span className="reg-error-message">{errors.confirmPassword}</span>
                                )}
                            </div>
                        </div>

                        <button 
                            type="submit" 
                            className="reg-button"
                            disabled={isLoading}
                        >
                            {isLoading ? 'Регистрация...' : 'Зарегистрироваться'}
                        </button>
                    </form>

                    <button className="reg-login-button" onClick={handleClick}>
                        Уже есть аккаунт?
                    </button>
                </div>
            </div>
        </>
    );
};

export default Register;