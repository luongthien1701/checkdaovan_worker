package util;

import model.MatchResult;
import model.Request;
import model.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonUtil {
    private JsonUtil() {}

    public static Request parseRequest(String json) throws IOException {
        Object parsed = new Parser(json).parseValue();
        if (!(parsed instanceof Map)) {
            throw new IOException("JSON is not an object");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> obj = (Map<String, Object>) parsed;
        Request request = new Request();
        request.setRequestId(stringValue(obj.get("requestId")));
        request.setUserId(stringValue(obj.get("userId")));
        request.setFileName(stringValue(obj.get("fileName")));
        request.setFullText(stringValue(obj.get("fullText")));
        return request;
    }

    public static String toJson(Response response) {
        Map<String, Object> obj = new LinkedHashMap<>();
        if (response.getRequestId() != null) {
            obj.put("requestId", response.getRequestId());
        }
        obj.put("status", response.getStatus());
        if (response.getErrorMessage() != null) {
            obj.put("errorMessage", response.getErrorMessage());
        }
        if (response.getMatches() != null) {
            List<Map<String, Object>> matches = new ArrayList<>();
            for (MatchResult match : response.getMatches()) {
                Map<String, Object> matchObj = new LinkedHashMap<>();
                matchObj.put("fileName", match.getFileName());
                matchObj.put("similarity", match.getSimilarity());
                matchObj.put("description", match.getDescription());
                matches.add(matchObj);
            }
            obj.put("matches", matches);
        }
        return Writer.write(obj);
    }

    private static String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private static final class Parser {
        private final String input;
        private int index;

        Parser(String input) {
            this.input = input;
        }

        Object parseValue() throws IOException {
            skipWhitespace();
            if (index >= input.length()) {
                throw new IOException("Unexpected end of JSON");
            }
            char c = input.charAt(index);
            switch (c) {
                case '{':
                    return parseObject();
                case '[':
                    return parseArray();
                case '"':
                    return parseString();
                case 't':
                case 'f':
                    return parseBoolean();
                case 'n':
                    return parseNull();
                default:
                    if (c == '-' || Character.isDigit(c)) {
                        return parseNumber();
                    }
                    throw new IOException("Unexpected character: " + c);
            }
        }

        private Map<String, Object> parseObject() throws IOException {
            Map<String, Object> map = new LinkedHashMap<>();
            expect('{');
            skipWhitespace();
            if (peek('}')) {
                index++;
                return map;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                skipWhitespace();
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                if (peek('}')) {
                    index++;
                    break;
                }
                expect(',');
            }
            return map;
        }

        private List<Object> parseArray() throws IOException {
            List<Object> list = new ArrayList<>();
            expect('[');
            skipWhitespace();
            if (peek(']')) {
                index++;
                return list;
            }
            while (true) {
                Object value = parseValue();
                list.add(value);
                skipWhitespace();
                if (peek(']')) {
                    index++;
                    break;
                }
                expect(',');
            }
            return list;
        }

        private String parseString() throws IOException {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (index < input.length()) {
                char c = input.charAt(index++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    if (index >= input.length()) {
                        throw new IOException("Unterminated escape");
                    }
                    char esc = input.charAt(index++);
                    switch (esc) {
                        case '"':
                        case '\\':
                        case '/':
                            sb.append(esc);
                            break;
                        case 'b':
                            sb.append('\b');
                            break;
                        case 'f':
                            sb.append('\f');
                            break;
                        case 'n':
                            sb.append('\n');
                            break;
                        case 'r':
                            sb.append('\r');
                            break;
                        case 't':
                            sb.append('\t');
                            break;
                        case 'u':
                            if (index + 4 > input.length()) {
                                throw new IOException("Invalid unicode escape");
                            }
                            String hex = input.substring(index, index + 4);
                            sb.append((char) Integer.parseInt(hex, 16));
                            index += 4;
                            break;
                        default:
                            throw new IOException("Invalid escape: " + esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            throw new IOException("Unterminated string");
        }

        private Boolean parseBoolean() throws IOException {
            if (match("true")) {
                return Boolean.TRUE;
            }
            if (match("false")) {
                return Boolean.FALSE;
            }
            throw new IOException("Invalid boolean literal");
        }

        private Object parseNull() throws IOException {
            if (match("null")) {
                return null;
            }
            throw new IOException("Invalid null literal");
        }

        private Number parseNumber() throws IOException {
            int start = index;
            if (peek('-')) {
                index++;
            }
            while (index < input.length() && Character.isDigit(input.charAt(index))) {
                index++;
            }
            if (peek('.')) {
                index++;
                while (index < input.length() && Character.isDigit(input.charAt(index))) {
                    index++;
                }
            }
            if (peek('e') || peek('E')) {
                index++;
                if (peek('+') || peek('-')) {
                    index++;
                }
                while (index < input.length() && Character.isDigit(input.charAt(index))) {
                    index++;
                }
            }
            String number = input.substring(start, index);
            try {
                if (number.contains(".") || number.contains("e") || number.contains("E")) {
                    return Double.parseDouble(number);
                }
                long value = Long.parseLong(number);
                if (value <= Integer.MAX_VALUE && value >= Integer.MIN_VALUE) {
                    return (int) value;
                }
                return value;
            } catch (NumberFormatException e) {
                throw new IOException("Invalid number: " + number);
            }
        }

        private void skipWhitespace() {
            while (index < input.length()) {
                char c = input.charAt(index);
                if (!Character.isWhitespace(c)) {
                    break;
                }
                index++;
            }
        }

        private void expect(char expected) throws IOException {
            if (index >= input.length() || input.charAt(index) != expected) {
                throw new IOException("Expected '" + expected + "'");
            }
            index++;
        }

        private boolean peek(char c) {
            return index < input.length() && input.charAt(index) == c;
        }

        private boolean match(String literal) {
            if (input.startsWith(literal, index)) {
                index += literal.length();
                return true;
            }
            return false;
        }
    }

    private static final class Writer {
        private Writer() {}

        static String write(Object value) {
            StringBuilder sb = new StringBuilder();
            writeValue(sb, value);
            return sb.toString();
        }

        @SuppressWarnings("unchecked")
        private static void writeValue(StringBuilder sb, Object value) {
            if (value == null) {
                sb.append("null");
            } else if (value instanceof String) {
                writeString(sb, (String) value);
            } else if (value instanceof Number || value instanceof Boolean) {
                sb.append(value.toString());
            } else if (value instanceof Map) {
                writeObject(sb, (Map<String, Object>) value);
            } else if (value instanceof List) {
                writeArray(sb, (List<Object>) value);
            } else {
                writeString(sb, value.toString());
            }
        }

        private static void writeObject(StringBuilder sb, Map<String, Object> map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                writeString(sb, entry.getKey());
                sb.append(':');
                writeValue(sb, entry.getValue());
            }
            sb.append('}');
        }

        private static void writeArray(StringBuilder sb, List<Object> list) {
            sb.append('[');
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                writeValue(sb, list.get(i));
            }
            sb.append(']');
        }

        private static void writeString(StringBuilder sb, String value) {
            sb.append('"');
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                switch (c) {
                    case '"':
                    case '\\':
                        sb.append('\\').append(c);
                        break;
                    case '\b':
                        sb.append("\\b");
                        break;
                    case '\f':
                        sb.append("\\f");
                        break;
                    case '\n':
                        sb.append("\\n");
                        break;
                    case '\r':
                        sb.append("\\r");
                        break;
                    case '\t':
                        sb.append("\\t");
                        break;
                    default:
                        if (c < 0x20) {
                            sb.append(String.format("\\u%04x", (int) c));
                        } else {
                            sb.append(c);
                        }
                }
            }
            sb.append('"');
        }
    }
}





