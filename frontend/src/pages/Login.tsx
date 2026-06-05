import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { login } from '../api/auth'
import '../styles/shared.css'
import './Login.css'

function Login() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const navigate = useNavigate()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    try {
      await login(username, password)
      navigate('/admin/panel')
    } catch {
      setError('Неверный логин или пароль')
    }
  }

  return (
    <div className="page">
      <main className="center">
        <div className="login-card">
          <h1 className="title title--clickable">dasha.</h1>
          <form className="login-form" onSubmit={handleSubmit}>
            <input
              className="login-input"
              type="text"
              placeholder="Логин"
              value={username}
              onChange={e => setUsername(e.target.value)}
              autoComplete="username"
            />
            <input
              className="login-input"
              type="password"
              placeholder="Пароль"
              value={password}
              onChange={e => setPassword(e.target.value)}
              autoComplete="current-password"
            />
            {error && <p className="login-error">{error}</p>}
            <button className="login-button" type="submit">Войти</button>
          </form>
        </div>
      </main>
      <footer className="footer">
        All rights reserved. Unauthorized reproduction or distribution of any content is strictly prohibited. dasha. is a registered trademark. ©
      </footer>
    </div>
  )
}

export default Login
