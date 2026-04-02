import { Helmet } from "react-helmet";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import './login.css';

const Login = () => {

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

                    <form className="log-form">
                        <div className="log-forms">
                            <div className="log-input-wrapper">
                                <input
                                    type="text"
                                    placeholder="Логин"
                                    className="log-form-input"
                                    required
                                />
                            </div>

                            <div className="log-input-wrapper">
                                <input
                                    type="password"
                                    placeholder="Пароль"
                                    className="log-form-input"
                                    required
                                />
                            </div>
                        </div>

                        <button type="submit" className="log-submit-btn">
                            Войти
                        </button>
                    </form>

                    <button className="log-register-btn">
                        Создать аккаунт
                    </button>
                </div>
            </div>
        </>
    );
};

export default Login;