package mechanist;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Tiny native JSON object reader for flat engine-owned records. */
final class SimpleJson {
    static Map<String, String> object(String json) {
        Map<String, String> out = new LinkedHashMap<>();
        if (json == null) return out;
        int i = 0;
        while (i < json.length()) {
            int keyStart = json.indexOf('"', i);
            if (keyStart < 0) break;
            int keyEnd = findStringEnd(json, keyStart + 1);
            if (keyEnd < 0) break;
            String key = unescape(json.substring(keyStart + 1, keyEnd));
            int colon = json.indexOf(':', keyEnd + 1);
            if (colon < 0) break;
            int valueStart = skipWhitespace(json, colon + 1);
            Value value = readValue(json, valueStart);
            out.put(key, value.text());
            i = Math.max(value.nextIndex(), valueStart + 1);
        }
        return out;
    }

    static int intValue(String s, int fallback) { try { return s == null ? fallback : Integer.parseInt(stripQuotes(s)); } catch (RuntimeException ex) { return fallback; } }
    static double doubleValue(String s, double fallback) { try { return s == null ? fallback : Double.parseDouble(stripQuotes(s)); } catch (RuntimeException ex) { return fallback; } }
    static Instant instantValue(String s, Instant fallback) { try { return s == null ? fallback : Instant.parse(stripQuotes(s)); } catch (RuntimeException ex) { return fallback; } }

    private record Value(String text, int nextIndex) { }

    private static Value readValue(String json, int start) {
        if (start >= json.length()) return new Value("", start);
        char c = json.charAt(start);
        if (c == '"') {
            int end = findStringEnd(json, start + 1);
            return new Value(unescape(json.substring(start + 1, Math.max(start + 1, end))), end + 1);
        }
        if (c == '{' || c == '[') {
            int end = findBalanced(json, start, c, c == '{' ? '}' : ']');
            return new Value(json.substring(start, Math.max(start + 1, end + 1)), end + 1);
        }
        int end = start;
        while (end < json.length() && ",}\n\r".indexOf(json.charAt(end)) < 0) end++;
        return new Value(json.substring(start, end).trim(), end);
    }

    private static int findBalanced(String s, int start, char open, char close) {
        int depth = 0;
        boolean string = false;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' && (i == 0 || s.charAt(i - 1) != '\\')) string = !string;
            if (string) continue;
            if (c == open) depth++;
            if (c == close && --depth == 0) return i;
        }
        return s.length() - 1;
    }

    private static int findStringEnd(String s, int start) {
        for (int i = start; i < s.length(); i++) if (s.charAt(i) == '"' && s.charAt(i - 1) != '\\') return i;
        return -1;
    }

    private static int skipWhitespace(String s, int i) { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; return i; }
    private static String stripQuotes(String s) { return s == null ? "" : s.replaceAll("^\\\"|\\\"$", ""); }
    private static String unescape(String s) { return s.replace("\\\"", "\"").replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t").replace("\\\\", "\\"); }
    private SimpleJson() { }
}
