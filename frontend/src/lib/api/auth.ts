import type { UserType } from '@/lib/api/types'
import { apiClient } from '@/lib/api/client'
import type { AuthResponse, RegistrationResponse, UserResponse } from '@/lib/api/types'

export interface LoginPayload {
  email: string
  password: string
}

export interface RegisterPayload {
  username: string
  email: string
  userType: UserType
  studentNumber: string | null
  phoneNumber: string | null
  password: string
}

export interface RegistrationAppealPayload {
  username: string
  userType: UserType
  studentNumber: string | null
  phoneNumber: string | null
}

export async function login(payload: LoginPayload): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>('/api/auth/login', payload)
  return data
}

export async function register(payload: RegisterPayload): Promise<RegistrationResponse> {
  const { data } = await apiClient.post<RegistrationResponse>('/api/auth/register', payload)
  return data
}

export async function submitRegistrationAppeal(
  payload: RegistrationAppealPayload,
  appealToken: string,
): Promise<RegistrationResponse> {
  const { data } = await apiClient.patch<RegistrationResponse>('/api/auth/appeal', payload, {
    headers: {
      Authorization: `Bearer ${appealToken}`,
    },
  })
  return data
}

export async function getCurrentUser(): Promise<UserResponse> {
  const { data } = await apiClient.get<UserResponse>('/api/users/me')
  return data
}
