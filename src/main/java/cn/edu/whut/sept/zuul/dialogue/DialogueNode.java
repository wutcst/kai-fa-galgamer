package cn.edu.whut.sept.zuul.dialogue;

import java.util.List;

public record DialogueNode(
        String nodeId,
        String type,
        String speakerSide,
        String speakerName,
        String expressionKey,
        String text,
        String audioSfx,
        DialogueCharacter leftCharacter,
        DialogueCharacter rightCharacter,
        String eventType,
        String eventPayload,
        String dialogueLog,
        String nextNodeId,
        List<DialogueChoice> choices
) {
    public DialogueNode {
        choices = choices == null ? List.of() : List.copyOf(choices);
    }
}
