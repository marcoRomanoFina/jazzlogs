package com.marcoromanofinaa.jazzlogs.ai.semantic.indexing;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "jazzlogs.ai.semantic.indexing.retry")
public class SemanticIndexingRetryProperties {

    private int recoveryBatchSize = 25;
}
