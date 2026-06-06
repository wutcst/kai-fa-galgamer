package cn.edu.whut.sept.zuul.minigame;

import java.util.HashMap;
import java.util.Map;

public class MiniGameSession {

    private final String sessionId;
    private final String gameId;
    private final String eventId;
    private String phase = "PLAYING";
    private final Map<String, Object> state = new HashMap<>();
    private MiniGameResult result;

    public MiniGameSession(String sessionId, String gameId, String eventId) {
        this.sessionId = sessionId;
        this.gameId = gameId;
        this.eventId = eventId;
    }

    public String sessionId() {
        return sessionId;
    }

    public String gameId() {
        return gameId;
    }

    public String eventId() {
        return eventId;
    }

    public String phase() {
        return phase;
    }

    public void phase(String phase) {
        this.phase = phase;
    }

    public Map<String, Object> state() {
        return state;
    }

    public MiniGameResult result() {
        return result;
    }

    public void finish(MiniGameResult result) {
        this.result = result;
        this.phase = "FINISHED";
    }

    public boolean finished() {
        return result != null;
    }
}
