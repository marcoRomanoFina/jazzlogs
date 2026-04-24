# JazzLogs Sample Data

Estos archivos son fixtures públicos y mínimos para mostrar el formato sin exponer la curaduría real.

- `albums.sample.json`: album logs con campos semánticos completos.
- `track-notes.sample.json`: notas curadas track por track.
- `artists.sample.json`: perfiles curatoriales de artistas.

Para correr la app usando estos samples:

```bash
JAZZLOGS_INGESTION_JSON_PATH=data/sample/albums.sample.json
JAZZLOGS_INGESTION_JSON_TRACK_NOTES_PATH=data/sample/track-notes.sample.json
JAZZLOGS_INGESTION_JSON_ARTIST_PROFILES_PATH=data/sample/artists.sample.json
```

Si mantenés tu dataset real privado, podés commitear esta carpeta y agregar los JSON reales a `.gitignore`.
