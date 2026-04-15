# jazzlogs
Jazzlogs is an AI-powered assistant that recommends music based on a curated archive of jazz albums, originally shared through daily posts.

## Local infra

1. Copy `.env.example` to `.env` if you want to customize credentials.
2. Start PostgreSQL with Docker:

```bash
docker compose up -d
```

3. Run the application:

```bash
./mvnw spring-boot:run
```

Flyway runs automatically on startup and creates the initial `album_logs` table.

To run the JSON seed ingestion on startup:

```bash
./mvnw spring-boot:run -Djazzlogs.ingestion.json.enabled=true
```

The default seed path is `data/albums.json`.

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
