import apiClient from './axiosConfig';

export interface BuddyMatch {
  id: number;
  buddyUserId: number;
  buddyName: string;
  buddyEmail: string;
  newEmployeeId: number;
  employeeName: string;
  employeeEmail: string;
  status: 'PENDING' | 'SUGGESTED' | 'ACCEPTED' | 'ACTIVE' | 'COMPLETED' | 'ENDED';
  matchScore?: number;
  matchedAt: string;
  acceptedAt?: string;
  completedAt?: string;
  endedAt?: string;
  endReason?: string;
}

export interface CreateMatchRequest {
  buddyUserId: number;
  newEmployeeId: number;
  status?: string;
  matchScore?: number;
}

export const buddyMatchApi = {
  getAll: (params?: { buddyId?: number; employeeId?: number; status?: string; page?: number; limit?: number }) =>
    apiClient.get('/matches', { params }),
  
  getById: (id: number) =>
    apiClient.get(`/matches/${id}`),
  
  create: (data: CreateMatchRequest) =>
    apiClient.post('/matches', data),
  
  accept: (id: number) =>
    apiClient.put(`/matches/${id}/accept`),
  
  complete: (id: number) =>
    apiClient.put(`/matches/${id}/complete`),
  
  update: (id: number, data: Partial<CreateMatchRequest>) =>
    apiClient.put(`/matches/${id}`, data),
  
  end: (id: number, endReason?: string) =>
    apiClient.delete(`/matches/${id}`, { data: { endReason } }),
};
