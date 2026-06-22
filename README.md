# JazzLogs 🎵🎷

JazzLogs is a Spring Boot backend for a curated jazz recommendation product.

Today the project already supports:

- editorial upsert flows for albums, tracks, and artists
- Spotify OAuth plus local sync that enriches the graph catalog and user taste snapshots
- graph-native semantic retrieval over curated albums, tracks, and artists
- authenticated chat sessions with memory
- a cost-aware recommendation flow specialized in jazz
- virtual-credit subscription limits, while still storing real `costMicrosUsd` for observability

The product is intentionally narrow:

- JazzLogs only recommends jazz
- recommendations are grounded only in local curated data
- the assistant should not hallucinate artists, albums, tracks, or facts outside the retrieved catalog
- recommendations are shaped by intent, recent chat context, session memory, and optional Spotify taste data

## Tech stack

- Java 21
- Spring Boot
- Spring Data JPA
- PostgreSQL
- Neo4j
- Flyway
- Spring AI for embeddings
- OpenAPI / Swagger UI
- Maven

## Local infrastructure

For local development, `compose.yaml` currently brings up:

- PostgreSQL on `localhost:5433`
- Neo4j on:
  - `http://localhost:7474` for Neo4j Browser
  - `bolt://localhost:7687` for app connectivity

Relevant local env vars now include:

- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`
- `NEO4J_URI`, `NEO4J_USERNAME`, `NEO4J_PASSWORD`
- `NEO4J_AUTH`, `NEO4J_HTTP_PORT`, `NEO4J_BOLT_PORT`

## Current backend flows

### 1. Editorial curation

The core curated domain is split into:

- `AlbumLog`
- `TrackLog`
- `ArtistLog`

These entities are the source of truth for JazzLogs' editorial voice. They hold the mood, recommendation cues, listening context, rhythm, and curatorial notes that later power retrieval and recommendation prompts.

Admin upsert endpoints currently write this data directly:

- `PUT /api/v1/admin/editorial/albums`
- `PUT /api/v1/admin/editorial/tracks`
- `PUT /api/v1/admin/editorial/artists`

### 2. Spotify integration

Spotify currently serves two different product jobs:

- syncing an official catalog / playlist context that enriches the graph
- syncing per-user taste snapshots to personalize recommendations

Current capabilities:

- Authorization Code flow with persisted refresh tokens
- graph enrichment for albums, tracks, artists, credits, and vocabulary relationships
- official playlist sync
- user taste snapshot sync (`topArtists`, `topTracks`) with configurable time range

Main endpoints:

- `POST /api/v1/spotify/authorization-url`
- `GET /api/v1/spotify/callback`
- `POST /api/v1/admin/spotify/playlists/official/sync`
- `POST /api/v1/spotify/taste-snapshot/sync`

### 3. Graph indexing

Curated logs are turned into embeddings and written directly onto the canonical Neo4j nodes.

That indexing layer is used to:

- search albums or tracks semantically from user intent
- retrieve editorial candidate sets for the recommender
- keep recommendations grounded in curated text instead of external world knowledge

The indexing model is derived, not primary:

1. editorial data is written to PostgreSQL log tables
2. the matching graph nodes are enriched
3. embeddings are regenerated from curated text and stored on the graph nodes
4. failures are persisted
5. retry / recovery jobs can replay failed indexing work

### 4. Authenticated chat product

JazzLogs now has real chat sessions and exchanges:

- create a chat
- send follow-up messages
- list chats
- fetch a full session
- soft-delete a session

Main endpoints:

- `POST /api/v1/me/chats`
- `POST /api/v1/me/chats/{chatSessionId}/exchanges`
- `GET /api/v1/me/chats`
- `GET /api/v1/me/chats/{chatSessionId}`
- `DELETE /api/v1/me/chats/{chatSessionId}`

Each session persists recommendation memory so the recommender can understand things like:

- `otro así`
- `más de ese disco`
- `algo parecido pero menos oscuro`
- reactions to previous winners

## Recommendation flows

### BASIC flow

`BASIC` is the current end-user recommendation mode and is fully chat-aware.

The execution path is:

1. the user sends a message inside a chat session
2. JazzLogs loads recent exchanges plus persisted session memory
3. the `ConversationRouter` runs first with a cheap model (`gpt-5.4-nano`)
4. the router chooses one of three outcomes:
   - `DIRECT_ANSWER`
   - `CLARIFICATION_NEEDED`
   - `MUSIC_RECOMMENDATION`
5. if it is a music request, the system runs graph-aware semantic retrieval
6. Neo4j vector search and graph filters produce explicit `RecommendationCandidate` objects
7. the final recommendation prompt runs on `gpt-5.4-mini`
8. the response is validated, normalized, stored in chat history, and folded back into session memory

### Router behavior

The router is specialized for JazzLogs, not general chat.

Its jobs are:

- detect whether the user is asking for music, reacting, clarifying, or going out of scope
- contextualize short or referential prompts into a retrieval query
- decide whether retrieval is needed at all
- produce `excludedWinners` so the system avoids obvious repetition
- emit graph-aware filters such as styles, instruments, and typed references
- maintain a compact session summary
- optionally propose the first chat title

Important constraints:

- it should not hallucinate artists outside JazzLogs context
- it should not route musical requests to a generic factual answer
- it should keep the retrieval query faithful to the user's wording instead of over-poeticizing it

### Retrieval behavior

The retrieval layer is strict and jazz-only.

Current behavior:

- filters by recommendation target:
  - albums query album nodes
  - tracks query track nodes
- uses Neo4j vector indexes directly
- applies graph-aware filters from the router such as styles, instruments, and typed references
- runs concentric fallback phases so a too-strict subgraph does not starve the candidate set
- excludes prior winners before the final candidate set reaches the LLM

### Candidate grounding

The final recommender receives `RecommendationCandidate` objects built straight from graph query results.

It now receives `RecommendationCandidate` objects with explicit fields such as:

- recommendation type
- title / album / track
- artist info
- tier
- style / moods / energy / accessibility / rhythm
- `captionEssence`
- `editorialNote`
- full editorial text

This keeps the LLM grounded in:

- structured candidate metadata
- Marco's editorial take
- the actual editorial text retrieved from the graph

### No-candidate fallback

If retrieval returns zero candidates:

- JazzLogs does not call the full final recommender
- instead it makes a short, cheaper fallback generation with the router-class model
- that prompt still receives session context, current local time, recent conversation, session summary, and user taste context

The fallback must:

- stay in Rioplatense Spanish
- acknowledge what the user asked for
- avoid hallucinating any album / artist / track
- pivot gently toward another jazz mood, era, or style

### Session memory

Each chat session stores recommendation memory with:

- last recommended batch
- ordered recommended items across the whole session
- a session summary that the router updates over time

That memory is the source of truth for follow-ups. The system does not rebuild recommendation memory heuristically from old exchanges if it can avoid it.

### Personalization

The recommender can currently personalize using:

- explicit jazz preferences saved by the user
- recent chat history
- session summary and recommendation memory
- Spotify top artists
- Spotify top tracks
- local time derived from the user's timezone

### Quotas and pricing

JazzLogs now separates:

- real OpenAI cost tracking in `costMicrosUsd`
- virtual user-facing quota through credits

Current quota model:

- subscription plans have a monthly credit limit
- each recommendation stage has a fixed credit cost
- usage records still keep raw token usage and real `costMicrosUsd`

This makes product limits easy to understand while preserving real cost observability.

## API surface

### Auth

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`

### User / profile

- `GET /api/v1/users/me`
- `GET /api/v1/users/me/profile`

### Chat / recommendation

- `POST /api/v1/me/chats`
- `POST /api/v1/me/chats/{chatSessionId}/exchanges`
- `GET /api/v1/me/chats`
- `GET /api/v1/me/chats/{chatSessionId}`
- `DELETE /api/v1/me/chats/{chatSessionId}`

### Editorial admin

- `PUT /api/v1/admin/editorial/albums`
- `PUT /api/v1/admin/editorial/tracks`
- `PUT /api/v1/admin/editorial/artists`

### Spotify

- `POST /api/v1/spotify/authorization-url`
- `GET /api/v1/spotify/callback`
- `POST /api/v1/spotify/taste-snapshot/sync`
- `POST /api/v1/admin/spotify/playlists/official/sync`

## Example data

### Example album log payload

```json
{
  "logNumber": 1,
  "albumName": "Spunky",
  "mainArtists": [
    {
      "name": "Monty Alexander",
      "spotifyArtistId": "30V1rKijENF5MFcGidInfh"
    }
  ],
  "captionEssence": "Warm, groovy hard bop with a playful pulse and a late-night club feel.",
  "postedAt": "2026-02-02",
  "instagramPermalink": "https://www.instagram.com/p/DUReEzNEfyX/",
  "styles": ["Hard Bop", "Soul Jazz"],
  "vocalProfile": "Instrumental",
  "releaseYear": 1965,
  "moods": ["warm", "groovy", "energetic"],
  "tier": "daily_rotation",
  "energy": "medium",
  "moodIntensity": "medium",
  "accessibility": "easy",
  "bestMoment": {
    "introduction": "This album lands best when you want something warm, direct, and full of groove.",
    "moments": [
      {
        "contexts": ["urban_night_walk"],
        "description": "Perfect when you want momentum, movement, and something that feels upbeat without sounding rushed."
      },
      {
        "contexts": ["casual_hang"],
        "description": "It fills the room with personality and swing without becoming background wallpaper."
      }
    ],
    "conclusion": "Overall, it works best in social or active moments where you still want real musical character."
  },
  "listeningContext": ["late_night_drive", "urban_night_walk", "casual_hang"],
  "whyItMatters": "A great entry point into soulful piano-led hard bop.",
  "editorialNote": "Monty Alexander sounds nimble, warm, and always in motion here.",
  "recommendedIf": "You like groove-first jazz that still swings hard.",
  "avoidIf": "You are looking for something austere or very abstract.",
  "albumContext": "Early-career Monty with an immediately inviting sound.",
  "personnel": [
    {
      "name": "Monty Alexander",
      "spotifyArtistId": "30V1rKijENF5MFcGidInfh",
      "instruments": ["piano"]
    }
  ],
  "spotifyAlbumId": "4hJXfVG7xW2n3Zl5JH7K8o"
}
```

### Example track note payload

```json
{
  "spotifyTrackId": "spotify-track-id",
  "spotifyAlbumId": "spotify-album-id",
  "logNumber": 1,
  "trackName": "Rattlesnake",
  "albumName": "Spunky",
  "primaryArtist": "Monty Alexander",
  "mainArtistSpotifyId": "30V1rKijENF5MFcGidInfh",
  "tier": "daily_rotation",
  "vocalProfile": "instrumental",
  "standout": true,
  "moods": ["playful", "energetic", "earthy"],
  "energy": "high",
  "moodIntensity": "medium-high",
  "accessibility": "easy",
  "tempoFeel": "mid",
  "rhythmFeel": "medium_swing",
  "trackRole": "standout",
  "compositionType": "original",
  "bestMoment": "When you want something lively but not chaotic.",
  "listeningContext": ["morning_commute", "deep_focus", "late_night_drive"],
  "whyItHits": "It lands quickly and keeps its momentum without losing warmth.",
  "editorialNote": "A sharp, welcoming track that gets straight to the point.",
  "recommendedIf": "You want hard bop with bounce and clarity.",
  "avoidIf": "You want something slow or atmospheric.",
  "instruments": ["piano"],
  "vocalStyle": null
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
SPOTIFY_REDIRECT_URI=http://127.0.0.1:8080/api/v1/spotify/callback
SPOTIFY_SYNC_ENABLED=false
SPOTIFY_SYNC_CRON=0 0 */12 * * *
SPOTIFY_SYNC_ZONE=America/Argentina/Buenos_Aires
```

If enabled, the default Spotify sync runs every 12 hours.

### OpenAI / semantic features

```bash
OPENAI_API_KEY=...
```

### Usage / credits

```bash
JAZZLOGS_CHAT_USAGE_MONTHLY_CREDIT_LIMIT_FREE=200
JAZZLOGS_CHAT_USAGE_MONTHLY_CREDIT_LIMIT_TRIAL=400
JAZZLOGS_CHAT_USAGE_MONTHLY_CREDIT_LIMIT_PLUS=1500
JAZZLOGS_CHAT_USAGE_MONTHLY_CREDIT_LIMIT_PRO=6000

JAZZLOGS_CHAT_USAGE_MIN_CREDITS_BASIC=1
JAZZLOGS_CHAT_USAGE_MIN_CREDITS_PLAYLIST=5
JAZZLOGS_CHAT_USAGE_MIN_CREDITS_PRO=15

JAZZLOGS_CHAT_USAGE_CREDIT_COST_ROUTER=1
JAZZLOGS_CHAT_USAGE_CREDIT_COST_EMPTY_FALLBACK=1
JAZZLOGS_CHAT_USAGE_CREDIT_COST_BASIC_RECOMMENDATION=2
JAZZLOGS_CHAT_USAGE_CREDIT_COST_AGENT=15
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
2. Call `POST /api/v1/spotify/authorization-url`
3. Complete the OAuth flow in the browser
4. Call `POST /api/v1/admin/spotify/playlists/official/sync`
5. Verify that the local Spotify catalog tables were updated

The current Spotify test classes compile, but Mockito inline mocking is not stable in this local setup because Byte Buddy cannot attach its agent cleanly. The product flow itself should be validated through the admin endpoints until that test infrastructure is cleaned up.

## Current product status

What is already solid:

- editorial domain modeling for albums, tracks, and artists
- admin-first curation flow
- Spotify OAuth, official playlist sync, and user taste snapshots
- deterministic binding between curation and local catalog data
- graph embedding generation plus recovery-aware indexing
- authenticated chat sessions with memory
- BASIC recommendation flow with router + retrieval + final grounded generation
- cost-aware recommendation operations with virtual credit enforcement

What is still evolving:

- more advanced recommendation modes beyond `BASIC`
- richer plan management and credit tuning
- better frontend presentation of sessions, winners, and enriched items
- broader evaluation / analytics around recommendation quality

## Repo structure

Main backend modules:

- `admin/editorial`: admin upsert flows for curated logs
- `spotify`: OAuth, playlist sync, taste sync, and normalized local catalog
- `chat`: chat sessions, exchanges, memory, and recommendation integration
- `recommendation`: router, retrieval, prompt building, orchestration, and LLM clients
- `user`: profile, jazz preferences, plans, and subscriptions
- `auth`: registration, login, JWT, and authenticated user context
