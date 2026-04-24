package com.marcoromanofinaa.jazzlogs.ai.ask;

import java.util.List;

public record AiAskResponse(
        String question,
        String answer,
        List<String> sources
) {
}
