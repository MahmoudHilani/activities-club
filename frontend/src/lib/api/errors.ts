import axios from 'axios'

interface ProblemPayload {
  detail?: string
  message?: string
  title?: string
}

export function getApiStatus(error: unknown): number | null {
  if (!axios.isAxiosError(error)) {
    return null
  }

  return error.response?.status ?? null
}

export function getApiMessage(error: unknown): string | null {
  if (!axios.isAxiosError<ProblemPayload>(error)) {
    return null
  }

  const data = error.response?.data
  return data?.detail ?? data?.message ?? data?.title ?? null
}

export function mapLoginError(error: unknown): string {
  const status = getApiStatus(error)

  if (status === 401) {
    return 'Email or password is incorrect.'
  }

  return getApiMessage(error) ?? 'Unable to sign in right now. Please try again.'
}

export function mapRegisterError(error: unknown): string {
  const status = getApiStatus(error)

  if (status === 409) {
    return getApiMessage(error) ?? 'That email or username is already in use.'
  }

  if (status === 400) {
    return getApiMessage(error) ?? 'Please check your details and try again.'
  }

  return 'Unable to create your account right now. Please try again.'
}

export function mapActivitiesError(error: unknown): string {
  return getApiMessage(error) ?? 'We could not load activities right now.'
}

export function mapAdminActivityError(error: unknown): string {
  return getApiMessage(error) ?? 'We could not save the activity right now.'
}

export function mapReservationError(error: unknown): string {
  const status = getApiStatus(error)

  if (status === 401) {
    return 'Your session has expired. Please log in again to reserve a place.'
  }

  return getApiMessage(error) ?? 'We could not update your reservation right now.'
}
