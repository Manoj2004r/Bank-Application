import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal JSON utility for flat (non-nested) objects. Good enough for
 * this API's simple request/response bodies without pulling in an
 * external dependency (keeps the Docker build offline-friendly).
 */
public class Json {

    /** Parses a flat JSON object like {"key":"value","num":123} into a String map. */
    public static Map<String, String> parseFlatObject(String json) {
        Map<String, String> map = new LinkedHashMap<>();
        if (json == null) return map;
        json = json.trim();
        if (json.isEmpty()) return map;
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}")) json = json.substring(0, json.length() - 1);

        int i = 0;
        int n = json.length();
        while (i < n) {
            while (i < n && (Character.isWhitespace(json.charAt(i)) || json.charAt(i) == ',')) i++;
            if (i >= n) break;
            if (json.charAt(i) != '"') break;
            i++;
            StringBuilder key = new StringBuilder();
            while (i < n && json.charAt(i) != '"') {
                key.append(json.charAt(i));
                i++;
            }
            i++; // closing quote
            while (i < n && (Character.isWhitespace(json.charAt(i)) || json.charAt(i) == ':')) i++;
            StringBuilder value = new StringBuilder();
            if (i < n && json.charAt(i) == '"') {
                i++;
                while (i < n && json.charAt(i) != '"') {
                    if (json.charAt(i) == '\\' && i + 1 < n) {
                        i++;
                    }
                    value.append(json.charAt(i));
                    i++;
                }
                i++; // closing quote
            } else {
                while (i < n && json.charAt(i) != ',' && json.charAt(i) != '}') {
                    value.append(json.charAt(i));
                    i++;
                }
            }
            map.put(key.toString(), value.toString().trim());
        }
        return map;
    }

    public static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    public static String str(String key, String value) {
        return "\"" + escape(key) + "\":\"" + escape(value) + "\"";
    }

    public static String num(String key, double value) {
        return "\"" + escape(key) + "\":" + value;
    }
}
