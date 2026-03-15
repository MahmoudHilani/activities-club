import { apiClient } from '@/lib/api/client'
import type { ActivityResponse, PageResponse } from '@/lib/api/types'

export interface PublicActivitiesQuery {
  page: number
  size?: number
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
