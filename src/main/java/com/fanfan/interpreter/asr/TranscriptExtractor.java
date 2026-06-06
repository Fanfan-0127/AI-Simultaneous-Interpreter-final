package com.fanfan.interpreter.asr;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Optional;

public final class TranscriptExtractor {
    private TranscriptExtractor() {}

    public static Optional<Transcript> extract(JsonObject event) {
        Optional<String> text = firstString(event, "transcript")
                .or(() -> firstString(event, "text"))
                .or(() -> firstString(event, "delta"));
        if (text.isEmpty() || text.get().isBlank()) return Optional.empty();
        String type = directString(event, "type").orElse("");
        boolean finalResult = type.contains("completed") || type.contains("final")
                || directBoolean(event, "is_final").orElse(false)
                || directBoolean(event, "final").orElse(false);
        return Optional.of(new Transcript(text.get(), finalResult));
    }

    private static Optional<String> firstString(JsonElement element, String key) {
        if (element == null || element.isJsonNull()) return Optional.empty();
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            Optional<String> direct = directString(object, key);
            if (direct.isPresent()) return direct;
            for (String childKey : object.keySet()) {
                Optional<String> child = firstString(object.get(childKey), key);
                if (child.isPresent()) return child;
            }
        }
        if (element.isJsonArray())
            for (JsonElement child : element.getAsJsonArray()) {
                Optional<String> value = firstString(child, key);
                if (value.isPresent()) return value;
            }
        return Optional.empty();
    }

    private static Optional<String> directString(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive()) return Optional.empty();
        return Optional.of(value.getAsString());
    }

    private static Optional<Boolean> directBoolean(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive()) return Optional.empty();
        return Optional.of(value.getAsBoolean());
    }

    public record Transcript(String text, boolean finalResult) {}
}
