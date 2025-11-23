package service;

import model.MatchResult;
import model.Request;
import model.Response;
import util.LSHIndex;
import util.MinHash;
import util.ShingleGenerator;
import util.TextPreprocessor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlagiarismDetector {
    private static final int SHINGLE_SIZE = 3;
    private static final int HASH_FUNCTIONS = 300;
    private static final int BANDS = 150;
    private static final int ROWS = 2;
    private static final double MIN_SIMILARITY = 0.01;

    private final LSHIndex index;
    private final ShingleGenerator shingleGenerator;

    public PlagiarismDetector(Path dataDir) {
        MinHash minHash = new MinHash(HASH_FUNCTIONS);
        this.index = new LSHIndex(BANDS, ROWS, minHash);
        this.shingleGenerator = new ShingleGenerator(SHINGLE_SIZE);
        loadCorpus(dataDir);
    }

    private void loadCorpus(Path dataDir) {
        if (!Files.exists(dataDir)) {
            System.err.println("Data directory not found: " + dataDir.toAbsolutePath());
            return;
        }
        try {
            Files.list(dataDir)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".txt"))
                    .forEach(this::addDocument);
        } catch (IOException e) {
            System.err.println("Failed to load corpus: " + e.getMessage());
        }
    }

    private void addDocument(Path path) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            String normalized = TextPreprocessor.normalize(content);
            Set<String> shingles = shingleGenerator.shingles(normalized);
            index.add(path.getFileName().toString(), normalized, shingles);
            System.out.println("Indexed " + path.getFileName());
        } catch (IOException e) {
            System.err.println("Failed to read " + path + ": " + e.getMessage());
        }
    }

    public Response handle(Request request) {
        try {
            String normalized = TextPreprocessor.normalize(request.getFullText());
            Set<String> queryShingles = shingleGenerator.shingles(normalized);
            if (queryShingles.isEmpty()) {
                return errorResponse(request.getRequestId(), "Document empty after preprocessing");
            }
            List<LSHIndex.SimilarityResult> similar = index.query(queryShingles, MIN_SIMILARITY);
            List<MatchResult> matchResults = new ArrayList<>();
            for (LSHIndex.SimilarityResult result : similar) {
                Set<String> candidate = new HashSet<>(shingleGenerator.shingles(result.content));
                double jaccard = ShingleGenerator.jaccard(queryShingles, candidate);
                String description = String.format("Similarity: %.2f%%", jaccard * 100);
                System.out.println(description);
                matchResults.add(new MatchResult(result.fileName, jaccard, description));
            }
            Response response = new Response();
            response.setRequestId(request.getRequestId());
            response.setStatus("SUCCESS");
            response.setMatches(matchResults);
            return response;
        } catch (Exception e) {
            return errorResponse(request.getRequestId(), "Internal error: " + e.getMessage());
        }
    }

    private Response errorResponse(String requestId, String message) {
        Response response = new Response();
        response.setRequestId(requestId);
        response.setStatus("ERROR");
        response.setErrorMessage(message);
        return response;
    }
}

