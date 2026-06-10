package cn.edu.whut.sept.zuul.dialogue;

import java.util.List;

public record DialogueChoice(
        String choiceId,
        String text,
        List<DialogueCondition> conditions,
        List<DialogueEffect> effects,
        String nextNodeId
) {
    public DialogueChoice {
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
        effects = effects == null ? List.of() : List.copyOf(effects);
    }
}
