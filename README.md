# jazzlogs
Jazzlogs is an AI-powered assistant that recommends music based on a curated archive of jazz albums, originally shared through daily posts.

## Local infra

1. Copy `.env.example` to `.env` if you want to customize credentials.
2. Start PostgreSQL with Docker:

```bash
docker compose up -d
```

The container is exposed on `localhost:5433` to avoid conflicts with a local PostgreSQL running on `5432`.

3. Run the application:

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

Set `JAZZLOGS_ADMIN_API_KEY` in your environment before using the admin endpoint.

From Swagger UI you can already test:

- `POST /admin/ingestion/album-logs`
- `DELETE /admin/ingestion/album-logs/{logNumber}`
- `GET /logs`
- `GET /logs/{logNumber}`

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
