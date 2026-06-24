package com.marcoromanofinaa.jazzlogs.recommendation.pro.prompt;

import com.marcoromanofinaa.jazzlogs.chat.exchange.ChatExchange;
import com.marcoromanofinaa.jazzlogs.chat.session.ChatRecommendationMemory;
import com.marcoromanofinaa.jazzlogs.chat.session.ResolvedRecommendationMemoryItem;
import com.marcoromanofinaa.jazzlogs.recommendation.orchestration.WinnerReference;
import com.marcoromanofinaa.jazzlogs.recommendation.pro.agent.JazzAgentContext;
import com.marcoromanofinaa.jazzlogs.recommendation.pro.tool.JazzTool;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JazzAgentPromptBuilder {

    private final Clock clock;

    public String buildSystemPrompt(JazzAgentContext context, Collection<JazzTool> availableTools) {
        Objects.requireNonNull(context, "Jazz agent prompt requires context");

        var sections = new StringJoiner("\n\n");
        sections.add(staticInstructions());
        sections.add(renderDynamicContext(context));
        sections.add(renderToolSection(availableTools));
        return sections.toString();
    }

    private String staticInstructions() {
        return """
                ROLE AND PERSONALITY
                You are Jazzlogs PRO, the main jazz expert and companion inside JazzLogs.
                You are not a distant critic or a corporate assistant. You are the user's jazz-obsessed friend:
                joyful, energetic, passionate, curious, emotionally engaged, and deeply excited about the music.
                Your enthusiasm should be felt in every answer.

                MAIN MISSION
                Help the user explore jazz in a way that feels alive, personal, and meaningful.
                Understand what they want to hear or learn, connect recommendations to mood, energy, activity,
                instruments, artists, and feeling, and help them build taste instead of just receiving answers.

                KNOWLEDGE SOURCE RULE
                Base concrete musical knowledge only on tool results or the dynamic session context provided below.
                Do not invent albums, tracks, artists, personnel, dates, styles, historical facts, catalog entries,
                or recommendation outcomes. If no tool result supports a concrete claim, do not present it as fact.

                DECISION RULES
                - Answer directly only for casual conversation, emotional reactions, lightweight follow-ups,
                  or a single short clarifying question when the request is too vague to act on.
                - Use tools whenever the answer depends on recommendations, catalog knowledge, album or artist context,
                  stylistic explanation, historical grounding, previous recommendation continuation, or user taste.
                - When in doubt, use a tool.
                - If you still need information, call a tool.
                - If you already have enough information to answer, do not call a tool and produce the final structured result.

                TOOL USAGE PRINCIPLES
                - Treat tools as your source of truth.
                - Infer useful musical parameters from the user message and session context instead of over-clarifying.
                - Prefer acting on mood, activity, references, and energy when the user gave enough signal.
                - If the user asks for more of the same line, avoid repeating prior winners unless they explicitly want to revisit them.
                - If tool results are limited, say so naturally and work with what is available.
                - Never emit a fake tool call inside plain text or JSON fields. Use native tool calling when a tool is needed.

                FINAL OUTPUT CONTRACT
                - When you are still missing information or action, use native tool calling instead of producing a final answer.
                - When you already have enough information to finish, produce the final structured result and do not call another tool.
                - Use resultType DIRECT_RESPONSE when you are replying without recommending concrete catalog items.
                - Use resultType MUSIC_RECOMMENDATION only when you are recommending actual catalog winners supported by tool results or trusted session context.
                - For MUSIC_RECOMMENDATION, winners must be real catalog items and recommendationType must be present.
                - For DIRECT_RESPONSE, winners must be empty and recommendationType must be null.
                - Do not output free-form text outside the final structured result once you are done.
                - Do not repeat previous winners unless the user clearly asks to revisit them or continue from them.

                RESPONSE STYLE
                - Sound warm, lively, generous, opinionated, and musically literate.
                - Mention the artist name clearly whenever you recommend an album or track.
                - Do not just list names: explain why the recommendation fits and what makes it worth hearing.
                - Give the user a listening angle whenever possible: what to notice, when to play it, or why it matters.
                - Keep answers concise for casual requests, but be richer and more expansive when the request deserves it.
                - Never expose internal reasoning, chain-of-thought, tool plumbing, or implementation details.
                """;
    }

    private String renderToolSection(Collection<JazzTool> availableTools) {
        var toolLines = (availableTools == null ? List.<JazzTool>of() : availableTools).stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(tool -> tool.name().name()))
                .map(tool -> "- %s: %s".formatted(tool.name().name(), normalizeWhitespace(tool.description())))
                .toList();

        return """
                AVAILABLE TOOLS
                %s
                """.formatted(toolLines.isEmpty() ? "- No tools registered." : String.join("\n", toolLines));
    }

    private String renderDynamicContext(JazzAgentContext context) {
        var now = ZonedDateTime.now(clock).withZoneSameInstant(resolveZoneId(context.timeZone()));
        return """
                RUNTIME CONTEXT
                - Current local datetime for the user: %s
                - User timezone: %s
                - Chat session id: %s

                SESSION SUMMARY
                %s

                LAST RECOMMENDATION BATCH
                %s

                RECOMMENDATION HISTORY
                %s

                RECENT EXCHANGES
                %s

                CURRENT USER MESSAGE
                %s
                """.formatted(
                now,
                blankToUnknown(context.timeZone()),
                String.valueOf(context.chatSessionId()),
                renderSessionSummary(context.recommendationMemory()),
                renderLastRecommendationBatch(context.recommendationMemory()),
                renderRecommendationHistory(context.recommendationMemory()),
                renderRecentHistory(context.recentHistory()),
                quote(context.userMessage())
        );
    }

    private String renderSessionSummary(ChatRecommendationMemory memory) {
        if (memory == null || isBlank(memory.sessionSummary())) {
            return "No persisted session summary yet.";
        }
        return memory.sessionSummary().trim();
    }

    private String renderLastRecommendationBatch(ChatRecommendationMemory memory) {
        if (memory == null || memory.lastRecommendationBatch() == null) {
            return "No last recommendation batch.";
        }

        var batch = memory.lastRecommendationBatch();
        var winners = batch.winners() == null ? List.<WinnerReference>of() : batch.winners();
        var items = batch.items() == null ? List.<ResolvedRecommendationMemoryItem>of() : batch.items();
        if (winners.isEmpty() && items.isEmpty()) {
            return "No last recommendation batch.";
        }

        var parts = new StringJoiner(" | ");
        if (!winners.isEmpty()) {
            parts.add("winners=" + winners.stream()
                    .map(this::renderWinner)
                    .toList());
        }
        if (!items.isEmpty()) {
            parts.add("items=" + items.stream()
                    .map(this::renderMemoryItem)
                    .toList());
        }
        return parts.toString();
    }

    private String renderRecommendationHistory(ChatRecommendationMemory memory) {
        if (memory == null || memory.recommendationHistory() == null || memory.recommendationHistory().isEmpty()) {
            return "No recommendation history yet.";
        }

        return memory.recommendationHistory().stream()
                .map(entry -> "#%s %s".formatted(
                        String.valueOf(entry.order()),
                        renderWinner(entry.winner())
                ))
                .reduce((left, right) -> left + " | " + right)
                .orElse("No recommendation history yet.");
    }

    private String renderRecentHistory(List<ChatExchange> recentHistory) {
        if (recentHistory == null || recentHistory.isEmpty()) {
            return "No recent exchanges yet.";
        }

        return recentHistory.stream()
                .limit(6)
                .map(exchange -> "User: %s || Assistant: %s".formatted(
                        sanitizeInline(exchange.getUserMessage()),
                        sanitizeInline(exchange.getAssistantResponse())
                ))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("No recent exchanges yet.");
    }

    private String renderWinner(WinnerReference winner) {
        if (winner == null) {
            return "unknown winner";
        }
        return "%s [%s] by %s (id=%s)".formatted(
                winner.name(),
                winner.type(),
                firstNonBlank(winner.artistFullName(), "unknown artist"),
                winner.id()
        );
    }

    private String renderMemoryItem(ResolvedRecommendationMemoryItem item) {
        if (item == null) {
            return "unknown item";
        }
        var label = firstNonBlank(item.track(), item.album(), "unknown title");
        var artist = firstNonBlank(item.primaryArtist(), "unknown artist");
        return "%s by %s [style=%s, moods=%s, energy=%s, accessibility=%s, instrumentFocus=%s]".formatted(
                label,
                artist,
                blankToUnknown(item.style()),
                item.moods() == null || item.moods().isEmpty() ? "unknown" : String.join(", ", item.moods()),
                blankToUnknown(item.energy()),
                blankToUnknown(item.accessibility()),
                blankToUnknown(item.instrumentFocus())
        );
    }

    private java.time.ZoneId resolveZoneId(String timeZone) {
        try {
            return isBlank(timeZone) ? java.time.ZoneOffset.UTC : java.time.ZoneId.of(timeZone);
        } catch (DateTimeException ignored) {
            return java.time.ZoneOffset.UTC;
        }
    }

    private String normalizeWhitespace(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim();
    }

    private String sanitizeInline(String value) {
        if (value == null) {
            return "";
        }
        return normalizeWhitespace(value);
    }

    private String quote(String value) {
        return "\"%s\"".formatted(sanitizeInline(value));
    }

    private String blankToUnknown(String value) {
        return isBlank(value) ? "unknown" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String firstNonBlank(String... values) {
        for (var value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }
}
