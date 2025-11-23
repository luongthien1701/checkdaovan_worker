package util;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.Delayed;

public class LSHIndex implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int bands;
    private final int rows;
    private final MinHash minHash;
    private final Map<String, int[]> signatureStore = new HashMap<>();
    private final Map<String, String> contentStore = new HashMap<>();
    private final List<Map<String, Set<String>>> buckets = new ArrayList<>();

    public LSHIndex(int bands, int rows, MinHash minHash) {
        this.bands = bands;
        this.rows = rows;
        this.minHash = minHash;
        for (int i = 0; i < bands; i++) {
            buckets.add(new HashMap<>());
        }
    }

    public void add(String fileName, String normalizedContent, Set<String> shingles) {
        int[] signature = minHash.signature(shingles);
        signatureStore.put(fileName, signature);
        contentStore.put(fileName, normalizedContent);
        for (int band = 0; band < bands; band++) {
            int start = band * rows;
            int end = Math.min(signature.length, start + rows);
            int[] slice = Arrays.copyOfRange(signature, start, end);
            String key = Arrays.toString(slice);
            buckets.get(band).computeIfAbsent(key, k -> new HashSet<>()).add(fileName);
        }
    }

    public List<SimilarityResult> query(Set<String> queryShingles, double threshold) {
        int[] querySig = minHash.signature(queryShingles);
        Set<String> candidates = collectCandidates(querySig);
        List<SimilarityResult> results = new ArrayList<>();
        for (String candidate : candidates) {
        	try {
    			Thread.sleep(500);
    		} catch (InterruptedException e) {
    			e.printStackTrace();
    		}

            int[] candidateSig = signatureStore.get(candidate);
            if (candidateSig == null) continue;
            double similarity = minHash.similarity(querySig, candidateSig);
            if (similarity >= threshold) {
                results.add(new SimilarityResult(candidate, similarity, contentStore.get(candidate)));
            }
        }
        results.sort((a, b) -> Double.compare(b.similarity, a.similarity));
        return results;
    }

    private Set<String> collectCandidates(int[] querySig) {
        Set<String> candidates = new HashSet<>();
        for (int band = 0; band < bands; band++) {
            int start = band * rows;
            int end = Math.min(querySig.length, start + rows);
            int[] slice = Arrays.copyOfRange(querySig, start, end);
            String key = Arrays.toString(slice);
            Set<String> bucket = buckets.get(band).get(key);
            if (bucket != null) {
                candidates.addAll(bucket);
            }
        }
        return candidates;
    }

    public static class SimilarityResult implements Serializable {
        private static final long serialVersionUID = 1L;
        public final String fileName;
        public final double similarity;
        public final String content;

        public SimilarityResult(String fileName, double similarity, String content) {
            this.fileName = fileName;
            this.similarity = similarity;
            this.content = content;
        }
    }
}

