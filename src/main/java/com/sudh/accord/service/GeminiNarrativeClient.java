package com.sudh.accord.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sudh.accord.dto.NarrativeInput;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Thin REST client for Gemini Flash's generateContent endpoint. Deliberately
 * not using the google-generativeai SDK: this is a single-sentence,
 * single-turn generation call once a week per user, and the SDK's
 * conversation/session/tool-calling machinery is unused weight for that. A
 * plain java.net.http.HttpClient call + Jackson (already on the classpath
 * via spring-boot-starter-web) covers it.
 */
@Component
public class GeminiNarrativeClient {

    // Everything the model needs to know about tone is here rather than
    // scattered across code comments, since this is the artifact that
    // actually gets iterated on. Written and reasoned about against the
    // design doc's two flagged worst cases:
    //   1. zero-activity week (earned=0, spent=0, tasks_completed=0,
    //      wallet_low_days=0, streak_days=0 or unrelated to the week) —
    //      Gemini Flash's default instinct is to spin this as "a fresh
    //      start!" or "ready when you are!", which reads as fake enthusiasm
    //      for a week where literally nothing happened.
    //   2. net-negative wallet week (spent > earned, wallet_low_days high) —
    //      the failure mode here is the opposite: the model either goes
    //      falsely cheerful ("every journey has ups and downs!") or tips
    //      into guilt-tripping. Neither is wanted; the sentence should just
    //      state what happened.
    // This has NOT been validated against a live Gemini call (see the
    // caller-side note in WeeklyNarrativeService) — worth a real pass with
    // actual worst-case inputs before this ships.
    private static final String SYSTEM_PROMPT = """
            You write exactly ONE short sentence summarizing a user's past week \
            in a personal finance and productivity app called Accord. The user \
            sets a weekly budget, creates tasks for themselves, and unlocks \
            spending money by completing those tasks.

            You will receive a JSON object with these fields:
            - earned: rupees unlocked this week by completing tasks
            - spent: rupees spent this week
            - tasks_completed: number of tasks completed this week
            - wallet_low_days: number of days this week the wallet balance was \
            under 20% of the user's monthly budget
            - streak_days: the user's current daily check-in streak, as of today

            Rules, in priority order:
            1. Output ONLY the sentence itself. No greeting, no preamble, no \
            quotation marks, no markdown, no emoji.
            2. Exactly one sentence. Under 30 words.
            3. Reflect what the numbers actually show. Do not editorialize \
            beyond what they support, and do not invent activity, causes, or \
            advice that isn't in the input.
            4. Tone is matter-of-fact and mildly warm — like a friend giving a \
            quick, honest recap, not an app trying to keep you engaged. Never \
            use exclamation marks, and never use language like "great job", \
            "keep it up", "fresh start", or "ready when you are".
            5. If tasks_completed is 0 and earned is 0 and spent is 0, this was \
            a quiet week with no app activity at all. Say that plainly and \
            neutrally — do not spin it as a positive ("a fresh start!") or a \
            negative (no guilt-tripping, no "you fell behind"). Just note the \
            week was quiet.
            6. If spent is greater than earned, or wallet_low_days is 4 or \
            more, do not use upbeat framing anywhere in the sentence, even if \
            streak_days or tasks_completed happen to be high that week. State \
            the wallet pressure plainly, as a fact, not as encouragement or a \
            scolding.
            7. Only mention streak_days if it is genuinely part of the week's \
            story (e.g. it's the most notable number, or it's at risk from a \
            gap) — do not force it into every sentence.
            8. Never suggest the user should download, buy, or do anything. No \
            calls to action.
            """;

    @Value("${gemini.api-key}")
    private String apiKey;

    // gemini-2.5-flash as a sane default — confirm this is still the current
    // Flash model name/version before relying on it; verify against
    // https://ai.google.dev/gemini-api/docs/models rather than assuming.
    @Value("${gemini.model:gemini-2.5-flash}")
    private String model;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public GeminiNarrativeClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * @throws NarrativeGenerationException on any HTTP, network, or
     *                                      response-shape failure. Callers
     *                                      should catch this per-user so one
     *                                      user's failure doesn't abort the
     *                                      Sunday batch for everyone else.
     */
    public String generateNarrative(NarrativeInput input) {
        try {
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "system_instruction", Map.of(
                            "parts", new Object[]{Map.of("text", SYSTEM_PROMPT)}
                    ),
                    "contents", new Object[]{
                            Map.of("parts", new Object[]{
                                    Map.of("text", objectMapper.writeValueAsString(input))
                            })
                    },
                    "generationConfig", Map.of(
                            "temperature", 0.6,
                            "maxOutputTokens", 120
                    )
            ));

            URI uri = URI.create(
                    "https://generativelanguage.googleapis.com/v1beta/models/"
                            + model + ":generateContent?key=" + apiKey
            );

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new NarrativeGenerationException(
                        "Gemini returned HTTP " + response.statusCode() + ": " + response.body());
            }

            return extractText(response.body());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new NarrativeGenerationException("Gemini call failed", e);
        }
    }

    private String extractText(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode textNode = root.path("candidates").path(0)
                .path("content").path("parts").path(0).path("text");

        if (!textNode.isTextual() || textNode.asText().isBlank()) {
            throw new NarrativeGenerationException(
                    "Gemini response had no usable text (possibly blocked by a safety filter): " + responseBody);
        }
        return textNode.asText().trim();
    }

    public static class NarrativeGenerationException extends RuntimeException {
        public NarrativeGenerationException(String message) {
            super(message);
        }

        public NarrativeGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}