package util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class TextPreprocessor {
    private static final Pattern DIACRITIC = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern NON_WORD = Pattern.compile("[^a-z0-9\\s]");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    private TextPreprocessor() {}

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String lower = raw.toLowerCase(Locale.ROOT);
        String normalized = Normalizer.normalize(lower, Normalizer.Form.NFD);
        String noMarks = DIACRITIC.matcher(normalized).replaceAll("");
        String clean = NON_WORD.matcher(noMarks).replaceAll(" ");
        return SPACES.matcher(clean).replaceAll(" ").trim();
    }
}

