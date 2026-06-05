import type { Song } from '../types/song'
import { apiFetch } from './apiFetch'

export async function fetchSongs(): Promise<Song[]> {
  const res = await apiFetch('/api/songs')
  if (!res.ok) throw new Error(String(res.status))
  return res.json()
}

export async function uploadSong(file: File): Promise<void> {
  const form = new FormData()
  form.append('file', file)
  const res = await apiFetch('/api/songs/upload', { method: 'POST', body: form })
  if (!res.ok) throw new Error(String(res.status))
}

export async function deleteSong(fileKey: string): Promise<void> {
  const filename = fileKey.replace('songs/', '')
  const res = await apiFetch(`/api/songs?objectName=${encodeURIComponent(filename)}`, { method: 'DELETE' })
  if (res.status === 409) {
    const data = await res.json()
    throw new Error(data.error)
  }
  if (!res.ok) throw new Error(String(res.status))
}

export async function fetchStreamStatus() {
  const res = await fetch('/api/stream/status')
  if (!res.ok) throw new Error(String(res.status))
  return res.json()
}

export async function startStream(): Promise<void> {
  const res = await apiFetch('/api/stream/play-all', { method: 'POST' })
  if (!res.ok) throw new Error(String(res.status))
}

export async function stopStream(): Promise<void> {
  const res = await apiFetch('/api/stream/stop', { method: 'POST' })
  if (!res.ok) throw new Error(String(res.status))
}
