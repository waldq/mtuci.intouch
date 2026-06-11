import { useEffect, useState } from 'react';
import io from 'socket.io-client';

export const useSocket = (roomId, onMessageReceived) => {
  const [socket, setSocket] = useState(null);

  useEffect(() => {
    const token = localStorage.getItem('access_token');
    if (!token) return;

    const newSocket = io(process.env.REACT_APP_API_URL, {
      transports: ['websocket'],
      auth: { token }
    });

    newSocket.on('connect', () => {
      console.log('Socket connected');
      if (roomId) {
        newSocket.emit('join_room', { room_id: roomId });
      }
    });

    newSocket.on('receive_message', (data) => {
      console.log('Message received:', data);
      if (onMessageReceived) {
        onMessageReceived(data);
      }
    });

    newSocket.on('connect_error', (error) => {
      console.error('Connection error:', error);
    });

    setSocket(newSocket);

    return () => {
      newSocket.disconnect();
    };
  }, [roomId]);

  return socket;
};