package com.marcoromanofinaa.jazzlogs.recommendation.basic.router;

import com.marcoromanofinaa.jazzlogs.recommendation.AIModelDefinition;
import com.marcoromanofinaa.jazzlogs.recommendation.RecommendationFlowType;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.router.client.OpenAIConversationRouterClient;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.router.context.ConversationRouterContextBuilder;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.router.model.ConversationRouterCommand;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.router.model.ConversationRouterResponse;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.router.model.ConversationRouterResult;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.router.prompt.ConversationRouterPromptBuilder;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.router.prompt.ConversationRouterPromptCommand;
import com.marcoromanofinaa.jazzlogs.recommendation.config.OpenAIRecommendationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConversationRouter {

    private final ConversationRouterContextBuilder contextBuilder;
    private final ConversationRouterPromptBuilder promptBuilder;
    private final OpenAIConversationRouterClient routerClient;
    private final OpenAIRecommendationProperties properties;

    public ConversationRouterResult route(ConversationRouterCommand command) {
        var context = contextBuilder.build(command.userId(), command.recommendationMemory(), command.recentHistory());
        var prompt = promptBuilder.build(new ConversationRouterPromptCommand(
                command.userMessage(),
                command.timeZone(),
                context
        ));
        var modelDefinition = routerModelDefinition(command.requestedModelDefinition());
        var result = routerClient.generate(prompt, modelDefinition, command.requestedModel());

        validate(result.response());

        return new ConversationRouterResult(
                result.response(),
                result.usageEntries()
        );
    }

    private AIModelDefinition routerModelDefinition(AIModelDefinition requestedModelDefinition) {
        var providerModelName = properties.routerModel() == null || properties.routerModel().isBlank()
                ? requestedModelDefinition.providerModelName()
                : properties.routerModel().trim();
        var routerMaxCompletionTokens = properties.routerMaxCompletionTokens() == null
                ? properties.maxCompletionTokens()
                : properties.routerMaxCompletionTokens();
        var outputTokenLimit = routerMaxCompletionTokens == null
                ? requestedModelDefinition.outputTokenLimit()
                : Math.min(requestedModelDefinition.outputTokenLimit(), routerMaxCompletionTokens);

        return new AIModelDefinition(
                requestedModelDefinition.type(),
                requestedModelDefinition.provider(),
                providerModelName,
                requestedModelDefinition.enabled(),
                requestedModelDefinition.allowedPlans(),
                requestedModelDefinition.inputTokenLimit(),
                outputTokenLimit,
                RecommendationFlowType.BASIC
        );
    }

    private void validate(ConversationRouterResponse response) {
        if (response == null) {
            throw new IllegalArgumentException("Conversation router returned null response");
        }
        if (response.route() == null) {
            throw new IllegalArgumentException("Conversation router requires route");
        }
        if (response.userIntent() == null) {
            throw new IllegalArgumentException("Conversation router requires userIntent");
        }

        switch (response.route()) {
            case DIRECT_ANSWER -> {
                if (isBlank(response.directAnswer())) {
                    throw new IllegalArgumentException("Conversation router requires directAnswer for DIRECT_ANSWER");
                }
            }
            case CLARIFICATION_NEEDED -> {
                if (isBlank(response.clarificationQuestion())) {
                    throw new IllegalArgumentException(
                            "Conversation router requires clarificationQuestion for CLARIFICATION_NEEDED"
                    );
                }
            }
            case MUSIC_RECOMMENDATION -> {
                if (isBlank(response.contextualizedQuery())) {
                    throw new IllegalArgumentException(
                            "Conversation router requires contextualizedQuery for MUSIC_RECOMMENDATION"
                    );
                }
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
