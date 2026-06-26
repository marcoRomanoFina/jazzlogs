package com.marcoromanofinaa.jazzlogs.recommendation.pro.tool;

import com.marcoromanofinaa.jazzlogs.recommendation.pro.agent.JazzAgentContext;

public interface CatalogContextLookupStrategy {

    String lookupMode();

    default boolean supports(String candidateLookupMode) {
        return candidateLookupMode != null && candidateLookupMode.equals(lookupMode());
    }

    JazzToolExecutionResult execute(String query, JazzAgentContext context);
}
