import type { ActivityResponse, PageResponse, UserResponse } from '@/lib/api/types'

export const sampleUser: UserResponse = {
  id: 1,
  username: 'alice',
  email: 'alice@example.com',
  role: 'STUDENT',
  createdAt: '2026-03-15T10:00:00Z',
  updatedAt: '2026-03-15T10:00:00Z',
}

export function buildActivity(overrides: Partial<ActivityResponse> = {}): ActivityResponse {
  return {
    id: 1,
    title: 'Chess Night',
    description: 'Weekly chess meetup',
    organizer: {
      id: 1,
      username: 'alice',
    },
    startAt: '2026-03-20T18:00:00Z',
    endAt: '2026-03-20T20:00:00Z',
    locationName: 'Student Center',
    locationAddress: 'Main Campus',
    capacity: 20,
    imageUrl: 'http://localhost:8080/uploads/chess-night.jpg',
    ticketPrice: '12.50',
    status: 'PUBLISHED',
    visibility: 'PUBLIC',
    reservationOpensAt: '2026-03-18T18:00:00Z',
    reservationClosesAt: '2026-03-20T17:00:00Z',
    confirmedReservationCount: 8,
    waitlistCount: 0,
    availableSpots: 12,
    atCapacity: false,
    currentUserReservationStatus: null,
    createdAt: '2026-03-15T10:00:00Z',
    updatedAt: '2026-03-15T10:00:00Z',
    ...overrides,
  }
}

export function buildPage<T>(
  content: T[],
  overrides: Partial<PageResponse<T>> = {},
): PageResponse<T> {
  return {
    content,
    totalElements: content.length,
    totalPages: content.length === 0 ? 0 : 1,
    size: 12,
    number: 0,
    numberOfElements: content.length,
    first: true,
    last: true,
    empty: content.length === 0,
    ...overrides,
  }
}
