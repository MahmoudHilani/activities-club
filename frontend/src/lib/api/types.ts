export interface AuthResponse {
  token: string
}

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
  status: 'DRAFT' | 'PUBLISHED' | 'CANCELLED' | 'COMPLETED'
  visibility: 'PUBLIC' | 'PRIVATE'
  reservationOpensAt: string | null
  reservationClosesAt: string | null
  createdAt: string
  updatedAt: string
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
