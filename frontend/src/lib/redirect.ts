export function resolveRedirectPath(value: unknown, fallback = '/activities'): string {
  const candidate = Array.isArray(value) ? value[0] : value

  if (typeof candidate !== 'string' || !candidate.startsWith('/') || candidate.startsWith('//')) {
    return fallback
  }

  try {
    const redirectUrl = new URL(candidate, 'http://activities-club.local')
    return `${redirectUrl.pathname}${redirectUrl.search}${redirectUrl.hash}`
  } catch {
    return fallback
  }
}
