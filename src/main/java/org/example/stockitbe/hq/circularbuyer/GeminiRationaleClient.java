package org.example.stockitbe.hq.circularbuyer;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GeminiRationaleClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${stockit.circular-buyer.ai.gemini.api-key:}")
    private String apiKey;

    @Value("${stockit.circular-buyer.ai.gemini.base-url:https://generativelanguage.googleapis.com}")
    private String baseUrl;

    @Value("${stockit.circular-buyer.ai.gemini.model:gemini-3.5-flash}")
    private String model;

    public String generate(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY is not configured");
        }

        GeminiGenerateContentResponse response;
        try {
            response = restClientBuilder
                    .baseUrl(baseUrl)
                    .build()
                    .post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/{model}:generateContent")
                            .queryParam("key", apiKey)
                            .build(model))
                    .body(new GeminiGenerateContentRequest(
                            List.of(new Content(List.of(new Part(prompt)))),
                            new GenerationConfig("application/json", 0.2)
                    ))
                    .retrieve()
                    .body(GeminiGenerateContentResponse.class);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException(
                    "Gemini API request failed status=" + e.getStatusCode()
                            + " body=" + abbreviate(e.getResponseBodyAsString(), 500),
                    e
            );
        }

        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new IllegalStateException("empty Gemini response");
        }

        Content content = response.candidates().get(0).content();
        if (content == null || content.parts() == null || content.parts().isEmpty()) {
            throw new IllegalStateException("empty Gemini response content");
        }

        String text = content.parts().stream()
                .map(Part::text)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
        if (text == null) {
            throw new IllegalStateException("empty Gemini response text");
        }
        return text;
    }

    private record GeminiGenerateContentRequest(
            List<Content> contents,
            GenerationConfig generationConfig
    ) {}

    private record GenerationConfig(
            String responseMimeType,
            Double temperature
    ) {}

    private record GeminiGenerateContentResponse(
            List<Candidate> candidates
    ) {}

    private record Candidate(
            Content content
    ) {}

    private record Content(
            List<Part> parts
    ) {}

    private record Part(
            String text
    ) {}

    private static String abbreviate(String value, int maxLength) {
        if (value == null) return "";
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength) + "...";
    }
}
