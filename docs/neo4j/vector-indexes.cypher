CREATE VECTOR INDEX album_embeddings IF NOT EXISTS
FOR (album:Album)
ON (album.embedding)
OPTIONS {
  indexConfig: {
    `vector.dimensions`: 1536,
    `vector.similarity_function`: 'cosine'
  }
};

CREATE VECTOR INDEX track_embeddings IF NOT EXISTS
FOR (track:Track)
ON (track.embedding)
OPTIONS {
  indexConfig: {
    `vector.dimensions`: 1536,
    `vector.similarity_function`: 'cosine'
  }
};

CREATE VECTOR INDEX artist_embeddings IF NOT EXISTS
FOR (artist:Artist)
ON (artist.embedding)
OPTIONS {
  indexConfig: {
    `vector.dimensions`: 1536,
    `vector.similarity_function`: 'cosine'
  }
};
