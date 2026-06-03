package com.marcoromanofinaa.jazzlogs.recommendation.basic;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcoromanofinaa.jazzlogs.chat.exchange.ChatExchange;
import com.marcoromanofinaa.jazzlogs.recommendation.AIModelType;
import com.marcoromanofinaa.jazzlogs.recommendation.preferences.UserPreferencesContext;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BasicPromptBuilderTest {

    private final BasicPromptBuilder promptBuilder = new BasicPromptBuilder();

    @Test
    void buildHighlightsLatestExchangeAndContinuityRules() {
        var prompt = promptBuilder.build(new BasicPromptCommand(
                "mas oscuro",
                BasicRecommendationTarget.ALBUM,
                List.of(
                        exchange("quiero algo nocturno", "Anda con este disco."),
                        exchange("me gusto", "Buenisimo, seguimos por ahi.")
                ),
                "Le viene gustando una linea nocturna y ahora busca algo mas oscuro.",
                ZonedDateTime.of(2026, 6, 3, 0, 35, 0, 0, ZoneId.of("America/Argentina/Buenos_Aires")),
                new UserPreferencesContext(null, List.of(), List.of()),
                List.of()
        ));

        assertThat(prompt.getContents())
                .contains("Rule 1 (The Hook): Your VERY FIRST sentence MUST be a high-energy, positive reaction")
                .contains("Current local time for the user:\n12:35 AM")
                .contains("Latest exchange:\nUser: me gusto\nAssistant: Buenisimo, seguimos por ahi.")
                .contains("Earlier exchange:\nUser: quiero algo nocturno\nAssistant: Anda con este disco.")
                .contains("Session summary:\nLe viene gustando una linea nocturna y ahora busca algo mas oscuro.");
    }

    private ChatExchange exchange(String userMessage, String assistantResponse) {
        return ChatExchange.create(
                UUID.randomUUID(),
                userMessage,
                AIModelType.BASIC,
                assistantResponse,
                List.of(),
                null,
                AIModelType.BASIC,
                0L,
                0L,
                0L,
                Instant.now()
        );
    }
}
