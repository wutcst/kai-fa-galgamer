package cn.edu.whut.sept.zuul.dialogue;

import cn.edu.whut.sept.zuul.model.GamePhase;

public class DialogueSession {

    private final String groupId;
    private final GamePhase suspendedPhase;
    private String currentNodeId;
    private boolean active;

    public DialogueSession(String groupId, String currentNodeId, GamePhase suspendedPhase) {
        this.groupId = groupId;
        this.currentNodeId = currentNodeId;
        this.suspendedPhase = suspendedPhase;
        this.active = true;
    }

    public String groupId() {
        return groupId;
    }

    public String currentNodeId() {
        return currentNodeId;
    }

    public void currentNodeId(String currentNodeId) {
        this.currentNodeId = currentNodeId;
    }

    public GamePhase suspendedPhase() {
        return suspendedPhase;
    }

    public boolean active() {
        return active;
    }

    public void close() {
        active = false;
    }
}
