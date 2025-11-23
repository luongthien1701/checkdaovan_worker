package model;

public class MatchResult {
    private String fileName;
    private double similarity;
    private String description;

    public MatchResult() {
    }

    public MatchResult(String fileName, double similarity, String description) {
        this.fileName = fileName;
        this.similarity = similarity;
        this.description = description;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(double similarity) {
        this.similarity = similarity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

