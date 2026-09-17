package ro.noadsx15.hybrid;

import java.util.Locale;
import java.util.Set;

public final class DomainRules {

    private DomainRules() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }

        String result = value.trim().toLowerCase(Locale.US);

        while (result.startsWith(".")) {
            result = result.substring(1);
        }

        while (result.endsWith(".") && !result.isEmpty()) {
            result = result.substring(0, result.length() - 1);
        }

        return result;
    }

    public static String parse(String line) {
        if (line == null) {
            return null;
        }

        String value = line.trim();

        if (value.isEmpty()
                || value.startsWith("#")
                || value.startsWith("!")
                || value.startsWith("@@")) {
            return null;
        }

        if (value.startsWith("||")) {
            value = value.substring(2);
        }

        int optionsPosition = value.indexOf('$');
        if (optionsPosition >= 0) {
            value = value.substring(0, optionsPosition);
        }

        while (value.endsWith("^") || value.endsWith("|")) {
            value = value.substring(0, value.length() - 1);
        }

        if (value.startsWith("0.0.0.0 ")
                || value.startsWith("127.0.0.1 ")) {
            value = value.substring(value.indexOf(' ') + 1).trim();
        }

        if (value.contains("/")
                || value.contains("*")
                || value.contains(" ")
                || !value.contains(".")) {
            return null;
        }

        value = normalize(value);

        if (!value.matches("[a-z0-9._-]+")) {
            return null;
        }

        return value;
    }

    public static boolean matches(String host, Set<String> rules) {
        String candidate = normalize(host);

        while (!candidate.isEmpty()) {
            if (rules.contains(candidate)) {
                return true;
            }

            int dotPosition = candidate.indexOf('.');

            if (dotPosition < 0) {
                return false;
            }

            candidate = candidate.substring(dotPosition + 1);
        }

        return false;
    }
}
