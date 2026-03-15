import { format, isSameDay } from 'date-fns'

export function formatDateRange(startAt?: string | null, endAt?: string | null): string {
  if (!startAt && !endAt) {
    return 'Schedule to be announced'
  }

  if (startAt && endAt) {
    const start = new Date(startAt)
    const end = new Date(endAt)

    if (isSameDay(start, end)) {
      return `${format(start, 'EEE, MMM d')} · ${format(start, 'p')} - ${format(end, 'p')}`
    }

    return `${format(start, 'EEE, MMM d, p')} - ${format(end, 'EEE, MMM d, p')}`
  }

  const value = new Date(startAt ?? endAt ?? '')
  return format(value, 'EEE, MMM d, p')
}

export function formatReservationWindow(opensAt?: string | null, closesAt?: string | null): string | null {
  if (!opensAt && !closesAt) {
    return null
  }

  if (opensAt && closesAt) {
    return `${format(new Date(opensAt), 'MMM d, p')} - ${format(new Date(closesAt), 'MMM d, p')}`
  }

  return format(new Date(opensAt ?? closesAt ?? ''), 'MMM d, p')
}

export function formatCapacity(capacity?: number | null): string {
  if (!capacity) {
    return 'Open capacity'
  }

  return `${capacity} spots`
}

export function formatLocation(locationName?: string | null, locationAddress?: string | null): string {
  const segments = [locationName, locationAddress].filter(Boolean)
  return segments.length > 0 ? segments.join(' • ') : 'Location to be announced'
}
