package cn.edu.whut.sept.zuul.dialogue;

public record DialogueEffect(
        String type,
        String itemKey,
        String flagKey,
        Boolean booleanValue,
        Integer value,
        String eventPayload
) {
}
