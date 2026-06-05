export const getToken = () => localStorage.getItem('token')
export const setToken = (token: string) => localStorage.setItem('token', token)
export const getRefreshToken = () => localStorage.getItem('refreshToken')
export const setRefreshToken = (token: string) => localStorage.setItem('refreshToken', token)
export const clearTokens = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
}

export async function login(username: string, password: string): Promise<void> {
  const res = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  })
  if (!res.ok) throw new Error(String(res.status))
  const data = await res.json()
  setToken(data.token)
  setRefreshToken(data.refreshToken)
}

export async function tryRefresh(): Promise<boolean> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) return false
  try {
    const res = await fetch('/api/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    })
    if (!res.ok) return false
    const data = await res.json()
    setToken(data.token)
    setRefreshToken(data.refreshToken)
    return true
  } catch {
    return false
  }
}
