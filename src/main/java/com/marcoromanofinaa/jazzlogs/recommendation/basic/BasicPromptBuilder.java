package com.marcoromanofinaa.jazzlogs.recommendation.basic;

import com.marcoromanofinaa.jazzlogs.chat.exchange.ChatExchange;
import com.marcoromanofinaa.jazzlogs.recommendation.preferences.UserPreferencesContext;
import java.time.format.DateTimeFormatter;
import com.marcoromanofinaa.jazzlogs.recommendation.prompt.PromptBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

@Component
public class BasicPromptBuilder implements PromptBuilder<BasicPromptCommand> {

    private static final int BASIC_PROMPT_HISTORY_LIMIT = 5;
    private static final int BASIC_PROMPT_WINNERS_LIMIT = 5;
    private static final DateTimeFormatter LOCAL_TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a", Locale.US);

    @Override
    public Prompt build(BasicPromptCommand command) {
        var sections = new ArrayList<String>();
        sections.add(buildSystemInstructions(command.target()));
        sections.add(buildOutputInstructions(command.target()));
        sections.add(buildFewShotExamples());
        appendIfPresent(
                sections,
                command.currentLocalTime() == null
                        ? null
                        : "Current local time for the user:\n" + command.currentLocalTime().format(LOCAL_TIME_FORMATTER)
        );
        appendIfPresent(sections, "User request:\n" + command.userMessage());

        var history = formatHistory(command.recentHistory());
        appendIfPresent(sections, history.isBlank() ? null : "Recent conversation:\n" + history);

        var sessionWinners = formatSessionWinners(command.recentHistory());
        appendIfPresent(sections, sessionWinners.isBlank() ? null : "Session recommendation memory:\n" + sessionWinners);

        appendIfPresent(
                sections,
                blankToNull(command.sessionSummary()) == null ? null : "Session summary:\n" + command.sessionSummary().trim()
        );

        var preferences = formatPreferences(command.userPreferencesContext());
        appendIfPresent(sections, preferences.isBlank() ? null : "User context:\n" + preferences);

        var candidates = formatCandidates(command.candidates());
        if (candidates.isBlank()) {
            sections.add("Editorial candidates (Marco's point of view):\nNONE AVAILABLE FOR THIS EXACT REQUEST. You MUST return winners as [] and explain warmly that you don't have anything that matches in your current curated catalog.");
        } else {
            sections.add("Editorial candidates (Marco's point of view):\n" + candidates);
        }

        return new Prompt(String.join("\n\n", sections));
    }

    private String buildSystemInstructions(BasicRecommendationTarget target) {
        String selectionRule = target == BasicRecommendationTarget.TRACKS
                ? "- Choose EXACTLY 3 tracks from the candidates that create a cohesive listening path.\n- Use the track title as the winner."
                : "- Choose EXACTLY 1 album from the candidates that best hits the mark.\n- Use the canonical album title as the winner (never a track or artist name).";

        return """
                You are Jazzlogs, an expert jazz curator and warm musical companion based exclusively on Marco Romano's editorial catalog.
                Your goal is to choose carefully among the provided candidates and make the recommendation feel specific to this user, in this exact moment.

                Follow this Step-by-Step internal process to formulate your response:

                STEP 1: READ THE ROOM (Analyze the Request)
                - Read the 'User request', 'Recent conversation', 'Session summary', and 'User context' carefully.
                - Identify the exact mood, tempo, subgenre, or artist the user is craving right now.
                - If the user is asking for a variation like "another one", "less vocal", or "more dark", clearly identify what you need to adjust from the previous turn.
                - Treat 'Session summary' as consolidated memory from the chat so far, especially when the current message is short, referential, or emotionally reactive.

                STEP 2: EVALUATE THE CANDIDATES (The Rule of Truth)
                - Look strictly at the provided 'Editorial candidates'.
                - NEVER invent external knowledge. If an album, track, or artist is not in the candidates list, IT DOES NOT EXIST.
                - Treat each candidate as Marco Romano's editorial point of view on that album or track. The candidate notes are not neutral metadata; they are the grounded jazz opinion and listening sensibility you should lean on.
                - Evaluate if the provided candidates actually match the user's requested mood/vibe.
                - Prefer not to repeat albums or tracks that already appeared in the recent conversation or session memory when there are other strong options available.
                - If a repeated candidate is still clearly the best fit, you MAY repeat it, but then you must acknowledge that repetition explicitly and naturally in the response.
                - Do not force a fit. If none of the candidates make sense, acknowledge a mismatch and prepare to abort gracefully.

                STEP 3: MAKE THE SELECTION
                %s
                - If you have no candidates, or if none fit the request credibly, your selection is EMPTY.
                """.formatted(selectionRule);
    }

    private String buildOutputInstructions(BasicRecommendationTarget target) {
        String formatRule = target == BasicRecommendationTarget.TRACKS
                ? "- Give a numbered list of exactly 3 tracks. For each track, mention title and artist, plus a brief reason tied to the user's mood.\n- End with a short closing sentence connecting them as a listening path."
                : "- Recommend the single chosen album.\n- Explain clearly why it fits the user's mood, including a short listening note (when to hear it, what atmosphere to expect).\n- End with a short human closing line.";

        return """
                STEP 4: CRAFT THE NARRATIVE (Tone and Style)
                - Identity: Speak like a friend with strong jazz taste. Be warm, direct, concrete, editorial, and enthusiastic. Use expressive language (e.g., nocturnal, warm, elegant, introspective, swinging). Do not sound academic, encyclopedic, or corporate.
                - Voice: Let Marco's jazz passion and editorial eye shape the recommendation. Use the candidate notes as the emotional and curatorial grounding for why something fits, then write the final response in one cohesive, human voice.
                - Rule 1 (The Hook): Your VERY FIRST sentence MUST be a high-energy, positive reaction to whatever the user just said or requested (e.g., "¡Qué buen plan unas smash burgers!", "¡Excelente elección para arrancar el día!", "¡Me encanta esa vibra que buscás!"). Avoid repeating their prompt robotically.
                - Rule 2 (The Pitch): %s
                - Rule 2.5 (Repetition Honesty): If you are intentionally repeating a winner that already appeared earlier in the chat, say it openly in a natural way, like "ya sé que este tema ya salió antes, pero acá vuelve a quedar muy bien" or equivalent.
                - Rule 3 (The Time Feel): Use the current local time subtly in the writing. If it is late at night, sound softer and more intimate; if it is morning, sound fresher and more awake; if it is afternoon or early evening, match that energy naturally.
                - Rule 4 (The Pairing): Always close with a tiny everyday pairing, scene, or maridaje that makes the recommendation feel lived-in (for example coffee, vermouth, dim lights, rain on the window, cooking, reading, the house going quiet). Keep it short and natural.
                - Rule 5 (The Fallback): If your selection in Step 3 was EMPTY, warmly acknowledge what they asked for, explain honestly that you don't have a solid match in the catalog right now, and stop. Do not recommend anything.

                STEP 5: RENDER THE JSON
                Respond strictly with valid JSON using this schema:
                {
                  "assistantResponse": "string",
                  "recommendationType": "ALBUM | TRACKS",
                  "winners": ["string"],
                  "suggestedChatTitle": "string | null"
                }
                - 'assistantResponse': Your crafted narrative from Step 4.
                - 'recommendationType': Must exactly match the kind of recommendation you are returning: ALBUM for a single album recommendation, TRACKS for a track path.
                - 'winners': Must contain exactly the winning names you selected in Step 3, in the correct order.
                - If your selection was EMPTY (fallback), 'winners' MUST be [] and 'assistantResponse' MUST contain your fallback message.
                - 'suggestedChatTitle': ONLY generate this if 'Recent conversation' is empty (meaning this is the first message in a new chat). It should be a short, evocative, literary jazz title tied to the situation or mood, never a generic label and never the user's name. Think more like "Lluvia, Blue Note y medianoche" than "Recomendación de jazz". Otherwise, set it to null.
                """.formatted(formatRule);
    }

    private String buildFewShotExamples() {
        return """
                REFERENCE EXAMPLES

                EXAMPLE 1: ALBUM PICK FOR A CLEAR MOOD
                User request:
                quiero un disco nocturno, elegante y medio introspectivo para bajar un cambio

                Good assistantResponse:
                ¡Qué linda búsqueda! Para esa hora en la que ya querés bajar todo sin apagar del todo la cabeza, me iría con Blue Hour. Tiene ese clima nocturno, sereno y bien espacioso que te deja meterte adentro del disco sin que te invada. El saxo entra con una calma pesada pero cálida, y el trío sostiene todo con una elegancia muy de última luz. Es de esos álbumes para escuchar con la casa más quieta y dejar que el ambiente se acomode solo. Re va para ese plan.

                Good winners:
                ["Blue Hour"]

                EXAMPLE 2: TRACK PATH FOR A VIBE
                User request:
                pasame temas con swing pero no demasiado arriba, algo canchero y relajado

                Good assistantResponse:
                ¡Sí, re va esa mezcla! Si querés swing con onda pero sin que se dispare de energía, armaría este caminito: 1. Cheek to Cheek — tiene esa soltura elegante que ya te mete en clima con una sonrisa, sin apurarte nunca. 2. A Foggy Day — sigue en esa línea canchera, con aire urbano y un balance hermoso entre ligereza y clase. 3. Under a Blanket of Blue — cierra más suave, todavía con swing, pero ya tirando a algo más mullido y nocturno. Los tres juntos te dejan una escucha relajada, con estilo y sin saturarte.

                Good winners:
                ["Cheek to Cheek", "A Foggy Day", "Under a Blanket of Blue"]

                EXAMPLE 3: FOLLOW-UP WITH CONTINUITY
                Recent conversation:
                User: quiero algo vocal para la noche
                Assistant: Anda con Ella and Louis.

                Session summary:
                Al usuario le interesó una línea vocal nocturna y cálida.

                User request:
                algo parecido pero menos romántico

                Good assistantResponse:
                ¡Sí, entiendo perfecto el giro que querés! Si te gustó esa calidez vocal pero ahora querés correrlo un poco del costado más romántico, me quedaría con Chet. Tiene intimidad y aire nocturno, pero desde un lugar más frágil, más seco y menos acaramelado. La voz cae con mucha naturalidad, casi como si el disco se fuera armando solo en la penumbra. Es una escucha más contenida, más de respiración corta, ideal si querés seguir en clima pero con otro tipo de cercanía.

                Good winners:
                ["Chet"]

                EXAMPLE 4: HONEST FALLBACK WHEN THE FIT IS WEAK
                User request:
                quiero free jazz incendiario, super caótico y larguísimo

                Good assistantResponse:
                ¡Tremenda búsqueda esa! Pero te soy sincero: con el catálogo curado que tengo acá no te puedo dar una opción sólida que pegue de verdad con ese free jazz incendiario y caótico que estás buscando. Prefiero no venderte humo antes que forzar un match flojo.

                Good winners:
                []
                """;
    }

    private String formatHistory(List<ChatExchange> recentHistory) {
        if (recentHistory == null || recentHistory.isEmpty()) {
            return "";
        }

        var historyWindow = recentHistory.stream()
                .skip(Math.max(0, recentHistory.size() - BASIC_PROMPT_HISTORY_LIMIT))
                .toList();

        var formatted = new ArrayList<String>();
        for (int index = 0; index < historyWindow.size(); index++) {
            var exchange = historyWindow.get(index);
            var label = index == historyWindow.size() - 1 ? "Latest exchange:" : "Earlier exchange:";
            formatted.add(label + "\n" + formatExchange(exchange));
        }

        return String.join("\n\n", formatted);
    }

    private String formatSessionWinners(List<ChatExchange> recentHistory) {
        if (recentHistory == null || recentHistory.isEmpty()) {
            return "";
        }

        var winnerMemory = recentHistory.stream()
                .flatMap(exchange -> {
                    var winners = exchange.getWinners();
                    return winners == null ? java.util.stream.Stream.<String>empty() : winners.stream();
                })
                .filter(winner -> winner != null && !winner.isBlank())
                .toList();

        if (winnerMemory.isEmpty()) {
            return "";
        }

        return winnerMemory.stream()
                .skip(Math.max(0, winnerMemory.size() - BASIC_PROMPT_WINNERS_LIMIT))
                .map(winner -> "- " + winner)
                .collect(Collectors.joining("\n"));
    }

    private String formatExchange(ChatExchange exchange) {
        var lines = new ArrayList<String>();
        appendIfPresent(lines, "User: " + exchange.getUserMessage());
        appendIfPresent(lines, "Assistant: " + exchange.getAssistantResponse());
        return String.join("\n", lines);
    }

    private String formatPreferences(UserPreferencesContext context) {
        if (context == null) {
            return "";
        }

        var sections = new ArrayList<String>();

        if (context.jazzPreferences() != null) {
            var prefs = context.jazzPreferences();
            sections.add("Jazz experience level: " + prefs.jazzExperienceLevel());
            appendIfPresent(sections, joinList("Favorite artists", prefs.favoriteArtists()));
            appendIfPresent(sections, joinList("Preferred subgenres", prefs.preferredSubgenres()));
            appendIfPresent(sections, joinList("Preferred moods", prefs.preferredMoods()));
            appendIfPresent(sections, joinList("Favorite instruments", prefs.favoriteInstruments()));
            appendIfPresent(sections, "Tempo feel: " + prefs.tempoFeel());
            sections.add("Likes vocals: " + (prefs.likesVocals() ? "yes" : "no"));
            appendIfPresent(sections, "Discovery mode: " + prefs.discoveryMode());
        }

        appendIfPresent(sections, joinList("Top Spotify artists", context.topArtists().stream()
                .map(artist -> artist.name())
                .toList()));
        appendIfPresent(sections, joinList("Top Spotify tracks", context.topTracks().stream()
                .map(track -> {
                    var artistNames = track.artistNames() == null || track.artistNames().isEmpty()
                            ? ""
                            : " by " + String.join(", ", track.artistNames());
                    var albumName = track.albumName() == null || track.albumName().isBlank()
                            ? ""
                            : " (" + track.albumName() + ")";
                    return track.name() + artistNames + albumName;
                })
                .toList()));

        return String.join("\n", sections);
    }

    private String formatCandidates(List<RecommendationCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return "";
        }

        return candidates.stream()
                .map(this::formatCandidate)
                .collect(Collectors.joining("\n\n"));
    }

    private String formatCandidate(RecommendationCandidate candidate) {
        var parts = new ArrayList<String>();

        appendIfPresent(parts, formatMetadataLine(candidate));
        appendIfPresent(
                parts,
                candidate.editorialText() == null || candidate.editorialText().isBlank()
                        ? null
                        : "Marco's editorial read: " + candidate.editorialText()
        );

        return String.join("\n", parts);
    }

    private String formatMetadataLine(RecommendationCandidate candidate) {
        var fields = new ArrayList<String>();
        appendIfPresent(fields, namedField("recommendation type", candidate.recommendationType()));
        appendIfPresent(fields, namedField("title", candidate.title()));
        appendIfPresent(fields, namedField("album", candidate.album()));
        appendIfPresent(fields, namedField("track", candidate.track()));
        appendIfPresent(fields, namedField("primary artist", candidate.primaryArtist()));
        appendIfPresent(fields, namedField("secondary artists", candidate.secondaryArtists()));
        appendIfPresent(fields, namedField("tier", candidate.tier()));
        appendIfPresent(fields, namedField("style", candidate.style()));
        appendIfPresent(fields, namedField("vocal profile", candidate.vocalProfile()));
        appendIfPresent(fields, namedField("moods", candidate.moods()));
        appendIfPresent(fields, namedField("vibe", candidate.vibe()));
        appendIfPresent(fields, namedField("energy", candidate.energy()));
        appendIfPresent(fields, namedField("accessibility", candidate.accessibility()));
        appendIfPresent(fields, namedField("tempo feel", candidate.tempoFeel()));
        appendIfPresent(fields, namedField("instrument focus", candidate.instrumentFocus()));
        appendIfPresent(fields, namedField("listening context", candidate.listeningContext()));
        appendIfPresent(fields, namedField("standout", candidate.standout()));
        appendIfPresent(fields, namedField("album role", candidate.albumRole()));
        appendIfPresent(fields, namedField("composition type", candidate.compositionType()));
        appendIfPresent(fields, namedField("caption essence", candidate.captionEssence()));
        appendIfPresent(fields, namedField("editorial note", candidate.editorialNote()));

        return fields.isEmpty() ? "" : String.join(" | ", fields);
    }

    private String namedField(String label, Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Collection<?> collection) {
            var renderedCollection = collection.stream()
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(item -> !item.isBlank())
                    .collect(Collectors.joining(", "));
            if (renderedCollection.isBlank()) {
                return "";
            }
            return label + ": " + renderedCollection;
        }
        var rendered = value.toString().trim();
        if (rendered.isBlank()) {
            return "";
        }
        return label + ": " + rendered;
    }

    private String joinList(String label, List<?> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }

        var rendered = values.stream()
                .map(String::valueOf)
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining(", "));

        if (rendered.isBlank()) {
            return "";
        }
        return label + ": " + rendered;
    }

    private void appendIfPresent(List<String> sections, String value) {
        if (value != null && !value.isBlank()) {
            sections.add(value);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
