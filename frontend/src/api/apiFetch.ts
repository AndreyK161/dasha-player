import { getToken, tryRefresh, clearTokens } from './auth'

export async function apiFetch(input: string, init: RequestInit = {}): Promise<Response> {
  const baseHeaders: Record<string, string> =
    init.headers instanceof Headers
      ? Object.fromEntries(init.headers.entries())
      : (init.headers as Record<string, string>) ?? {}

  const withAuth = (token: string): RequestInit => ({
    ...init,
    headers: { ...baseHeaders, Authorization: `Bearer ${token}` },
  })

  let res = await fetch(input, withAuth(getToken() ?? ''))

  if (res.status === 401 || res.status === 403) {
    const refreshed = await tryRefresh()
    if (refreshed) {
      res = await fetch(input, withAuth(getToken() ?? ''))
    } else {
      clearTokens()
      window.location.href = '/admin'
    }
  }

  return res
}
