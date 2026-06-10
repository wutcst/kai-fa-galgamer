package cn.edu.whut.sept.zuul.dialogue;

public record DialogueCondition(
        String type,
        String itemKey,
        String flagKey,
        Boolean expected,
        Integer value
) {
}
