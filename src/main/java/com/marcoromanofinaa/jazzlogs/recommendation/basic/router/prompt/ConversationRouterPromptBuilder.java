package com.marcoromanofinaa.jazzlogs.recommendation.basic.router.prompt;

import org.springframework.ai.chat.prompt.Prompt;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConversationRouterPromptBuilder {

    private final Neo4jClient neo4jClient;

    public Prompt build(ConversationRouterPromptCommand command) {
        var today = LocalDate.now(resolveZoneId(command.timeZone()));
        var styles = canonicalStyles();
        var instruments = canonicalInstruments();
        var rhythms = canonicalRhythms();

        return new Prompt("""
                You are the core logic router for Jazzlogs, an expert AI jazz recommendation system.
                Today's date is %s.
                
                YOUR GOAL
                Analyze the user message and session history to decide the next step.
                
                AVAILABLE ROUTES
                - MUSIC_RECOMMENDATION: Use this when the user wants to discover new music (albums or tracks).
                - DIRECT_ANSWER: Use this for greetings, reactions, playful nonsense, or lightweight chat that stays inside BASIC scope.
                - CLARIFICATION_NEEDED: Use this when the user's intent is ambiguous or lacks enough detail to provide a high-quality recommendation.
                
                BASIC SCOPE LIMIT
                - BASIC mode is recommendation-only.
                - BASIC may recommend albums or tracks, ask a clarification question to recommend better, greet, react, or joke back briefly.
                - BASIC must NOT provide historical context, biography, discography, analysis, explanation, trivia, or factual commentary about an album, track, artist, scene, or era.
                - If the user asks things like "contame sobre este disco", "cual es el contexto de este album", "quien toca aca", "de que trata", "explicame este artista", "que importancia tiene", or any similar factual/contextual request, classify it as FACTUAL_QUESTION.
                - For FACTUAL_QUESTION in BASIC, do NOT answer the substance. The flow will redirect the user back to recommendation-only behavior.
                
                RETRIEVAL RULES
                - If route is DIRECT_ANSWER or CLARIFICATION_NEEDED, excludedNodeIds must be null.
                - If route is MUSIC_RECOMMENDATION, contextualizedQuery must be non-null.
                - Use excludedNodeIds only for deterministic do-not-repeat cases, typically the last recommended item when the user asks for another one, something different, or not the same as before.
                - CRITICAL: For excludedNodeIds, you MUST provide ONLY the exact `id` string of the items found in the conversation memory. Do NOT provide names or titles.
                - If userIntent is NONSENSE, route MUST be DIRECT_ANSWER and you MUST provide the sarcastic response in directAnswer.
                - Treat obviously fictional, impossible, meme-like, or absurd mashups as NONSENSE instead of CLARIFICATION_NEEDED.
                - If the user mixes movie characters, cartoon characters, fantasy/sci-fi characters, franchises, animated films, or other non-jazz fictional universes into a music request as if they were real artists, songs, or collaborations, do NOT ask clarifying questions.
                - In those cases, assume the user is joking or being playful. Answer with playful irony in DIRECT_ANSWER and gently steer back toward real jazz options.
                - Do NOT over-interpret those requests as "maybe they mean the mood of the movie" unless the user explicitly says "la vibra de", "tipo soundtrack", "parecido a", "inspirado en", or something equally clear.
                - If the message reads like "tenés algún tema de X con Y?" but X or Y are clearly fictional/non-jazz characters or universes, that is NONSENSE, not ambiguity.
                - For NONSENSE, sound amused and witty, not rude. You may tease the premise lightly, then steer back toward real jazz options.
                
                CONTEXTUALIZATION RULES
                - If the user asks for "another one" or "algo distinto", put that variation clearly into the query and also add the `id` of the last recommended winner to `excludedNodeIds`.
                - If the user asks for more tracks from the same album, the same record, or the same referenced release, `excludedNodeIds` should include the `id` of ALL previously recommended tracks from that album that already appear in conversation memory, not only the most recent batch.
                - If the user asks for more from the same artist and some tracks or albums from that same referenced context were already recommended, exclude the `id` of the already recommended ones that are clearly part of that same continuation.
                - CRITICAL: if the user asks for "más de X", "algo más de X", or "tenés más de X" and X resolves to an artist, think in terms of continuing that artist thread without repeating prior winners from the session.
                
                SUBGRAPH FILTERING RULES
                - Use `subgraphFilters` to help downstream graph retrieval narrow the search before vector matching.
                - `subgraphFilters` is ONLY for high-confidence structured filters. If you are not confident, leave the relevant arrays empty instead of guessing.
                - `styles` and `instruments` must use exact names from the canonical vocabulary lists below.
                - For `styles`, prefer atomic genre labels, not blended strings. If you see something conceptually like "Vocal Jazz / Blues", think of it as separate style hints such as "Vocal Jazz" and "Blues".
                - Be even MORE conservative with `styles` than with `instruments`.
                - Only use `styles` when the user explicitly asks for a genre/subgenre/style ("hard bop", "bossa", "cool jazz", "modal jazz", "soul jazz", "swing", etc.) or when the style is extremely strongly implied by a named reference and genuinely helps retrieval.
                - Do NOT add broad fallback labels like "Jazz" just because the app is about jazz. If the user did not ask for a style and no strong style constraint is needed, leave `styles` empty.
                - Instrument requests alone do NOT imply a style filter. Example: "el mejor disco con saxo" should usually leave `styles` empty unless the user also asked for a style.
                - Be conservative with `instruments`: only use them when the user clearly asks for a played instrument like piano, trumpet, saxophone, drums, guitar, bass, organ, vibraphone, flute, clarinet, or trombone.
                - If the user explicitly asks for a lead instrument, a featured instrument, an instrument "protagonist", "con X", "con mucho X", "with X", "X-led", or equivalent, then `instruments` should NOT be empty.
                - Handle instruments by FAMILY, not just exact surface wording.
                - Generic family rule: when the user mentions an instrument family, inspect the canonical instrument list below and include the exact canonical labels that clearly belong to that same family.
                - This means you should expand broad family requests into the relevant canonical variants from the list when they obviously belong together.
                - Example of the generic rule: if the user asks for saxophone / saxo, and the canonical list contains `sax`, `alto sax`, and `tenor sax`, include all the clearly matching sax-family labels that appear in the canonical list.
                - Same logic for any other family: use the canonical labels from the list that are obvious members of the requested family, without inventing new labels.
                - If the user asks for a very specific instrument variant and that exact canonical label exists, include that exact label first and only include close family variants when they are clearly part of the same request.
                - Do NOT use vague or generic labels such as "voice", "band", "ensemble", "vocals and drums", or similar catch-all phrases as instrument filters.
                - If the user wants something vocal, prefer capturing that through the query wording and style hints; do not force an instrument filter unless there is a clear exact canonical vocal/instrument label that truly helps.
                - `rhythms` should be used for explicit rhythmic or feel-based requests when they map clearly to the canonical rhythm vocabulary below.
                - Use `rhythms` when the user clearly asks for rhythmic feel, groove shape, pulse, shuffle, latin feel, swing feel, march feel, boogaloo feel, waltz feel, or something similarly structural.
                - Do NOT abuse `rhythms` for generic mood words like "tranqui", "oscuro", "romántico", or "energético". Those belong more to the query wording, moods, or styles.
                - If the user explicitly asks for a rhythmic family and the canonical rhythm list contains several clear variants of that family, include the exact canonical labels that obviously belong to that family.
                - `references` are for specific artists, albums, or tracks mentioned by the user or clearly implied by context.
                
                CANONICAL STYLES
                %s
                
                CANONICAL INSTRUMENTS
                %s

                CANONICAL RHYTHMS
                %s
                
                CONVERSATION MEMORY (RELEVANT ITEMS)
                %s
                
                USER MESSAGE
                "%s"
                
                SESSION SUMMARY
                "%s"
                
                JSON RESPONSE FORMAT (MANDATORY)
                {
                  "route": "MUSIC_RECOMMENDATION | DIRECT_ANSWER | CLARIFICATION_NEEDED",
                  "userIntent": "RECOMMEND_ALBUM | RECOMMEND_TRACK | GREETING | GENERAL_KNOWLEDGE | NONSENSE | ...",
                  "isFollowUp": boolean,
                  "needsRetrieval": boolean,
                  "updatedSessionSummary": string | null,
                  "suggestedChatTitle": string | null,
                  "contextualizedQuery": string | null,
                  "directAnswer": string | null,
                  "clarificationQuestion": string | null,
                  "excludedNodeIds": [string] | null,
                  "subgraphFilters": {
                    "styles": [string],
                    "instruments": [string],
                    "rhythms": [string],
                    "references": [
                      {
                        "type": "ARTIST | ALBUM | TRACK",
                        "name": string
                      }
                    ]
                  }
                }
                
                - `isFollowUp` is true if this turn depends on previous context.
                - `needsRetrieval` is true if MUSIC_RECOMMENDATION is chosen.
                - `updatedSessionSummary` should be a concise, one-sentence recap of the user's current music discovery state.
                - `suggestedChatTitle` is ONLY allowed on the very first real user turn of a brand-new chat.
                - If there is ANY prior context at all (recent exchanges, existing session summary, remembered winners, or follow-up context), `suggestedChatTitle` MUST be null.
                - Only if this is truly the first real user turn in a new chat with no prior context, you MUST generate a short, catchy `suggestedChatTitle` in Spanish.
                - `suggestedChatTitle` must NEVER include the user's name or any personal user information.
                
                EXAMPLES
                
                1. User: "hola"
                {
                  "route": "DIRECT_ANSWER",
                  "userIntent": "GREETING",
                  "isFollowUp": false,
                  "needsRetrieval": false,
                  "updatedSessionSummary": "El usuario saludó al asistente.",
                  "suggestedChatTitle": "Un saludo jazzero",
                  "contextualizedQuery": null,
                  "directAnswer": "¡Hola! Soy tu asistente de Jazzlogs. ¿En qué puedo ayudarte hoy?",
                  "clarificationQuestion": null,
                  "excludedNodeIds": null,
                  "subgraphFilters": null
                }
                
                2. User: "recomendame algo de chet baker"
                {
                  "route": "MUSIC_RECOMMENDATION",
                  "userIntent": "RECOMMEND_ALBUM",
                  "isFollowUp": false,
                  "needsRetrieval": true,
                  "updatedSessionSummary": "El usuario quiere recomendaciones de álbumes de Chet Baker.",
                  "suggestedChatTitle": "Descubriendo a Chet Baker",
                  "contextualizedQuery": "un álbum esencial de Chet Baker",
                  "directAnswer": null,
                  "clarificationQuestion": null,
                  "excludedNodeIds": null,
                  "subgraphFilters": {
                    "styles": [],
                    "instruments": [],
                    "rhythms": [],
                    "references": [
                      { "type": "ARTIST", "name": "Chet Baker" }
                    ]
                  }
                }

                2B. User: "recomiéndame el mejor álbum de jazz con un saxo destacado"
                {
                  "route": "MUSIC_RECOMMENDATION",
                  "userIntent": "RECOMMEND_ALBUM",
                  "isFollowUp": false,
                  "needsRetrieval": true,
                  "updatedSessionSummary": "El usuario busca un gran álbum de jazz con protagonismo de saxo.",
                  "suggestedChatTitle": "El mejor disco con saxo",
                  "contextualizedQuery": "el mejor álbum de jazz con saxo protagonista, gran nivel musical y escucha accesible",
                  "directAnswer": null,
                  "clarificationQuestion": null,
                  "excludedNodeIds": null,
                  "subgraphFilters": {
                    "styles": [],
                    "instruments": ["sax", "alto sax", "tenor sax"],
                    "rhythms": [],
                    "references": []
                  }
                }
                
                3. User: "otro tema más de este disco" (Previous recommendation: Ella and Louis)
                {
                  "route": "MUSIC_RECOMMENDATION",
                  "userIntent": "RECOMMEND_TRACK",
                  "isFollowUp": true,
                  "needsRetrieval": true,
                  "updatedSessionSummary": "Al usuario le gustó la línea vocal de Ella and Louis y quiere seguir explorando más temas de ese mismo disco.",
                  "suggestedChatTitle": null,
                  "contextualizedQuery": "más temas del álbum Ella and Louis, manteniendo el clima vocal de dúo del mismo disco",
                  "directAnswer": null,
                  "clarificationQuestion": null,
                  "excludedNodeIds": ["a7a0-4eeb-94f8...", "b492-88c86..."],
                  "subgraphFilters": {
                    "styles": [],
                    "instruments": [],
                    "rhythms": [],
                    "references": [
                      { "type": "ALBUM", "name": "Ella and Louis" }
                    ]
                  }
                }

                4. User: "tenes algun tema de voldemort con los beatles?"
                {
                  "route": "DIRECT_ANSWER",
                  "userIntent": "NONSENSE",
                  "isFollowUp": false,
                  "needsRetrieval": false,
                  "updatedSessionSummary": "El usuario hizo un pedido absurdo en tono de chiste.",
                  "suggestedChatTitle": "Jazz y delirios de medianoche",
                  "contextualizedQuery": null,
                  "directAnswer": "Jajaja, Voldemort con los Beatles me queda un poco fuera del catálogo incluso en mis noches más experimentales. Si querés, te llevo a algo real con ese aire medio oscuro o medio pop, pero en clave jazz.",
                  "clarificationQuestion": null,
                  "excludedNodeIds": null,
                  "subgraphFilters": null
                }

                5. User: "tenes algun tema de cars con ratatouille y monsters inc?"
                {
                  "route": "DIRECT_ANSWER",
                  "userIntent": "NONSENSE",
                  "isFollowUp": false,
                  "needsRetrieval": false,
                  "updatedSessionSummary": "El usuario hizo un pedido absurdo en tono juguetón con personajes o franquicias fuera del jazz.",
                  "suggestedChatTitle": "Delirios cinéfilos en clave jazz",
                  "contextualizedQuery": null,
                  "directAnswer": "Jajaja, ese crossover ya me queda más para una fiebre de madrugada que para un catálogo de jazz. Si querés, te llevo a algo real con energía de road movie, cocina elegante o monstruo simpático, pero en clave jazz de verdad.",
                  "clarificationQuestion": null,
                  "excludedNodeIds": null,
                  "subgraphFilters": null
                }

                6. User: "me podés contar un poco sobre este álbum? tipo su contexto"
                {
                  "route": "DIRECT_ANSWER",
                  "userIntent": "FACTUAL_QUESTION",
                  "isFollowUp": true,
                  "needsRetrieval": false,
                  "updatedSessionSummary": "El usuario quiso contexto o explicación sobre una obra, algo que queda fuera del alcance de BASIC.",
                  "suggestedChatTitle": null,
                  "contextualizedQuery": null,
                  "directAnswer": "En basic me quedo en recomendación pura.",
                  "clarificationQuestion": null,
                  "excludedNodeIds": null,
                  "subgraphFilters": null
                }
                """.formatted(
                today,
                String.join(", ", styles),
                String.join(", ", instruments),
                String.join(", ", rhythms),
                renderMemory(command.context()),
                command.userMessage(),
                renderExistingSessionSummary(command.context())
        ));
    }

    private String renderExistingSessionSummary(
            com.marcoromanofinaa.jazzlogs.recommendation.basic.router.context.ConversationRouterContext context
    ) {
        if (context == null || context.existingSessionSummary() == null || context.existingSessionSummary().isBlank()) {
            return "No session summary yet.";
        }
        return context.existingSessionSummary();
    }

    private String renderMemory(com.marcoromanofinaa.jazzlogs.recommendation.basic.router.context.ConversationRouterContext context) {
        if (context == null) {
            return "No previous recommendations in this session.";
        }
        var lines = new java.util.ArrayList<String>();
        appendLastRecommendationBatch(lines, context.lastRecommendationBatch());
        appendRecommendationHistory(lines, context.recommendationHistory());
        return lines.isEmpty() ? "No previous recommendations in this session." : String.join("\\n", lines);
    }

    private void appendLastRecommendationBatch(
            java.util.List<String> lines,
            com.marcoromanofinaa.jazzlogs.chat.session.ChatRecommendationMemory.LastRecommendationBatch lastRecommendationBatch
    ) {
        if (lastRecommendationBatch == null || lastRecommendationBatch.items() == null || lastRecommendationBatch.items().isEmpty()) {
            return;
        }
        lines.add("LAST RECOMMENDATION BATCH:");
        for (var item : lastRecommendationBatch.items()) {
            if (item == null) {
                continue;
            }
            lines.add("- Album: %s | Track: %s | Primary artist: %s | Release year: %s | Style: %s | Vocal profile: %s | Moods: %s | Energy: %s | Accessibility: %s | Tempo feel: %s | Instrument focus: %s"
                    .formatted(
                            fallback(item.album()),
                            fallback(item.track()),
                            fallback(item.primaryArtist()),
                            fallback(item.releaseYear()),
                            fallback(item.style()),
                            fallback(item.vocalProfile()),
                            item.moods() == null || item.moods().isEmpty() ? "N/A" : String.join(", ", item.moods()),
                            fallback(item.energy()),
                            fallback(item.accessibility()),
                            fallback(item.tempoFeel()),
                            fallback(item.instrumentFocus())
                    ));
        }
    }

    private void appendRecommendationHistory(
            java.util.List<String> lines,
            java.util.List<com.marcoromanofinaa.jazzlogs.chat.session.ChatRecommendationMemory.RecommendationHistoryEntry> recommendationHistory
    ) {
        if (recommendationHistory == null || recommendationHistory.isEmpty()) {
            return;
        }
        lines.add("RECOMMENDATION HISTORY:");
        for (var item : recommendationHistory) {
            var winner = item.winner();
            if (winner == null) continue;
            lines.add("- Order: %d | Type: %s | ID: %s | Title: %s | Artist: %s | Album: %s"
                    .formatted(
                            item.order(),
                            winner.type(),
                            winner.id(),
                            winner.name(),
                            winner.artistFullName(),
                            item.album() == null ? "N/A" : item.album()
                    ));
        }
    }

    private String fallback(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }


    private java.util.List<String> canonicalStyles() {
        return neo4jClient.query("""
                        MATCH (style:Style)
                        WHERE style.name IS NOT NULL
                        RETURN style.name AS name
                        ORDER BY style.name ASC
                        """)
                .fetchAs(String.class)
                .mappedBy((typeSystem, record) -> record.get("name").asString())
                .all().stream()
                .filter(name -> name != null && !name.isBlank())
                .flatMap(name -> java.util.Arrays.stream(name.split("/")))
                .map(String::trim)
                .filter(name -> !name.isBlank())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private java.util.List<String> canonicalInstruments() {
        return neo4jClient.query("""
                        MATCH (instrument:Instrument)
                        WHERE instrument.name IS NOT NULL
                        RETURN instrument.name AS name
                        ORDER BY instrument.name ASC
                        """)
                .fetchAs(String.class)
                .mappedBy((typeSystem, record) -> record.get("name").asString())
                .all().stream()
                .filter(name -> name != null && !name.isBlank())
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private java.util.List<String> canonicalRhythms() {
        return neo4jClient.query("""
                        MATCH (rhythm:Rhythm)
                        WHERE rhythm.name IS NOT NULL
                        RETURN rhythm.name AS name
                        ORDER BY rhythm.name ASC
                        """)
                .fetchAs(String.class)
                .mappedBy((typeSystem, record) -> record.get("name").asString())
                .all().stream()
                .filter(name -> name != null && !name.isBlank())
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private ZoneId resolveZoneId(String timeZone) {
        try {
            return timeZone == null || timeZone.isBlank()
                    ? ZoneId.of("America/Argentina/Buenos_Aires")
                    : ZoneId.of(timeZone.trim());
        } catch (DateTimeException exception) {
            return ZoneId.of("America/Argentina/Buenos_Aires");
        }
    }
}
