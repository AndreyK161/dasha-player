import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchStreamStatus } from './api/songs'
import type { Song } from './types/song'
import './styles/shared.css'
import './App.css'

const ICECAST_URL = '/stream'

function App() {
  const [song, setSong] = useState<Song | null>(null)
  const [position, setPosition] = useState(0)
  const [playing, setPlaying] = useState(false)
  const audioRef = useRef<HTMLAudioElement | null>(null)
  const prevSongRef = useRef<string | null>(null)

  const reconnectAudio = () => {
    if (!audioRef.current) return
    audioRef.current.pause()
    const next = new Audio(`${ICECAST_URL}?t=${Date.now()}`)
    audioRef.current = next
    next.play().catch(() => {})
  }

  useEffect(() => {
    const poll = async () => {
      try {
        const data = await fetchStreamStatus()
        const newSong: Song | null = data.currentSong ?? null
        setSong(newSong)
        setPosition(data.positionSeconds ?? 0)

        const newKey = newSong ? `${newSong.songName}-${newSong.artist}` : null
        if (playing && newKey && newKey !== prevSongRef.current) reconnectAudio()
        prevSongRef.current = newKey
      } catch {
        setSong(null)
        setPosition(0)
      }
    }

    poll()
    const interval = setInterval(poll, 2000)
    return () => clearInterval(interval)
  }, [playing])

  useEffect(() => {
    if (!song?.duration) return
    const tick = setInterval(() => {
      setPosition(p => Math.min(p + 1, song.duration!))
    }, 1000)
    return () => clearInterval(tick)
  }, [song])

  const handleClick = async () => {
    if (!playing) {
      const audio = new Audio(`${ICECAST_URL}?t=${Date.now()}`)
      audioRef.current = audio
      await audio.play()
      setPlaying(true)
    } else {
      audioRef.current?.pause()
      audioRef.current = null
      setPlaying(false)
    }
  }

  const progress = song?.duration ? Math.min(position / song.duration, 1) : 0

  return (
    <div className="page">
      <header className="header">
        <span className={`live-indicator${song ? ' live-indicator--on' : ''}`}>
          <span className="live-dot" />
          {song ? 'в эфире' : 'эфир не идёт'}
        </span>
        <Link to="/admin" className="link">Войти</Link>
      </header>
      <main className="center">
        <div className="player">
          <h1 className="title title--clickable" onClick={handleClick}>dasha.</h1>
          <div className="progress-bar">
            <div className="progress-fill" style={{ width: `${progress * 100}%` }} />
          </div>
          <p className="track">
            {song ? `${song.songName} – ${song.artist}` : 'Unknown Track – Unknown Artist'}
          </p>
          <div className="links">
            <a href="https://t.me/pipakik" className="link">Telegram</a>
            <a href="https://vk.com/id658988396" className="link">VK</a>
          </div>
        </div>
      </main>
      <footer className="footer">
        All rights reserved. Unauthorized reproduction or distribution of any content is strictly prohibited. dasha. is a registered trademark. ©
      </footer>
    </div>
  )
}

export default App
