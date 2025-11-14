import apiClient from './axiosConfig';

export interface Message {
  id: number;
  senderId: number;
  senderName: string;
  senderEmail: string;
  receiverId: number;
  receiverName: string;
  receiverEmail: string;
  content: string;
  isRead: boolean;
  createdAt: string;
  readAt?: string;
}

export interface SendMessageRequest {
  receiverId: number;
  content: string;
}

export const messageApi = {
  getAll: (params?: { type?: 'inbox' | 'sent'; otherUserId?: number; page?: number; limit?: number }) =>
    apiClient.get('/messages', { params }),
  
  getById: (id: number) =>
    apiClient.get(`/messages/${id}`),
  
  send: (data: SendMessageRequest) =>
    apiClient.post('/messages', data),
  
  markAsRead: (id: number) =>
    apiClient.put(`/messages/${id}/read`),
  
  delete: (id: number) =>
    apiClient.delete(`/messages/${id}`),
};
