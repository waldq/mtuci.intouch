import { useRef, useEffect } from "react";
import { io } from 'socket.io-client';

export const useSocket = (chatId, onMessageReceived) => {
    const socketRef = useRef(null);

    useEffect(() => {
        const token = localStorage.getItem("access_token");

        if (!token) {
            console.error("Нет токена в localStorage! Сокет не запустится.");
            return;
        }

        socketRef.current = io('http://localhost:8000', {
            auth: { token:token },
            transports: ['websocket'],
        });

        const socket = socketRef.current;

        socket.on('connect', () => {
            console.log('Connected!');
            console.log(chatId)
            socket.emit('join_chat', chatId);
        });

        socket.on('receive_message', (data) => {
            if (onMessageReceived) {
                onMessageReceived(data);
            }
        });

        socket.on('connect_error', (err) => {
            console.error('Socket error:', err.message);
        });

        return () => {
            socket.emit('leave_room', chatId);
            socket.disconnect();
        };
    }, [chatId, onMessageReceived]);

    return socketRef.current;
};