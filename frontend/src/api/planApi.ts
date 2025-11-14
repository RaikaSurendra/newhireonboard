import apiClient from './axiosConfig';

export interface Plan {
  id: number;
  name: string;
  description: string;
  version: number;
  department: string;
  durationDays: number;
  isActive: boolean;
  templateCount?: number;
  usageCount?: number;
  createdBy: {
    id: number;
    name: string;
  };
  publishedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePlanRequest {
  name: string;
  description: string;
  department: string;
  durationDays: number;
}

export interface UpdatePlanRequest {
  name: string;
  description: string;
  durationDays: number;
  changeReason: string;
}

export const planApi = {
  getAll: (params?: { department?: string; isActive?: boolean; search?: string }) =>
    apiClient.get('/plans', { params }),
  
  getById: (id: number, version?: number) =>
    apiClient.get<any, { success: boolean; data: Plan }>(`/plans/${id}`, { 
      params: { version } 
    }),
  
  create: (data: CreatePlanRequest) =>
    apiClient.post<any, { success: boolean; data: Plan }>('/plans', data),
  
  update: (id: number, data: UpdatePlanRequest) =>
    apiClient.put<any, { success: boolean; data: Plan }>(`/plans/${id}`, data),
  
  publish: (id: number, publishNotes: string) =>
    apiClient.put(`/plans/${id}/publish`, { publishNotes }),
  
  delete: (id: number) =>
    apiClient.delete(`/plans/${id}`),
  
  getVersions: (id: number) =>
    apiClient.get(`/plans/${id}/versions`),
};
