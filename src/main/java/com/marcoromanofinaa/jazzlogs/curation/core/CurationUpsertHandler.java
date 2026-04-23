package com.marcoromanofinaa.jazzlogs.curation.core;

public interface CurationUpsertHandler<R> {

    CurationElementType type();

    String identifier(R request);

    CurationUpsertResult upsert(R request);
}
