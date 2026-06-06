import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { fetchSongs, uploadSong, deleteSong, fetchStreamStatus, startStream, stopStream } from '../api/songs'
import type { Song } from '../types/song'
import '../styles/shared.css'
import './AdminPanel.css'

function formatDuration(seconds: number | null): string {
  if (!seconds) return '—'
  const m = Math.floor(seconds / 60)
  const s = Math.floor(seconds % 60)
  return `${m}:${s.toString().padStart(2, '0')}`
}

function AdminPanel() {
  const [songs, setSongs] = useState<Song[]>([])
  const [search, setSearch] = useState('')
  const [dragging, setDragging] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [deleteError, setDeleteError] = useState<string | null>(null)
  const [deleteErrorHidden, setDeleteErrorHidden] = useState(false)
  const [uploadError, setUploadError] = useState<string | null>(null)
  const [uploadErrorHidden, setUploadErrorHidden] = useState(false)
  const [streaming, setStreaming] = useState(false)
  const [currentSong, setCurrentSong] = useState<Song | null>(null)
  const [streamLoading, setStreamLoading] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const navigate = useNavigate()

  const loadSongs = async () => {
    try {
      setSongs(await fetchSongs())
    } catch (e: any) {
      if (e.message === '401' || e.message === '403') navigate('/admin')
    }
  }

  const loadStatus = async () => {
    try {
      const data = await fetchStreamStatus()
      setStreaming(data.streaming)
      setCurrentSong(data.currentSong ?? null)
    } catch {}
  }

  useEffect(() => {
    loadSongs()
    loadStatus()
    const interval = setInterval(loadStatus, 3000)
    return () => clearInterval(interval)
  }, [])

  const handleStreamToggle = async () => {
    setStreamLoading(true)
    try {
      if (streaming) {
        await stopStream()
      } else {
        await startStream()
      }
      await loadStatus()
    } finally {
      setStreamLoading(false)
    }
  }

  const showUploadError = (msg: string) => {
    setUploadError(msg)
    setUploadErrorHidden(false)
    setTimeout(() => setUploadErrorHidden(true), 2500)
    setTimeout(() => setUploadError(null), 3000)
  }

  const handleUpload = async (file: File) => {
    if (!file.name.toLowerCase().endsWith('.mp3')) {
      showUploadError('Только .mp3 файлы')
      return
    }
    setUploading(true)
    try {
      await uploadSong(file)
      await loadSongs()
    } catch (e: any) {
      showUploadError(e.message === '400' ? 'Файл повреждён или не является MP3' : 'Ошибка загрузки')
    } finally {
      setUploading(false)
      if (fileInputRef.current) fileInputRef.current.value = ''
    }
  }

  const handleDelete = async (fileKey: string) => {
    try {
      setDeleteError(null)
      await deleteSong(fileKey)
      await loadSongs()
    } catch (e: any) {
      setDeleteError(e.message)
      setDeleteErrorHidden(false)
      setTimeout(() => setDeleteErrorHidden(true), 2500)
      setTimeout(() => setDeleteError(null), 3000)
    }
  }

  const onDrop = (e: React.DragEvent) => {
    e.preventDefault()
    setDragging(false)
    const file = e.dataTransfer.files[0]
    if (file) handleUpload(file)
  }

  return (
    <div className="page page--fixed">
      <main className="panel-main">
        <h1 className="title title--clickable">dasha.</h1>

        <div className="stream-control">
          <span className="stream-track">
            {streaming && currentSong ? `${currentSong.songName} – ${currentSong.artist}` : ' '}
          </span>
          <button
            className={`stream-btn${streaming ? ' stream-btn--live' : ''}`}
            onClick={handleStreamToggle}
            disabled={streamLoading}
          >
            {streamLoading ? '...' : streaming ? 'в эфире' : 'запустить'}
          </button>
        </div>

        <div
          className={`drop-zone${dragging ? ' drop-zone--active' : ''}${uploading ? ' drop-zone--uploading' : ''}`}
          onDragOver={e => { e.preventDefault(); setDragging(true) }}
          onDragLeave={() => setDragging(false)}
          onDrop={onDrop}
          onClick={() => fileInputRef.current?.click()}
        >
          <span>{uploading ? 'Загружаю...' : 'Перетащи mp3 или нажми'}</span>
          <input
            ref={fileInputRef}
            type="file"
            accept=".mp3"
            hidden
            onChange={e => { if (e.target.files?.[0]) handleUpload(e.target.files[0]) }}
          />
        </div>

        {uploadError && (
          <p className={`delete-error${uploadErrorHidden ? ' delete-error--hidden' : ''}`}>
            {uploadError}
          </p>
        )}
        {deleteError && (
          <p className={`delete-error${deleteErrorHidden ? ' delete-error--hidden' : ''}`}>
            {deleteError}
          </p>
        )}

        <input
          className="search-input"
          type="text"
          placeholder="Поиск..."
          value={search}
          onChange={e => setSearch(e.target.value)}
        />

        <div className="song-list">
          {(() => {
            const filtered = songs.filter(s => {
              const q = search.toLowerCase()
              return s.songName.toLowerCase().includes(q) || s.artist.toLowerCase().includes(q)
            })
            if (songs.length === 0) return <p className="empty">Треков нет</p>
            if (filtered.length === 0) return <p className="empty">Ничего не найдено</p>
            return filtered.map(song => (
              <div key={song.fileKey} className="song-row">
                <div className="song-info">
                  <span className="song-name">{song.songName}</span>
                  <span className="song-artist">{song.artist}</span>
                </div>
                <div className="song-right">
                  <span className="song-duration">{formatDuration(song.duration)}</span>
                  <button className="delete-btn" onClick={() => handleDelete(song.fileKey)}>✕</button>
                </div>
              </div>
            ))
          })()}
        </div>
      </main>
      <footer className="footer">
        All rights reserved. Unauthorized reproduction or distribution of any content is strictly prohibited. dasha. is a registered trademark. ©
      </footer>
    </div>
  )
}

export default AdminPanel
