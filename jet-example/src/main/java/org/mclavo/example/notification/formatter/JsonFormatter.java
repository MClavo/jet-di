package org.mclavo.example.notification.formatter;

public final class JsonFormatter implements MessageFormatter {
    @Override
    public String format(String title, String body) {
        return "{\"title\":\"" + escape(title) + "\",\"body\":\"" + escape(body) + "\"}";
    }

    @Override
    public String name() {
        return "JsonFormatter";
    }

    private String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
