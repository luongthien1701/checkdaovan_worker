package util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ShingleGenerator {
    private final int shingleSize;

    public ShingleGenerator(int shingleSize) {
        this.shingleSize = Math.max(1, shingleSize);
    }
    public Set<String> shingles(String normalizedText) {
        Set<String> output = new HashSet<>();
        String[] tokens = normalizedText.split("\\s+");
        if (tokens.length == 0) {
            return output;
        }
        if (tokens.length <= shingleSize) {
            output.add(String.join(" ", tokens));
            return output;
        }
        for (int i = 0; i <= tokens.length - shingleSize; i++) {
            String shingle = String.join(" ", Arrays.copyOfRange(tokens, i, i + shingleSize));
            output.add(shingle);
        }
        return output;
    }

    public static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) {
            return 1.0;
        }
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    }
}

