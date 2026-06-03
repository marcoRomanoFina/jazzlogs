package com.marcoromanofinaa.jazzlogs.recommendation.exception;

import com.marcoromanofinaa.jazzlogs.recommendation.RecommendationFlowType;
import com.marcoromanofinaa.jazzlogs.core.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class RecommendationFlowNotFoundException extends ApplicationException {

    public RecommendationFlowNotFoundException(RecommendationFlowType flowType) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "No recommendation flow found for type: " + flowType);
    }
}
