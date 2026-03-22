import { Helmet } from 'react-helmet';
import { useState } from 'react';
import './register.css';

const Register = () => {
    const [formData, setFormData] = useState({
        username: '',
        login: '',
        password: '',
        confirmPassword: ''
    });
    const [errors, setErrors] = useState({});
    const [touched, setTouched] = useState({});

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

    const handleSubmit = (e) => {
        e.preventDefault();
        console.log('Form submitted:', formData);
    };

    return (
        <>
            <Helmet>
                <title>Регистрация</title>
                <meta name="description" content="Создайте аккаунт на нашем сайте" />
                <meta name="theme-color" content="#372579" />
            </Helmet>

            <div className="register-container">
                <div className="register-card">
                    <h2 className="register-title">Создайте аккаунт</h2>

                    <form className="register-form" noValidate onSubmit={handleSubmit}>
                        <div className="forms">
                            <div className="input-wrapper">
                                <input
                                    type="text"
                                    placeholder="Имя пользователя"
                                    className={`form-input ${touched.username && errors.username ? 'error' : ''}`}
                                    name="username"
                                    value={formData.username}
                                    onChange={handleChange}
                                    onBlur={handleBlur}
                                    required
                                />
                                {touched.username && errors.username && (
                                    <span className="error-message">{errors.username}</span>
                                )}
                            </div>

                            <div className="input-wrapper">
                                <input
                                    type="text"
                                    placeholder="Логин"
                                    className={`form-input ${touched.login && errors.login ? 'error' : ''}`}
                                    name="login"
                                    value={formData.login}
                                    onChange={handleChange}
                                    onBlur={handleBlur}
                                    required
                                />
                                {touched.login && errors.login && (
                                    <span className="error-message">{errors.login}</span>
                                )}
                            </div>

                            <div className="input-wrapper">
                                <input
                                    type="password"
                                    placeholder="Пароль"
                                    className={`form-input ${touched.password && errors.password ? 'error' : ''}`}
                                    name="password"
                                    value={formData.password}
                                    onChange={handleChange}
                                    onBlur={handleBlur}
                                    required
                                />
                                {touched.password && errors.password && (
                                    <span className="error-message">{errors.password}</span>
                                )}
                            </div>

                            <div className="input-wrapper">
                                <input
                                    type="password"
                                    placeholder="Подтвердите пароль"
                                    className={`form-input ${touched.confirmPassword && errors.confirmPassword ? 'error' : ''}`}
                                    name="confirmPassword"
                                    value={formData.confirmPassword}
                                    onChange={handleChange}
                                    onBlur={handleBlur}
                                    required
                                />
                                {touched.confirmPassword && errors.confirmPassword && (
                                    <span className="error-message">{errors.confirmPassword}</span>
                                )}
                            </div>
                        </div>

                        <button type="submit" className="register-button">
                            Зарегистрироваться
                        </button>
                    </form>

                    <button className="login-button">
                        Уже есть аккаунт?
                    </button>
                </div>
            </div>
        </>
    );
};

export default Register;