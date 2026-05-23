package com.marcoromanofinaa.jazzlogs.core.outbox;

import com.marcoromanofinaa.jazzlogs.core.outbox.exception.OutboxEventHandlerNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxEventHandlerResolver {

    private final List<OutboxEventHandler> handlers;

    public OutboxEventHandler resolve(OutboxEventType type) {
        return handlers.stream()
                .filter(handler -> handler.supports(type))
                .findFirst()
                .orElseThrow(() -> new OutboxEventHandlerNotFoundException(type));
    }
}
