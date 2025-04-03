package dev.lukebemish.codecextras.structured.reflective;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * {@link CreationOption}s for modifying the naming of properties within a structure.
 */
public enum PropertyNamingOption implements CreationOption {
    /**
     * Structure fields are given the same name as the property
     */
    IDENTITY {
        @Override
        protected String formatPart(String part) {
            return part;
        }

        @Override
        protected String joinParts(List<String> parts) {
            return String.join("", parts);
        }
    },

    /**
     * Property names are converted to {@code PascalCase}
     */
    PASCAL_CASE {
        @Override
        protected String formatPart(String part) {
            return capitalizeFirst(part);
        }

        @Override
        protected String joinParts(List<String> parts) {
            return String.join("", parts);
        }
    },

    /**
     * Property names are converted to {@code camelCase}
     */
    CAMEL_CASE {
        @Override
        protected String formatPart(String part) {
            return capitalizeFirst(part);
        }

        @Override
        protected String joinParts(List<String> parts) {
            if (parts.isEmpty()) {
                return "";
            }
            var result = new StringBuilder();
            result.append(parts.getFirst().toLowerCase(Locale.ROOT));
            for (int i = 1; i < parts.size(); i++) {
                result.append(parts.get(i));
            }
            return result.toString();
        }
    },

    /**
     * Property names are converted to {@code snake_case}
     */
    SNAKE_CASE {
        @Override
        protected String formatPart(String part) {
            return part.toLowerCase(Locale.ROOT);
        }

        @Override
        protected String joinParts(List<String> parts) {
            return String.join("_", parts);
        }
    },

    /**
     * Property names are converted to {@code SCREAMING_SNAKE_CASE}
     */
    SCREAMING_SNAKE_CASE {
        @Override
        protected String formatPart(String part) {
            return part.toUpperCase(Locale.ROOT);
        }

        @Override
        protected String joinParts(List<String> parts) {
            return String.join("_", parts);
        }
    },

    /**
     * Property names are converted to {@code kebab-case}
     */
    KEBAB_CASE {
        @Override
        protected String formatPart(String part) {
            return part.toLowerCase(Locale.ROOT);
        }

        @Override
        protected String joinParts(List<String> parts) {
            return String.join("-", parts);
        }
    },

    /**
     * Property names are converted to {@code SCREAMING-KEBAB-CASE}
     */
    SCREAMING_KEBAB_CASE {
        @Override
        protected String formatPart(String part) {
            return part.toUpperCase(Locale.ROOT);
        }

        @Override
        protected String joinParts(List<String> parts) {
            return String.join("-", parts);
        }
    };

    public String format(String name) {
        int firstAlphaChar = -1;
        for (int i = 0; i < name.length(); i++) {
            if (Character.isAlphabetic(name.charAt(i))) {
                firstAlphaChar = i;
                break;
            }
        }
        if (firstAlphaChar == -1) {
            return name;
        }
        var prologue = name.substring(0, firstAlphaChar);
        var parts = new ArrayList<String>();
        var part = new StringBuilder();
        for (int i = firstAlphaChar; i < name.length(); i++) {
            var c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                if (!part.isEmpty()) {
                    parts.add(formatPart(part.toString()));
                    part = new StringBuilder();
                }
            }
            part.append(c);
        }
        parts.add(formatPart(part.toString()));
        return prologue + joinParts(parts);
    }

    protected abstract String formatPart(String part);
    protected abstract String joinParts(List<String> parts);

    private static String capitalizeFirst(String s) {
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }
}
