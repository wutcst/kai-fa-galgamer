package cn.edu.whut.sept.zuul.save;

import java.util.HashSet;
import java.util.Set;

public class ProfileState {

    private int schemaVersion = 1;
    private boolean creatorModeUnlocked;
    private boolean cycleBroken;
    private Set<String> completedEndings = new HashSet<>();

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public boolean isCreatorModeUnlocked() {
        return creatorModeUnlocked;
    }

    public void setCreatorModeUnlocked(boolean creatorModeUnlocked) {
        this.creatorModeUnlocked = creatorModeUnlocked;
    }

    public boolean isCycleBroken() {
        return cycleBroken;
    }

    public void setCycleBroken(boolean cycleBroken) {
        this.cycleBroken = cycleBroken;
    }

    public Set<String> getCompletedEndings() {
        return completedEndings;
    }

    public void setCompletedEndings(Set<String> completedEndings) {
        this.completedEndings = completedEndings == null ? new HashSet<>() : new HashSet<>(completedEndings);
    }
}
