import { apiClient } from '@/lib/api/client'
import type { UserResponse } from '@/lib/api/types'

export async function getAdminUsers(): Promise<UserResponse[]> {
  const { data } = await apiClient.get<UserResponse[]>('/api/admin/users')
  return data
}

export async function updateAdminUserAccess(userId: number, isAdmin: boolean): Promise<UserResponse> {
  const { data } = await apiClient.patch<UserResponse>(`/api/admin/users/${userId}/admin`, {
    isAdmin,
  })
  return data
}
