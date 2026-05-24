import { apiClient } from '@/lib/api/client'
import type {
  PageResponse,
  SportsClubSignupRequest,
  SportsClubSignupResponse,
} from '@/lib/api/types'

export async function submitSportsClubSignup(
  payload: SportsClubSignupRequest,
): Promise<SportsClubSignupResponse> {
  const { data } = await apiClient.post<SportsClubSignupResponse>(
    '/api/sports-club-signups',
    payload,
  )
  return data
}

export interface AdminSportsClubSignupsQuery {
  page?: number
  size?: number
}

export async function getAdminSportsClubSignups({
  page = 0,
  size = 50,
}: AdminSportsClubSignupsQuery = {}): Promise<PageResponse<SportsClubSignupResponse>> {
  const params = new URLSearchParams()
  params.set('page', String(page))
  params.set('size', String(size))
  params.append('sort', 'createdAt,desc')

  const { data } = await apiClient.get<PageResponse<SportsClubSignupResponse>>(
    `/api/admin/sports-club-signups?${params.toString()}`,
  )
  return data
}
