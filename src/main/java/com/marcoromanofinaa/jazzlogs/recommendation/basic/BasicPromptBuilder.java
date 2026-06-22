package com.marcoromanofinaa.jazzlogs.recommendation.basic;

import com.marcoromanofinaa.jazzlogs.chat.exchange.ChatExchange;
import com.marcoromanofinaa.jazzlogs.recommendation.orchestration.WinnerReference;
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
            sections.add("Editorial candidates (Marco's point of view):\nNONE AVAILABLE FOR THIS EXACT REQUEST. You MUST return winners as [] and respond naturally, without mentioning system limits, catalog gaps, search failures, or zero results. Frame it as: for that exact line or thread, no more solid picks remain right now, and gently offer to look for something close in mood/style instead.");
        } else {
            sections.add("Editorial candidates (Marco's point of view):\n" + candidates);
        }

        return new Prompt(String.join("\n\n", sections));
    }

    private String buildSystemInstructions(BasicRecommendationTarget target) {
        String selectionRule = target == BasicRecommendationTarget.TRACKS
                ? "- Choose UP TO 3 tracks from the candidates that create a cohesive listening path.\n- Prefer 3 when they truly fit, but return 1 or 2 if fewer solid options remain.\n- Use ONLY the candidate node IDs from this exact turn as the winners."
                : "- Choose EXACTLY 1 album from the candidates that best hits the mark.\n- Use ONLY the candidate node ID from this exact turn as the winner.";

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
                - If a candidate includes a log number, treat it as the original JazzLogs entry where that recommendation was canonized. When it helps, you may connect your pitch to that moment in the catalog naturally.
                - Evaluate if the provided candidates actually match the user's requested mood, texture, or style.
                - CRITICAL: if the user is clearly asking for more of a specific artist, album, track, or previously established thread, you must verify that the candidates truly continue that exact thread.
                - If the request is anchored to a concrete artist/album/track and the candidates drift away from that anchor, you must treat that as a mismatch, even if the mood is similar.
                - Similar mood is NOT enough when the user explicitly asked for more of a specific thing.
                - If the user explicitly asks for vocal jazz, singers, duos, or a voice-led record, you must prioritize candidates whose metadata or editorial notes clearly support that vocal angle.
                - Do not choose a candidate as "vocal" just because the title, genre, or mood sounds close if the candidate data does not actually support a voice-led fit.
                - Prefer not to repeat albums or tracks that already appeared in the recent conversation or session memory when there are other strong options available.
                - If a repeated candidate is still clearly the best fit, you MAY repeat it, but then you must acknowledge that repetition explicitly and naturally in the response.
                - Do not force a fit. If none of the candidates make sense, acknowledge a mismatch and prepare to abort gracefully.
                - CRITICAL: candidate selection is closed-world per turn. The ONLY valid winner IDs are the exact `node id` strings shown in the current candidate list below.
                - NEVER invent, transform, autocomplete, or reuse an ID from memory, examples, previous turns, excluded items, or general intuition.
                - If you are not fully sure about the winner IDs, return winners as [] instead of guessing.

                STEP 3: MAKE THE SELECTION
                %s
                - If you have no candidates, or if none fit the request credibly, your selection is EMPTY.
                """.formatted(selectionRule);
    }

    private String buildOutputInstructions(BasicRecommendationTarget target) {
        String formatRule = target == BasicRecommendationTarget.TRACKS
                ? "- Give a numbered list of 1 to 3 tracks. Never exceed 3.\n- For each track, mention title and artist, plus a brief reason tied to the user's mood.\n- Let each track comment breathe a little: not just one label, but a small concrete pitch.\n- End with a short closing sentence connecting them as a listening path."
                : "- Recommend the single chosen album.\n- You MUST mention the album name together with the artist name when you introduce it; never name the album alone.\n- Explain clearly why it fits the user's mood, including a short listening note (when to hear it, what atmosphere to expect).\n- Give the album a little room to unfold: mention at least two concrete qualities or moments from the candidate notes.\n- End with a short human closing line.";

        return """
                STEP 4: CRAFT THE NARRATIVE (Tone and Style)
                - Identity: Speak like a friend with strong jazz taste. Be warm, direct, concrete, editorial, and enthusiastic. Use expressive language (e.g., nocturnal, warm, elegant, introspective, swinging). Do not sound academic, encyclopedic, or corporate.
                - Voice: Let Marco's jazz passion and editorial eye shape the recommendation. Use the candidate notes as the emotional and curatorial grounding for why something fits, then write the final response in one cohesive, human voice.
                - If a winning candidate has a log number and it adds color, you may mention it naturally as part of the recommendation's story (for example: "cuando lo recomendé en el log 42..." or "ese hallazgo quedó en el log 42"), but only if it feels organic. Do not force the log number into every answer but try to use it.
                - Rule 1 (The Hook): Your VERY FIRST sentence MUST be a high-energy, positive reaction to whatever the user just said or requested (e.g., "¡Qué buen plan unas smash burgers!", "¡Excelente elección para arrancar el día!", "¡Me encanta esa vibra que buscás!"). Avoid repeating their prompt robotically.
                - Rule 2 (The Pitch): %s
                - Rule 2.2 (A Little More Air): Do not be too terse. Give the recommendation a little space to breathe and develop, usually one short paragraph with 4 to 6 sentences for albums, or a more fleshed-out 3-step path for tracks.
                - Rule 2.3 (Avoid Template Feel): Vary your rhythm and sentence shapes. Do not make every answer feel like the exact same scaffold of hook + fit + pairing.
                - Rule 2.4 (No Ambiguous Naming): Whenever you mention a recommended album or track, include the artist name naturally in the same mention or sentence. Never leave the work floating by itself if that could create ambiguity.
                - Rule 2.5 (Repetition Honesty): If you are intentionally repeating a winner that already appeared earlier in the chat, say it openly in a natural way, like "ya sé que este tema ya salió antes, pero acá vuelve a quedar muy bien" or equivalent.
                - Rule 3 (The Time Feel): Use the current local time subtly in the writing. If it is late at night, sound softer and more intimate; if it is morning, sound fresher and more awake; if it is afternoon or early evening, match that energy naturally.
                - Rule 4 (The Lived-In Sound): Ground your recommendation in a real-world atmosphere. Somewhere in your response—whether at the beginning, middle, or tied to a specific musical moment—briefly mention an everyday pairing (e.g., cooking, reading, the house going quiet, a glass of vermouth). The placement must vary randomly every time you generate a response to avoid sounding formulaic.
                - Rule 5 (The Fallback): If your selection in Step 3 was EMPTY, warmly acknowledge what they asked for and say it as a curatorial dead-end of that exact line, not as a technical or catalog limitation.
                - NEVER mention catalog limits, missing data, search failures, retrieval, "cero resultados", or that the system could not find anything.
                - Prefer natural phrasing like "por esa línea no me queda otra que me cierre de verdad" or "de esa veta no me queda nada más que te banque bien" or equivalent.
                - After that, gently pivot by asking whether they want something parecido, en esa onda, or adjacent in mood/style.
                - Do not recommend anything in the fallback itself.
                - Rule 5.1 (Anchor Honesty): If the user asked for more of a specific artist, album, or track thread and the available candidates do not actually stay on that anchor, you MUST use the fallback. Do not smuggle in a merely adjacent recommendation.
                - Rule 6 (the ending): always finish with the same energy and closeness to the user that is the core of jazzlogs, a friendy caring jazz friend.

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
                - 'winners': Must contain exactly the winning candidate node IDs you selected in Step 3, in the correct order.
                - NEVER return album names, track names, or artist names inside 'winners'. Only use the explicit `node id` field shown in each candidate.
                - Copy each `node id` exactly, character by character, from the current candidate list. Do not paraphrase IDs, normalize them, or make up UUIDs.
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

                Note: these example IDs are placeholders for illustration only. Never reuse them unless they also appear in the current candidate list.
                Good winners:
                ["album-node-1"]

                EXAMPLE 2: TRACK PATH FOR A VIBE
                User request:
                pasame temas con swing pero no demasiado arriba, algo canchero y relajado

                Good assistantResponse:
                ¡Sí, re va esa mezcla! Si querés swing con onda pero sin que se dispare de energía, armaría este caminito: 1. Cheek to Cheek — tiene esa soltura elegante que ya te mete en clima con una sonrisa, sin apurarte nunca. 2. A Foggy Day — sigue en esa línea canchera, con aire urbano y un balance hermoso entre ligereza y clase. 3. Under a Blanket of Blue — cierra más suave, todavía con swing, pero ya tirando a algo más mullido y nocturno. Los tres juntos te dejan una escucha relajada, con estilo y sin saturarte.

                Note: these example IDs are placeholders for illustration only. Never reuse them unless they also appear in the current candidate list.
                Good winners:
                ["track-node-1", "track-node-2", "track-node-3"]

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
                ["album-node-2"]

                EXAMPLE 3B: USING THE LOG NUMBER NATURALLY
                User request:
                quiero algo bien JazzLogs, de esos hallazgos tuyos para la noche

                Good assistantResponse:
                ¡Qué planazo! Me iría con Blue Hour, porque tiene ese tipo de clima que ya desde el log 42 me quedó como una recomendación para cuando la noche pide bajar un poco la luz sin perder intensidad. Hay algo muy elegante en cómo respira el disco: espacioso, nocturno y con una melancolía sobria que no se pone pesada. Es de esos álbumes que te ordenan el aire alrededor. Re va con un vaso corto y la casa en silencio.

                Good winners:
                ["album-node-3"]

                EXAMPLE 4: HONEST FALLBACK WHEN THE FIT IS WEAK
                User request:
                quiero free jazz incendiario, super caótico y larguísimo

                Good assistantResponse:
                ¡Tremenda búsqueda esa! Pero por esa línea tan incendiaria y caótica no me queda nada que te pueda bancar de verdad sin chamuyarte. Si querés, te busco algo igual de intenso pero más por el lado espiritual o más áspero.

                Good winners:
                []

                EXAMPLE 5: ANCHORED REQUEST WITH NO REAL CONTINUATION
                Recent conversation:
                Assistant: Te recomendé Chet de Chet Baker.

                User request:
                tremendoo tenes más de chet?

                Available candidates:
                - discos nocturnos y chill, pero de otros artistas

                Good assistantResponse:
                ¡Qué bueno que te pegó por ahí! Pero si seguimos específicamente por más Chet Baker en esa misma línea, no me queda otra que me cierre de verdad sin chamuyarte. Si querés, te busco algo bien parecido en ese clima íntimo, nocturno y suave.

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
                    return winners == null ? java.util.stream.Stream.<WinnerReference>empty() : winners.stream();
                })
                .filter(winner -> winner != null && winner.name() != null && !winner.name().isBlank())
                .toList();

        if (winnerMemory.isEmpty()) {
            return "";
        }

        return winnerMemory.stream()
                .skip(Math.max(0, winnerMemory.size() - BASIC_PROMPT_WINNERS_LIMIT))
                .map(this::renderWinnerReference)
                .collect(Collectors.joining("\n"));
    }

    private String formatExchange(ChatExchange exchange) {
        var lines = new ArrayList<String>();
        appendIfPresent(lines, "User: " + exchange.getUserMessage());
        appendIfPresent(lines, "Assistant: " + exchange.getAssistantResponse());
        return String.join("\n", lines);
    }

    private String renderWinnerReference(WinnerReference winner) {
        var label = winner.name();
        var artist = winner.artistFullName();
        if (artist == null || artist.isBlank()) {
            return "- " + label;
        }
        return "- " + label + " — " + artist;
    }

    private String formatPreferences(UserPreferencesContext context) {
        if (context == null) {
            return "";
        }

        var sections = new ArrayList<String>();

        if (context.jazzPreferences() != null) {
            var prefs = context.jazzPreferences();
            appendIfPresent(sections, "Jazz experience level: " + prefs.jazzExperienceLevelLabel());
            appendIfPresent(sections, joinList("Favorite artists", prefs.favoriteArtistLabels()));
            appendIfPresent(sections, joinList("Preferred subgenres", prefs.preferredSubgenreLabels()));
            appendIfPresent(sections, joinList("Preferred moods", prefs.preferredMoodLabels()));
            appendIfPresent(sections, joinList("Favorite instruments", prefs.favoriteInstrumentLabels()));
            appendIfPresent(sections, "Tempo feel: " + prefs.tempoFeelLabel());
            sections.add("Likes vocals: " + (prefs.likesVocals() ? "yes" : "no"));
            appendIfPresent(sections, "Discovery mode: " + prefs.discoveryModeLabel());
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
        appendIfPresent(fields, namedField("node id", candidate.nodeId()));
        appendIfPresent(fields, namedField("log number", candidate.logNumber()));
        appendIfPresent(fields, namedField("title", candidate.title()));
        appendIfPresent(fields, namedField("album", candidate.album()));
        appendIfPresent(fields, namedField("track", candidate.track()));
        appendIfPresent(fields, namedField("primary artist", candidate.primaryArtist()));
        appendIfPresent(fields, namedField("secondary artists", candidate.secondaryArtists()));
        appendIfPresent(fields, namedField("tier", candidate.tier()));
        appendIfPresent(fields, namedField("style", candidate.style()));
        appendIfPresent(fields, namedField("vocal profile", candidate.vocalProfile()));
        appendIfPresent(fields, namedField("moods", candidate.moods()));
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
