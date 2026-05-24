export interface AuthResponse {
  token: string | null
  appealToken: string | null
}

export type UserType = 'STUDENT' | 'STAFF'
export type ApprovalStatus = 'PENDING' | 'APPROVED' | 'DENIED'
export type ReservationStatus = 'RESERVED' | 'WAITLISTED' | 'CANCELLED' | 'ATTENDED' | 'NO_SHOW'

export interface RegistrationResponse {
  approvalStatus: ApprovalStatus
  message: string
}

export interface UserResponse {
  id: number
  username: string
  email: string
  studentNumber: string | null
  phoneNumber: string | null
  dateOfBirth: string | null
  userType: UserType
  approvalStatus: ApprovalStatus
  isAdmin: boolean
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
  isOvernight: boolean
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

export type Gender = 'MALE' | 'FEMALE' | 'OTHER'
export type SportsClub = 'FOOTBALL' | 'BASKETBALL' | 'BADMINTON' | 'VOLLEYBALL'

export interface SportsClubSignupRequest {
  name: string
  email: string
  phoneNumber: string
  studentNumber: string
  course: string
  gender: Gender
  sportsClubs: SportsClub[]
}

export interface SportsClubSignupResponse {
  id: number
  name: string
  email: string
  phoneNumber: string
  studentNumber: string
  course: string
  gender: Gender
  sportsClubs: SportsClub[]
  userId: number | null
  createdAt: string
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
