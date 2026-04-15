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

## Current foundations

- PostgreSQL via Docker Compose
- Flyway migrations for schema changes
- Spring Boot backend with JPA
- Stable JSON seed data in `data/albums.json`
- Basic CI pipeline with Maven tests
