package com.marcoromanofinaa.jazzlogs.recommendation.llm;

import com.marcoromanofinaa.jazzlogs.recommendation.AIProvider;

public interface LLMClient {

    LLMResult generate(LLMCommand command);

    <T> StructuredLLMResult<T> generateStructured(StructuredLLMCommand<T> command);

    boolean supports(AIProvider provider);
}
