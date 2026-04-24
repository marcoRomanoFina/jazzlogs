package com.marcoromanofinaa.jazzlogs.ai.ask;

import com.marcoromanofinaa.jazzlogs.core.exception.FeatureUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AiAskService {

    public AiAskResponse ask(AiAskRequest request) {
        /*
         * Placeholder intencional.
         * Este milestone llega hasta documentos listos para embeddings, preview, indexado y búsqueda semántica.
         * El diseño de prompt/tools del LLM se agrega después de validar el comportamiento de la recuperación.
         */
        log.warn("AI ask requested before implementation was enabled. question='{}'", request.question());
        throw new FeatureUnavailableException("AI ask is not implemented yet");
    }
}
