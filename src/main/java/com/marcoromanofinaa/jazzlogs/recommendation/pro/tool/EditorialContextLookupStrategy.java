package com.marcoromanofinaa.jazzlogs.recommendation.pro.tool;

import com.marcoromanofinaa.jazzlogs.recommendation.pro.agent.JazzAgentContext;

public interface EditorialContextLookupStrategy {

    String lookupMode();

    default boolean supports(String lookupMode) {
        return lookupMode() != null && lookupMode().equals(lookupMode);
    }

    JazzToolExecutionResult execute(String query, JazzAgentContext context);
}
