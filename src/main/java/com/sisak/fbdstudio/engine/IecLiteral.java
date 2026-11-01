package com.sisak.fbdstudio.engine;

import com.sisak.fbdstudio.model.DataType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses and formats IEC 61131-3 literal values (BOOL/numeric/TIME/STRING) to/from runtime Java objects. */
public final class IecLiteral {

    private static final Pattern TIME_COMPONENT = Pattern.compile(
            "(?i)(\\d+(?:\\.\\d+)?)\\s*(ms|s|m|h|d)");

    private IecLiteral() {
    }

    public static Object parse(DataType type, String literal) {
        if (literal == null || literal.isBlank()) {
            return type.defaultValue();
        }
        String s = literal.trim();
        try {
            return switch (type) {
                case BOOL -> parseBool(s);
                case REAL, LREAL -> Double.parseDouble(stripTypePrefix(s));
                case TIME -> parseTimeMillis(s);
                case STRING -> stripQuotes(s);
                case BYTE, WORD, DWORD, SINT, INT, DINT, LINT, USINT, UINT, UDINT, ULINT ->
                        Long.parseLong(stripTypePrefix(s).replace("_", ""));
                default -> s;
            };
        } catch (NumberFormatException e) {
            return type.defaultValue();
        }
    }

    private static Boolean parseBool(String s) {
        String u = s.toUpperCase();
        return u.equals("TRUE") || u.equals("1");
    }

    private static String stripTypePrefix(String s) {
        int hash = s.indexOf('#');
        return hash >= 0 ? s.substring(hash + 1) : s;
    }

    private static String stripQuotes(String s) {
        if (s.length() >= 2 && s.startsWith("'") && s.endsWith("'")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    /** Parses an IEC TIME literal such as {@code T#1h30m}, {@code T#500ms}, {@code T#2s} into milliseconds. */
    public static long parseTimeMillis(String literal) {
        String body = stripTypePrefix(literal);
        Matcher m = TIME_COMPONENT.matcher(body);
        double totalMs = 0;
        boolean any = false;
        while (m.find()) {
            any = true;
            double value = Double.parseDouble(m.group(1));
            String unit = m.group(2).toLowerCase();
            totalMs += switch (unit) {
                case "ms" -> value;
                case "s" -> value * 1000;
                case "m" -> value * 60_000;
                case "h" -> value * 3_600_000;
                case "d" -> value * 86_400_000;
                default -> 0;
            };
        }
        if (!any) {
            // Fall back to a plain millisecond number.
            return (long) Double.parseDouble(body);
        }
        return (long) totalMs;
    }

    public static String formatTimeMillis(long millis) {
        if (millis == 0) {
            return "T#0s";
        }
        long rem = millis;
        long days = rem / 86_400_000;
        rem %= 86_400_000;
        long hours = rem / 3_600_000;
        rem %= 3_600_000;
        long minutes = rem / 60_000;
        rem %= 60_000;
        long seconds = rem / 1000;
        long ms = rem % 1000;
        StringBuilder sb = new StringBuilder("T#");
        if (days > 0) sb.append(days).append('d');
        if (hours > 0) sb.append(hours).append('h');
        if (minutes > 0) sb.append(minutes).append('m');
        if (seconds > 0) sb.append(seconds).append('s');
        if (ms > 0 || sb.length() == 2) sb.append(ms).append("ms");
        return sb.toString();
    }

    public static String format(DataType type, Object value) {
        if (value == null) {
            return "";
        }
        if (type == DataType.TIME && value instanceof Number n) {
            return formatTimeMillis(n.longValue());
        }
        if (type == DataType.STRING) {
            return "'" + value + "'";
        }
        if (type == DataType.BOOL) {
            return Boolean.TRUE.equals(value) ? "TRUE" : "FALSE";
        }
        return String.valueOf(value);
    }
}
