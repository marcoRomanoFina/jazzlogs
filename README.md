# JazzLogs 🎵🎷

JazzLogs is a Spring Boot backend for a curated jazz knowledge base. It combines editorial music writing with Spotify catalog sync and a semantic indexing layer designed for retrieval-augmented recommendations.

The project is currently optimized around three backend flows:

- curated content management for `AlbumLog`, `TrackNote`, and `ArtistProfile`
- Spotify playlist synchronization into a local normalized catalog
- semantic document generation and indexing for future AI-powered recommendation experiences


## Tech stack

- Java 21
- Spring Boot
- Spring Data JPA
- PostgreSQL
- Flyway
- Spring AI abstractions for semantic/vector integration
- OpenAPI / Swagger UI
- Maven

## Current architecture

### 1. Logbook domain

The core knowledge base is split into three editorial entities:

- `AlbumLog`
- `TrackNote`
- `ArtistProfile`

These live under `src/main/java/com/marcoromanofinaa/jazzlogs/logbook` and represent the source of truth for JazzLogs' curation.

### 2. Curation write flow

Admin-only upsert endpoints allow content to be created or updated directly through HTTP:

- `POST /admin/curation/album-logs`
- `POST /admin/curation/track-notes`
- `POST /admin/curation/artist-profiles`

This is the current write path of the application. The old JSON ingestion flow is no longer the product-facing path.

### 3. Spotify integration

Spotify is currently modeled as a single admin-owned integration for the app, not as per-user Spotify connections.

That decision is intentional for now:

- it keeps the scope focused on the product core
- it avoids premature auth complexity
- it makes the project easier to ship, explain, and present

A future refactor can introduce user login and multiple Spotify connections when JazzLogs needs personalization.

Current Spotify capabilities:

- server-side Authorization Code flow
- refresh-token persistence
- periodic or manual playlist sync
- normalized local catalog:
  - `spotify_connections`
  - `spotify_albums`
  - `spotify_tracks`
  - `spotify_artists`
  - `spotify_track_secondary_artists`

### 4. Semantic indexing

Each curated entity can be transformed into a `SemanticDocument` with deterministic embedding text.

The semantic layer currently supports:

- previewing generated semantic documents
- manual full reindexing
- point reindexing after curation changes
- failure persistence and scheduled recovery

The indexing flow is designed as:

1. curation is written transactionally to PostgreSQL
2. an internal indexing event is published
3. indexing runs `AFTER_COMMIT`
4. failures are persisted
5. a scheduled recovery job retries failed items

This keeps the relational database as the source of truth and treats semantic indexing as a derived projection.

The next product step is to turn this semantic layer into a proper recommendation engine:

- RAG over curated JazzLogs documents
- LLM calls to synthesize answers in an editorial voice
- personalized recommendations shaped by mood, context, and listening intent
- lightweight plan limits for `free`, `plus`, and `pro` style experiences

## API surface

### Public read endpoints

- `GET /logs`
- `GET /logs/{logNumber}`

### Admin curation endpoints

- `POST /admin/curation/album-logs`
- `POST /admin/curation/track-notes`
- `POST /admin/curation/artist-profiles`

### Admin Spotify endpoints

- `POST /admin/spotify/authorization-url`
- `GET /admin/spotify/callback`
- `POST /admin/spotify/playlist-items/sync`

### Admin AI / semantic endpoints

- `GET /admin/ai/semantic-documents/album-logs/{logNumber}/preview`
- `GET /admin/ai/semantic-documents/track-notes/{spotifyTrackId}/preview`
- `GET /admin/ai/semantic-documents/artist-profiles/{spotifyArtistId}/preview`
- `POST /admin/ai/semantic-documents/index`
- `POST /admin/ai/ask` for the current internal recommendation prototype

## Example data

### Example album log payload

```json
{
  "album": "Spunky",
  "artist": "Monty Alexander",
  "caption": "Warm, groovy hard bop with a playful pulse and a late-night club feel.",
  "postedAt": "2026-02-02",
  "instagramPermalink": "https://www.instagram.com/p/DUReEzNEfyX/",
  "style": "Hard Bop / Soul Jazz",
  "releaseYear": "1965",
  "logNumber": 1,
  "moods": ["warm", "groovy", "energetic"],
  "tier": "free",
  "vibe": ["playful", "driving", "earthy"],
  "energy": "medium-high",
  "moodIntensity": "medium",
  "accessibility": "high",
  "bestMoment": "A lively evening walk or a small gathering with good speakers.",
  "listeningContext": ["night", "walking", "small-group"],
  "notes": "Standout track: Rattlesnake.",
  "whyItMatters": "A great entry point into soulful piano-led hard bop.",
  "editorialNote": "Monty Alexander sounds nimble, warm, and always in motion here.",
  "recommendedIf": "You like groove-first jazz that still swings hard.",
  "avoidIf": "You are looking for something austere or very abstract.",
  "albumContext": "Early-career Monty with an immediately inviting sound.",
  "personnel": [],
  "spotifyAlbumId": "4hJXfVG7xW2n3Zl5JH7K8o"
}
```

### Example track note payload

```json
{
  "spotifyTrackId": "spotify-track-id",
  "spotifyAlbumId": "spotify-album-id",
  "logNumber": 1,
  "track": "Rattlesnake",
  "album": "Spunky",
  "artistId": "spotify-artist-id",
  "tier": "free",
  "isInstrumental": true,
  "isStandout": true,
  "vibe": ["nimble", "swinging", "punchy"],
  "energy": "high",
  "moodIntensity": "medium-high",
  "accessibility": "high",
  "tempoFeel": "up-tempo",
  "rhythmicFeel": "swing",
  "trackRole": "standout",
  "compositionType": "original",
  "bestMoment": "When you want something lively but not chaotic.",
  "listeningContext": ["commute", "focus", "night"],
  "whyItHits": "It lands quickly and keeps its momentum without losing warmth.",
  "editorialNote": "A sharp, welcoming track that gets straight to the point.",
  "recommendedIf": "You want hard bop with bounce and clarity.",
  "avoidIf": "You want something slow or atmospheric.",
  "instrumentFocus": "piano",
  "vocalStyle": null,
  "standoutTags": ["groove", "swing", "entry-point"]
}
```

### Example artist profile payload

```json
{
  "spotifyArtistId": "spotify-artist-id",
  "name": "Monty Alexander",
  "primaryInstrument": "Piano",
  "mainStyles": ["Hard Bop", "Soul Jazz", "Caribbean Jazz"],
  "signatureSound": "Rhythmic, warm, extroverted piano playing with strong groove.",
  "artistContext": "A pianist whose sound often bridges bop language and a more open, danceable pulse.",
  "jazzlogsTake": "Best when you want joy, movement, and direct melodic appeal.",
  "recommendedEntryPoint": "Spunky",
  "bestFor": ["groove-first listening", "entry-level jazz", "night listening"],
  "avoidIf": "You prefer cool, restrained, or highly abstract jazz.",
  "relatedArtists": ["Oscar Peterson", "Ahmad Jamal"],
  "importance": "Useful as a bridge artist between virtuosity and accessibility.",
  "logAppearances": [1]
}
```


## Local setup

1. Copy `.env.example` to `.env`

```bash
cp .env.example .env
```

2. Start PostgreSQL

```bash
docker compose up -d
```

The database container is exposed on `127.0.0.1:5433` to avoid conflicts with a local PostgreSQL instance on `5432`.

3. Run the app

```bash
./mvnw spring-boot:run
```

Flyway runs automatically on startup and applies the schema.

## Important environment variables

### Admin API

```bash
JAZZLOGS_ADMIN_API_KEY=...
```

### Spotify

```bash
SPOTIFY_CLIENT_ID=...
SPOTIFY_CLIENT_SECRET=...
SPOTIFY_PLAYLIST_ID=...
SPOTIFY_REDIRECT_URI=http://127.0.0.1:8080/admin/spotify/callback
SPOTIFY_SYNC_ENABLED=false
SPOTIFY_SYNC_CRON=0 0 */12 * * *
SPOTIFY_SYNC_ZONE=America/Argentina/Buenos_Aires
```

If enabled, the default Spotify sync runs every 12 hours.

### OpenAI / semantic features

```bash
OPENAI_API_KEY=...
```

## Swagger and OpenAPI

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI document:

```text
http://localhost:8080/v3/api-docs
```

## Testing

Run the regular test suite with:

```bash
./mvnw -q test
```

For day-to-day verification of the Spotify integration, a manual smoke test is currently more reliable than the dedicated Spotify unit tests in this environment.

Recommended Spotify smoke-test flow:

1. Start the app
2. Call `POST /admin/spotify/authorization-url`
3. Complete the OAuth flow in the browser
4. Call `POST /admin/spotify/playlist-items/sync`
5. Verify that the local Spotify catalog tables were updated

The current Spotify test classes compile, but Mockito inline mocking is not stable in this local setup because Byte Buddy cannot attach its agent cleanly. The product flow itself should be validated through the admin endpoints until that test infrastructure is cleaned up.

## Current product status

What is already solid:

- editorial domain modeling
- admin-first curation flow
- Spotify OAuth and playlist sync
- deterministic binding between curation and Spotify catalog data
- semantic document generation
- transactional-after-commit semantic indexing with persisted recovery

What is still evolving:

- the final end-user recommendation experience
- public authentication and user accounts
- tiered plans and usage limits
- RAG orchestration plus LLM-backed personalized recommendation generation

## Repo structure

Main backend modules:

- `logbook`: source-of-truth editorial domain
- `curation`: admin upsert flow
- `spotify`: OAuth, sync, catalog, and binding
- `ai/semantic`: semantic document generation, preview, indexing, recovery
- `core`: shared exceptions and foundational pieces
