package com.marcoromanofinaa.jazzlogs.recommendation.llm;

import com.marcoromanofinaa.jazzlogs.recommendation.AIModelDefinition;
import org.springframework.ai.chat.prompt.Prompt;

public record StructuredLLMCommand<T>(
        Prompt prompt,
        AIModelDefinition modelDefinition,
        Class<T> responseType
) {
}
