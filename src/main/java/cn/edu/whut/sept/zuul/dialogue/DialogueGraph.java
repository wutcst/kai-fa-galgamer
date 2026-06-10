package cn.edu.whut.sept.zuul.dialogue;

import java.util.Map;

public record DialogueGraph(
        String dialogueGroupId,
        String startNodeId,
        Map<String, DialogueNode> nodes
) {
    public DialogueGraph {
        nodes = nodes == null ? Map.of() : Map.copyOf(nodes);
    }

    public DialogueNode startNode() {
        return nodes.get(startNodeId);
    }

    public DialogueNode node(String nodeId) {
        return nodes.get(nodeId);
    }
}
