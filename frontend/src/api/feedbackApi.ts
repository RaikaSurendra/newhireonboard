import apiClient from './axiosConfig';

export interface Feedback {
  id: number;
  matchId: number;
  fromUserId: number;
  fromUserName: string;
  fromUserEmail: string;
  toUserId: number;
  toUserName: string;
  toUserEmail: string;
  rating: number;
  comments?: string;
  feedbackType: 'BUDDY_TO_EMPLOYEE' | 'EMPLOYEE_TO_BUDDY' | 'MANAGER_REVIEW';
  createdAt: string;
}

export interface SubmitFeedbackRequest {
  matchId: number;
  toUserId: number;
  rating: number;
  comments?: string;
  feedbackType: 'BUDDY_TO_EMPLOYEE' | 'EMPLOYEE_TO_BUDDY' | 'MANAGER_REVIEW';
}

export const feedbackApi = {
  getAll: (params?: { matchId?: number; fromUserId?: number; toUserId?: number; page?: number; limit?: number }) =>
    apiClient.get('/feedback', { params }),
  
  submit: (data: SubmitFeedbackRequest) =>
    apiClient.post('/feedback', data),
};
