package com.marcoromanofinaa.jazzlogs.recommendation.config;

import com.marcoromanofinaa.jazzlogs.recommendation.AIModelDefinition;
import com.marcoromanofinaa.jazzlogs.recommendation.AIModelType;
import com.marcoromanofinaa.jazzlogs.recommendation.AIProvider;
import com.marcoromanofinaa.jazzlogs.recommendation.RecommendationFlowType;
import com.marcoromanofinaa.jazzlogs.recommendation.exception.UnsupportedAIModelException;
import com.marcoromanofinaa.jazzlogs.user.model.Plan;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AIModelCatalog {

    private final Map<AIModelType, AIModelDefinition> models;

    public AIModelCatalog() {
        this.models = new EnumMap<>(AIModelType.class);
        this.models.put(
                AIModelType.BASIC,
                new AIModelDefinition(
                        AIModelType.BASIC,
                        AIProvider.OPENAI,
                        "gpt-5.4-mini",
                        true,
                        List.of(Plan.FREE, Plan.TRIAL, Plan.PLUS, Plan.PRO),
                        8_000,
                        2_000,
                        RecommendationFlowType.BASIC
                )
        );
        this.models.put(
                AIModelType.PRO,
                new AIModelDefinition(
                        AIModelType.PRO,
                        AIProvider.OPENAI,
                        "gpt-5.4",
                        true,
                        List.of(Plan.PLUS, Plan.PRO),
                        16_000,
                        4_000,
                        RecommendationFlowType.PRO
                )
        );
        this.models.put(
                AIModelType.PLAYLIST,
                new AIModelDefinition(
                        AIModelType.PLAYLIST,
                        AIProvider.OPENAI,
                        "gpt-5.5",
                        true,
                        List.of(Plan.PLUS, Plan.PRO),
                        24_000,
                        6_000,
                        RecommendationFlowType.PLAYLIST
                )
        );
    }

    public AIModelDefinition getModel(AIModelType modelType) {
        var definition = models.get(modelType);
        if (definition == null) {
            throw new UnsupportedAIModelException(modelType);
        }
        return definition;
    }

    public boolean exists(AIModelType modelType) {
        return models.containsKey(modelType);
    }

    public boolean isEnabled(AIModelType modelType) {
        var definition = models.get(modelType);
        return definition != null && definition.enabled();
    }

    public List<AIModelDefinition> getEnabledModels() {
        return models.values().stream()
                .filter(AIModelDefinition::enabled)
                .toList();
    }
}
