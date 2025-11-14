import apiClient from './axiosConfig';

export interface TaskTemplate {
  id: number;
  planId: number;
  planVersion: number;
  name: string;
  description: string;
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
  dayOffset: number;
  estimatedDuration: number;
  taskType: 'ADMINISTRATIVE' | 'TECHNICAL' | 'COMPLIANCE' | 'SOCIAL' | 'PROJECT' | 'REVIEW';
  ownerType: 'MANAGER_OWNED' | 'NEW_HIRE_OWNED' | 'INTERNAL_TEAM_OWNED' | 'INTERNAL_EMPLOYEE_OWNED' | 'EXTERNAL_TEAM_OWNED' | 'SHARED_OWNERSHIP';
  assigneeType: 'NEW_EMPLOYEE' | 'BUDDY' | 'MANAGER' | 'HR_TEAM' | 'IT_TEAM' | 'TEAM_MEMBER' | 'TEAM' | 'EXTERNAL_VENDOR';
  executionMode: 'SEQUENTIAL' | 'PARALLEL';
  sequenceOrder?: number;
  parallelGroup?: string;
  category?: string;
  tags?: string[];
}

export interface CreateTaskTemplateRequest {
  name: string;
  description: string;
  priority: string;
  dayOffset: number;
  estimatedDuration: number;
  taskType: string;
  ownerType: string;
  assigneeType: string;
  executionMode: string;
  sequenceOrder?: number;
  parallelGroup?: string;
  category?: string;
  tags?: string[];
}

export interface Task {
  id: number;
  title: string;
  description: string;
  status: 'PENDING' | 'IN_PROGRESS' | 'BLOCKED' | 'COMPLETED' | 'CANCELLED';
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
  dueDate: string;
  assignedTo: number;
  createdBy: number;
  completedAt?: string;
}

export const taskApi = {
  // Task Templates
  getTemplatesByPlan: (planId: number) =>
    apiClient.get(`/templates?planId=${planId}`),
  
  createTemplate: (planId: number, data: CreateTaskTemplateRequest) =>
    apiClient.post(`/templates`, { ...data, planId }),
  
  updateTemplate: (_planId: number, templateId: number, data: Partial<CreateTaskTemplateRequest>) =>
    apiClient.put(`/templates/${templateId}`, data),
  
  deleteTemplate: (_planId: number, templateId: number) =>
    apiClient.delete(`/templates/${templateId}`),
  
  // Tasks
  getMyTasks: (params?: { status?: string; priority?: string }) =>
    apiClient.get('/tasks', { params }),
  
  getTaskById: (id: number) =>
    apiClient.get(`/tasks/${id}`),
  
  updateTaskStatus: (id: number, status: string) =>
    apiClient.put(`/tasks/${id}/status`, { status }),
  
  completeTask: (id: number) =>
    apiClient.post(`/tasks/${id}/complete`),
};
