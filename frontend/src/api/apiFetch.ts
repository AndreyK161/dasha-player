import { getToken, tryRefresh, clearTokens } from './auth'

export async function apiFetch(input: string, init: RequestInit = {}): Promise<Response> {
  const withAuth = (token: string): RequestInit => ({
    ...init,
    headers: { ...init.headers, Authorization: `Bearer ${token}` },
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
