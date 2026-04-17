# jazzlogs
Jazzlogs is an AI-powered assistant that recommends music based on a curated archive of jazz albums, originally shared through daily posts.

## Local infra

1. Copy `.env.example` to `.env` and fill in the values you need:

```bash
cp .env.example .env
```

2. Export the variables before running the app:

```bash
export $(grep -v '^#' .env | xargs)
```
3. Start PostgreSQL with Docker:

```bash
docker compose up -d
```

The container is exposed on `127.0.0.1:5433` to avoid conflicts with a local PostgreSQL running on `5432`.

4. Run the application:

```bash
./mvnw spring-boot:run
```

Flyway runs automatically on startup and creates the initial `album_logs` table.

Swagger UI will be available at:

```text
http://localhost:8080/swagger-ui.html
```

The generated OpenAPI document will be available at:

```text
http://localhost:8080/v3/api-docs
```

To run the JSON seed ingestion on startup:

```bash
./mvnw spring-boot:run -Djazzlogs.ingestion.json.enabled=true
```

The default seed path is `data/albums.json`.

To ingest a single album log manually through HTTP:

```bash
curl -X POST http://localhost:8080/admin/ingestion/album-logs \
  -H "Content-Type: application/json" \
  -H "X-Admin-Key: $JAZZLOGS_ADMIN_API_KEY" \
  -d '{
    "album": "Spunky",
    "artist": "Monty Alexander",
    "caption": "Welcome to jazz.logs...",
    "postedAt": "2026-02-02",
    "instagramPermalink": "https://www.instagram.com/p/DUReEzNEfyX/",
    "style": "Hard Bop / Soul Jazz",
    "logNumber": 1,
    "moods": ["energetic", "groovy", "warm"],
    "notes": "Standout track: Rattlesnake."
  }'
```

Set `JAZZLOGS_ADMIN_API_KEY` in `.env` before using the admin endpoints.

From Swagger UI you can already test:

- `POST /admin/ingestion/album-logs`
- `DELETE /admin/ingestion/album-logs/{logNumber}`
- `GET /logs`
- `GET /logs/{logNumber}`

## Spotify setup

For the Spotify playlist sync flow, configure:

```bash
SPOTIFY_CLIENT_ID=...
SPOTIFY_CLIENT_SECRET=...
SPOTIFY_PLAYLIST_ID=...
SPOTIFY_REDIRECT_URI=http://127.0.0.1:8080/admin/spotify/callback
SPOTIFY_SYNC_ENABLED=false
SPOTIFY_SYNC_CRON=0 0 1 * * *
SPOTIFY_SYNC_ZONE=America/Argentina/Buenos_Aires
SPOTIFY_SYNC_ON_STARTUP=false
```

The backend uses the Authorization Code flow on the server side and stores the refresh token so the playlist sync can keep working after app restarts.

Current Spotify admin flow:

1. Call `POST /admin/spotify/authorization-url` with `X-Admin-Key`
2. Open the returned URL in the browser and authorize the app with your Spotify account
3. Let Spotify redirect back to `/admin/spotify/callback`
4. Call `POST /admin/spotify/playlist-items/sync` with `X-Admin-Key`

This sync uses `GET /playlists/{playlist_id}/items` and stores a local Spotify catalog of albums and tracks for application use.

If you want the sync to run automatically every day, set `SPOTIFY_SYNC_ENABLED=true`. By default it is disabled. The default cron expression runs at `01:00` in `America/Argentina/Buenos_Aires`.

If you also want a full sync whenever the app starts, set `SPOTIFY_SYNC_ON_STARTUP=true`.

## Current foundations

- PostgreSQL via Docker Compose
- Flyway migrations for schema changes
- Spring Boot backend with JPA
- Stable JSON seed data in `data/albums.json`
- Basic CI pipeline with Maven tests

## Data conventions

`data/albums.json` is the editorial source of truth. Keep it focused on Jazzlogs metadata, not Spotify enrichment.

For `style`:

- Prefer concise musical labels such as `Hard Bop`, `Soul Jazz / Blues`, `Modal Jazz`, `Cool Jazz`
- Use title case
- Reuse existing wording when a similar album already exists

For `moods`:

- Treat them as lightweight editorial tags, not strict taxonomy
- Prefer lowercase
- Prefer short hyphenated forms when needed, such as `late-night`
- Reuse existing tags instead of inventing near-duplicates
- Current normalized examples include `relaxed`, `melancholic`, `warm`, `soulful`, `intimate`, `groovy`
