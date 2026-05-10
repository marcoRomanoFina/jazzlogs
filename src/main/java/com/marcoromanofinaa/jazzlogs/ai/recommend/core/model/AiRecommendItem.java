package com.marcoromanofinaa.jazzlogs.ai.recommend.core.model;

public record AiRecommendItem(
        AiRecommendItemKind kind,
        String title,
        String subtitle,
        Integer logNumber,
        String spotifyUrl,
        String coverImageUrl,
        String instagramPermalink
) {
}
