import { apiClient } from '@/lib/api/client'
import type { ApprovalStatus, UserResponse } from '@/lib/api/types'

export async function getAdminUsers(): Promise<UserResponse[]> {
  const { data } = await apiClient.get<UserResponse[]>('/api/admin/users')
  return data
}

export async function updateUserApprovalStatus(
  userId: number,
  approvalStatus: Exclude<ApprovalStatus, 'PENDING'>,
  dateOfBirth?: string | null,
): Promise<UserResponse> {
  const { data } = await apiClient.patch<UserResponse>(`/api/admin/users/${userId}/approval`, {
    approvalStatus,
    dateOfBirth,
  })
  return data
}

export async function updateAdminUserAccess(userId: number, isAdmin: boolean): Promise<UserResponse> {
  const { data } = await apiClient.patch<UserResponse>(`/api/admin/users/${userId}/admin`, {
    isAdmin,
  })
  return data
}

export async function updateUserDateOfBirth(
  userId: number,
  dateOfBirth: string | null,
): Promise<UserResponse> {
  const { data } = await apiClient.patch<UserResponse>(`/api/admin/users/${userId}/date-of-birth`, {
    dateOfBirth,
  })
  return data
}
