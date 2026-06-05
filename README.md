# dasha.

Персональное интернет-радио. Стримит музыку через Icecast, треки хранятся в MinIO. Управляется через веб-админку.

<img width="1920" height="507" alt="Снимок экрана 2026-06-06 в 00 22 23" src="https://github.com/user-attachments/assets/99a33a8b-c95c-449b-9c01-54f2e9913966" />

## Стек

| Слой | Технологии |
|------|-----------|
| Backend | Java 21, Spring Boot 3, Spring Security, JWT |
| Frontend | React, TypeScript, Vite |
| Стриминг | Icecast2 |
| Хранилище | MinIO (S3-compatible) |
| Инфраструктура | Docker Compose |

## Структура

```
├── backend/       # Spring Boot приложение
│   ├── Dockerfile
│   └── docker-compose.yml
└── frontend/      # React приложение
```

## Запуск

### Требования

- Docker & Docker Compose
- Node.js 18+ (для локальной разработки фронтенда)

### 1. Переменные окружения

```bash
cp backend/.env-example backend/.env
```

Заполнить `backend/.env`:

```env
MINIO_USERNAME=admin
MINIO_PASSWORD=password123
MINIO_ACCESS=access_key
MINIO_SECRET=secret_key
ICECAST_MOUNT=stream
ICECAST_SOURCE_PASSWORD=hackme
ICECAST_ADMIN_PASSWORD=admin
ICECAST_RELAY_PASSWORD=relay
```

Также нужен `JWT_SECRET` и `ADMIN_USERNAME` / `ADMIN_PASSWORD` — добавить в `.env` или передать через environment.

### 2. Запуск бэкенда

```bash
cd backend
docker compose up -d
```

Сервисы поднимутся на:
- `http://localhost:8080` — Spring Boot API
- `http://localhost:8000` — Icecast (стрим)
- `http://localhost:9000` — MinIO S3 API
- `http://localhost:9001` — MinIO Web UI

### 3. Запуск фронтенда

```bash
cd frontend
npm install
npm run dev
```

Открыть `http://localhost:5173`

## API

| Метод | Endpoint | Доступ | Описание |
|-------|----------|--------|----------|
| POST | `/api/auth/login` | Публичный | Получить access + refresh токен |
| POST | `/api/auth/refresh` | Публичный | Обновить токены |
| GET | `/api/stream/status` | Публичный | Текущий трек и позиция |
| GET | `/api/stream/songs` | Публичный | Список треков |
| POST | `/api/stream/songs` | Админ | Загрузить трек |
| DELETE | `/api/stream/songs` | Админ | Удалить трек |
| POST | `/api/player/start` | Админ | Запустить эфир |
| POST | `/api/player/stop` | Админ | Остановить эфир |

## Авторизация

JWT с коротким временем жизни (15 мин) и refresh токеном (7 дней). Удалить трек, который сейчас играет в эфире — невозможно (вернёт 409).
