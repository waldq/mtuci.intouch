import { Helmet } from 'react-helmet';
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
        return formData.password !== formData.confirmPassword || Object.values(errors).some(error => error !== '');
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
        navigate('/login');
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (hasFormErrors()) {
            alert('Исправьте ошибки в форме!');
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
                setFormData({
                    username: '',
                    login: '',
                    password: '',
                    confirmPassword: ''
                });
                setTouched({});
                setErrors({});
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
                                    required
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
                                    required
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
                                    required
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
                                    required
                                />
                                {touched.confirmPassword && errors.confirmPassword && (
                                    <span className="reg-error-message">{errors.confirmPassword}</span>
                                )}
                            </div>
                        </div>

                        <button 
                        type="submit" 
                        className="reg-button"
                        disabled={isLoading || hasFormErrors()}
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