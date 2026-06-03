package com.marcoromanofinaa.jazzlogs.recommendation.llm;

import com.marcoromanofinaa.jazzlogs.recommendation.AIProvider;
import com.marcoromanofinaa.jazzlogs.recommendation.exception.LLMClientNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LLMClientResolver {

    private final List<LLMClient> clients;

    public LLMClient resolve(AIProvider provider) {
        return clients.stream()
                .filter(client -> client.supports(provider))
                .findFirst()
                .orElseThrow(() -> new LLMClientNotFoundException(provider));
    }
}
