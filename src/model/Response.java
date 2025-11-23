package model;

import java.util.List;

public class Response {
    private String requestId;
    private String status;
    private List<MatchResult> matches;
    private String errorMessage;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<MatchResult> getMatches() {
        return matches;
    }

    public void setMatches(List<MatchResult> matches) {
        this.matches = matches;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}

