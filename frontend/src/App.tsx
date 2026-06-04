import { useEffect, useRef, useState } from 'react'
import './App.css'

const ICECAST_URL = '/stream'

interface Song {
  songName: string
  artist: string
  duration: number | null
}

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
    const fetchStatus = async () => {
      try {
        const res = await fetch('/api/stream/status')
        const data = await res.json()
        const newSong: Song | null = data.currentSong ?? null
        setSong(newSong)
        setPosition(data.positionSeconds ?? 0)

        const newKey = newSong ? `${newSong.songName}-${newSong.artist}` : null
        if (playing && newKey && newKey !== prevSongRef.current) {
          reconnectAudio()
        }
        prevSongRef.current = newKey
      } catch {
        setSong(null)
        setPosition(0)
      }
    }

    fetchStatus()
    const poll = setInterval(fetchStatus, 2000)
    return () => clearInterval(poll)
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
      <main className="center">
        <div className="player">
          <h1 className="title" onClick={handleClick}>dasha.</h1>
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
