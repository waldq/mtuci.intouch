import { useRef, useEffect } from "react";
import { io } from 'socket.io-client';

export const useSocket = (chatId, onMessageReceived) => {
    const socketRef = useRef(null);
    
    const onMessageReceivedRef = useRef(onMessageReceived);

    useEffect(() => {
        onMessageReceivedRef.current = onMessageReceived;
    }, [onMessageReceived]);

    useEffect(() => {
        const token = localStorage.getItem("access_token");

        if (!token) {
            console.error("Нет токена в localStorage! Сокет не запустится.");
            return;
        }

        socketRef.current = io(`${process.env.REACT_APP_API_URL}`, {
            auth: { token: token },
            transports: ['websocket'],
        });

        const socket = socketRef.current;

        socket.on('connect', () => {
            console.log('Сессия сокета открыта');
        });

        socket.on('receive_message', (data) => {
            if (onMessageReceivedRef.current) {
                onMessageReceivedRef.current(data);
            }
        });

        socket.on('connect_error', (err) => {
            console.error('Socket error:', err.message);
        });

        return () => {
            console.log('Полное закрытие сокета');
            socket.disconnect();
        };
    }, []);

    useEffect(() => {
        const socket = socketRef.current;
        if (!socket) return;

        if (chatId) {
            if (socket.connected) {
                console.log('Вход в чат:', chatId);
                socket.emit('join_chat', chatId);
            }

            const handleConnect = () => {
                console.log("Вход в комнату после переподключения сокета:", chatId);
                socket.emit('join_chat', chatId);
            };
            socket.on('connect', handleConnect);

            return () => {
                console.log('Покидание комнаты:', chatId);
                socket.emit('leave_chat', chatId); 
                socket.off('connect', handleConnect);
            };
        }
    }, [chatId]);

    return socketRef.current;
};