import { Navigate } from 'react-router-dom'
import { getToken, getRefreshToken } from '../api/auth'

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  if (!getToken() && !getRefreshToken()) return <Navigate to="/admin" replace />
  return <>{children}</>
}

export default ProtectedRoute
