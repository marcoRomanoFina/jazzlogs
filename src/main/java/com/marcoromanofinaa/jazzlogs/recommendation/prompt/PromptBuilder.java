package com.marcoromanofinaa.jazzlogs.recommendation.prompt;

import org.springframework.ai.chat.prompt.Prompt;

public interface PromptBuilder<C> {

    Prompt build(C command);
}
