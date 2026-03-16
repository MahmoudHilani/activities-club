export interface AuthResponse {
  token: string
}

export type ReservationStatus = 'RESERVED' | 'WAITLISTED' | 'CANCELLED' | 'ATTENDED' | 'NO_SHOW'

export interface UserResponse {
  id: number
  username: string
  email: string
  role: 'ADMIN' | 'STUDENT'
  createdAt: string
  updatedAt: string
}

export interface ActivityOrganizerResponse {
  id: number
  username: string
}

export interface ActivityResponse {
  id: number
  title: string
  description: string | null
  organizer: ActivityOrganizerResponse | null
  startAt: string | null
  endAt: string | null
  locationName: string | null
  locationAddress: string | null
  capacity: number | null
  imageUrl: string
  ticketPrice: string
  status: 'DRAFT' | 'PUBLISHED' | 'CANCELLED' | 'COMPLETED'
  visibility: 'PUBLIC' | 'PRIVATE'
  reservationOpensAt: string | null
  reservationClosesAt: string | null
  confirmedReservationCount: number
  waitlistCount: number
  availableSpots: number | null
  atCapacity: boolean
  currentUserReservationStatus: ReservationStatus | null
  createdAt: string
  updatedAt: string
}

export interface ReservationResponse {
  activityId: number
  status: ReservationStatus
  confirmedReservationCount: number
  waitlistCount: number
  availableSpots: number | null
  atCapacity: boolean
}

export interface AdminActivityReservationUserResponse {
  id: number
  username: string
  email: string
}

export interface AdminActivityReservationEntryResponse {
  id: number
  status: ReservationStatus
  reservedAt: string
  cancelledAt: string | null
  user: AdminActivityReservationUserResponse
}

export interface AdminActivityReservationsResponse {
  activity: ActivityResponse
  reservations: AdminActivityReservationEntryResponse[]
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  numberOfElements: number
  first: boolean
  last: boolean
  empty: boolean
}
