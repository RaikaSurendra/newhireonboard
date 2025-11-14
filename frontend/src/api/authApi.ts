import apiClient from './axiosConfig';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  name: string;
  role: string;
  department?: string;
}

export interface AuthResponse {
  success: boolean;
  data: {
    token: string;
    refreshToken: string;
    user: {
      id: number;
      email: string;
      name: string;
      role: string;
    };
  };
}

export const authApi = {
  login: (data: LoginRequest) => 
    apiClient.post('/auth/login', data),
  
  register: (data: RegisterRequest) => 
    apiClient.post('/auth/register', data),
  
  logout: () => 
    apiClient.post('/auth/logout'),
  
  refreshToken: (refreshToken: string) => 
    apiClient.post('/auth/refresh-token', { refreshToken }),
  
  getCurrentUser: () => 
    apiClient.get('/auth/me'),
};
