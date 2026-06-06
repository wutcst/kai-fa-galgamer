package cn.edu.whut.sept.zuul.save;

public class EndingSaveData {

    private String endingId;
    private String title;
    private String text;
    private String assetKey;
    private boolean creatorModeUnlocked;

    public String getEndingId() {
        return endingId;
    }

    public void setEndingId(String endingId) {
        this.endingId = endingId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getAssetKey() {
        return assetKey;
    }

    public void setAssetKey(String assetKey) {
        this.assetKey = assetKey;
    }

    public boolean isCreatorModeUnlocked() {
        return creatorModeUnlocked;
    }

    public void setCreatorModeUnlocked(boolean creatorModeUnlocked) {
        this.creatorModeUnlocked = creatorModeUnlocked;
    }
}
