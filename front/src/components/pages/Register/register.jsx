import { Helmet } from 'react-helmet';
import './register.css';

const Register = () => {
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

                    <form className="register-form">
                        <div className="forms">
                            <input 
                                type="text" 
                                placeholder="Имя пользователя" 
                                className="form-input"
                            />
                            <input 
                                type="text" 
                                placeholder="Логин" 
                                className="form-input"
                            />
                            <input 
                                type="password" 
                                placeholder="Пароль" 
                                className="form-input"
                            />
                            <input 
                                type="password" 
                                placeholder="Подтвердите пароль" 
                                className="form-input"
                            />
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