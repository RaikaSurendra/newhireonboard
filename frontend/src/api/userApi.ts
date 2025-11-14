import apiClient from './axiosConfig';

export interface User {
  id: number;
  email: string;
  name: string;
  role: string;
  department?: string;
  status: string;
  phone?: string;
  experienceLevel?: string;
  avatarUrl?: string;
  createdAt?: string;
  lastLogin?: string;
}

export interface UserListResponse {
  success: boolean;
  data: User[];
  page: number;
  limit: number;
}

export interface UserResponse {
  success: boolean;
  data: User;
}

export interface ErrorResponse {
  success: boolean;
  error: string;
}

const userApi = {
  /**
   * Get all users with pagination and filters
   */
  getUsers: (page = 1, limit = 20, filters?: {
    role?: string;
    department?: string;
    status?: string;
  }) => {
    const params = new URLSearchParams({
      page: page.toString(),
      limit: limit.toString(),
    });
    
    if (filters?.role) params.append('role', filters.role);
    if (filters?.department) params.append('department', filters.department);
    if (filters?.status) params.append('status', filters.status);
    
    return apiClient.get<UserListResponse>(`/users?${params}`);
  },

  /**
   * Get single user by ID
   */
  getUser: (id: number) => {
    return apiClient.get<UserResponse>(`/users/${id}`);
  },

  /**
   * Update user
   */
  updateUser: (id: number, data: Partial<User>) => {
    return apiClient.put<{ success: boolean; message: string }>(`/users/${id}`, data);
  },

  /**
   * Delete user (soft delete - sets status to INACTIVE)
   */
  deleteUser: (id: number) => {
    return apiClient.delete<{ success: boolean; message: string }>(`/users/${id}`);
  },
};

export default userApi;
