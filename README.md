# jazzlogs
Jazzlogs is an AI-powered assistant in progress that recommends music from a curated archive of album logs, enriched with Spotify catalog data from one canonical playlist.

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

Flyway runs automatically on startup and applies the full schema, including the editorial `album_logs` table and the Spotify catalog tables.

Swagger UI will be available at:

```text
http://localhost:8080/swagger-ui.html
```

The generated OpenAPI document will be available at:

```text
http://localhost:8080/v3/api-docs
```

JSON seed ingestion is enabled by default on startup and reads from `data/albums.json`.

To disable it temporarily:

```bash
./mvnw spring-boot:run -Djazzlogs.ingestion.json.enabled=false
```

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
    "notes": "Standout track: Rattlesnake.",
    "spotifyAlbumId": "4hJXfVG7xW2n3Zl5JH7K8o"
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

The backend uses Spotify's Authorization Code flow on the server side and stores the refresh token in the database so sync can continue after app restarts.

Current Spotify admin flow:

1. Call `POST /admin/spotify/authorization-url` with `X-Admin-Key`
2. Open the returned URL in the browser and authorize the app with your Spotify account
3. Let Spotify redirect back to `/admin/spotify/callback`
4. Call `POST /admin/spotify/playlist-items/sync` with `X-Admin-Key`

This sync uses `GET /playlists/{playlist_id}/items` and builds a local Spotify catalog for application use.

## Current Spotify model

Spotify data is normalized into:

- `spotify_connections`: OAuth tokens and expiry metadata
- `spotify_albums`: synced albums from the canonical playlist
- `spotify_tracks`: synced tracks, linked to albums and artists
- `spotify_artists`: normalized Spotify artists
- `spotify_track_secondary_artists`: secondary-artist join table

Editorial logs stay separate in:

- `album_logs`

The current editorial-to-Spotify link is:

- `album_logs.spotify_album_id` -> `spotify_albums.spotify_album_id`

That link is bound deterministically from the JSON seed using:

- `spotifyAlbumId` in `data/albums.json`

Internally this is stored on `AlbumLog` as `spotifyAlbumSeedId` so the project can distinguish:

- the editorial seed id
- from the actual bound `SpotifyAlbum` relation

## Sync behavior

The Spotify playlist sync is now delta-based:

- fetch the current playlist snapshot from Spotify
- collect albums, artists, and tracks in memory
- compare the fetched snapshot against the current database snapshot
- insert new rows
- update changed rows
- delete removed rows

After sync, a binding step links unresolved `album_logs` to synced `spotify_albums` using the explicit `spotifyAlbumId` from the seed JSON.

This means the project no longer depends on fuzzy album-name matching as the main strategy.

If you want the sync to run automatically every day, set `SPOTIFY_SYNC_ENABLED=true`. By default it is disabled. The default cron expression runs at `01:00` in `America/Argentina/Buenos_Aires`.

If you also want a full sync whenever the app starts, set `SPOTIFY_SYNC_ON_STARTUP=true`.

## Current foundations

- PostgreSQL via Docker Compose
- Flyway migrations for schema changes
- Spring Boot backend with JPA
- Stable editorial JSON seed data in `data/albums.json`
- Spotify OAuth integration with refresh-token support
- Delta-based Spotify playlist sync
- Deterministic album-log binding via `spotifyAlbumId`
- Basic CI pipeline with Maven tests

## Data conventions

`data/albums.json` is the editorial source of truth. Keep it focused on Jazzlogs metadata plus the explicit Spotify album identifier used for binding.

Recommended fields:

- editorial fields: `album`, `artist`, `caption`, `postedAt`, `instagramPermalink`, `style`, `logNumber`, `moods`, `notes`
- binding field: `spotifyAlbumId`

For `spotifyAlbumId`:

- use the raw Spotify album id, not the full URI
- example: `4hJXfVG7xW2n3Zl5JH7K8o`
- this keeps binding deterministic and avoids heuristic matching

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

## Testing

Run the regular test suite with:

```bash
mvn -q test
```

Some integration tests use Testcontainers and are opt-in. To run them:

```bash
mvn -q test -DrunTestcontainers=true
```
