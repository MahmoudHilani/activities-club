import { apiClient } from '@/lib/api/client'
import type {
  AdminActivityReservationsResponse,
  ActivityResponse,
  PageResponse,
  ReservationResponse,
} from '@/lib/api/types'

export interface PublicActivitiesQuery {
  page: number
  size?: number
}

export interface AdminActivitiesQuery {
  page?: number
  size?: number
}

export interface ActivityUpsertPayload {
  title: string
  description: string | null
  startAt: string | null
  endAt: string | null
  locationName: string | null
  locationAddress: string | null
  capacity: number | null
  ticketPrice: number
  visibility: 'PUBLIC' | 'PRIVATE'
  reservationOpensAt: string | null
  reservationClosesAt: string | null
}

export async function getPublicActivities({
  page,
  size = 12,
}: PublicActivitiesQuery): Promise<PageResponse<ActivityResponse>> {
  const params = new URLSearchParams()
  params.set('page', String(page))
  params.set('size', String(size))
  params.append('sort', 'startAt,asc')
  params.append('sort', 'createdAt,desc')

  const { data } = await apiClient.get<PageResponse<ActivityResponse>>(
    `/api/activities/public?${params.toString()}`,
  )

  return data
}

export async function getAdminActivities({
  page = 0,
  size = 100,
}: AdminActivitiesQuery = {}): Promise<PageResponse<ActivityResponse>> {
  const params = new URLSearchParams()
  params.set('page', String(page))
  params.set('size', String(size))
  params.append('sort', 'createdAt,desc')

  const { data } = await apiClient.get<PageResponse<ActivityResponse>>(
    `/api/admin/activities?${params.toString()}`,
  )

  return data
}

export async function createAdminActivity(
  payload: ActivityUpsertPayload,
  image: File,
): Promise<ActivityResponse> {
  const { data } = await apiClient.post<ActivityResponse>(
    '/api/admin/activities',
    buildActivityFormData(payload, image),
  )

  return data
}

export async function updateAdminActivity(
  activityId: number,
  payload: ActivityUpsertPayload,
  image?: File | null,
): Promise<ActivityResponse> {
  const { data } = await apiClient.put<ActivityResponse>(
    `/api/admin/activities/${activityId}`,
    buildActivityFormData(payload, image ?? undefined),
  )

  return data
}

export async function publishAdminActivity(activityId: number): Promise<ActivityResponse> {
  const { data } = await apiClient.patch<ActivityResponse>(
    `/api/admin/activities/${activityId}/publish`,
  )
  return data
}

export async function cancelAdminActivity(activityId: number): Promise<ActivityResponse> {
  const { data } = await apiClient.patch<ActivityResponse>(
    `/api/admin/activities/${activityId}/cancel`,
  )
  return data
}

export async function deleteAdminActivity(activityId: number): Promise<void> {
  await apiClient.delete(`/api/admin/activities/${activityId}`)
}

export async function getAdminActivityReservations(
  activityId: number,
): Promise<AdminActivityReservationsResponse> {
  const { data } = await apiClient.get<AdminActivityReservationsResponse>(
    `/api/admin/activities/${activityId}/reservations`,
  )

  return data
}

export async function reserveActivity(activityId: number): Promise<ReservationResponse> {
  const { data } = await apiClient.post<ReservationResponse>(
    `/api/activities/${activityId}/reservations`,
  )
  return data
}

export async function cancelReservation(activityId: number): Promise<ReservationResponse> {
  const { data } = await apiClient.delete<ReservationResponse>(
    `/api/activities/${activityId}/reservations/me`,
  )
  return data
}

function buildActivityFormData(payload: ActivityUpsertPayload, image?: File): FormData {
  const formData = new FormData()
  formData.append(
    'activity',
    new Blob([JSON.stringify(payload)], {
      type: 'application/json',
    }),
  )

  if (image) {
    formData.append('image', image)
  }

  return formData
}
