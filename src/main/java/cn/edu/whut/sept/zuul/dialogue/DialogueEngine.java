package cn.edu.whut.sept.zuul.dialogue;

import cn.edu.whut.sept.zuul.model.GamePhase;
import cn.edu.whut.sept.zuul.model.GameSnapshot;
import cn.edu.whut.sept.zuul.service.PlayerService;
import cn.edu.whut.sept.zuul.state.WorldState;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DialogueEngine {

    public static final String EXIT = "EXIT";

    private final DialogueConditionEvaluator conditionEvaluator;
    private final DialogueEffectExecutor effectExecutor;
    private final Map<String, DialogueGraph> graphs = new LinkedHashMap<>();
    private DialogueSession session;

    public DialogueEngine(DialogueConditionEvaluator conditionEvaluator, DialogueEffectExecutor effectExecutor) {
        this.conditionEvaluator = conditionEvaluator;
        this.effectExecutor = effectExecutor;
        registerDefaults();
    }

    public synchronized boolean hasActiveSession() {
        return session != null && session.active();
    }

    public synchronized GamePhase suspendedPhase() {
        return session == null ? GamePhase.EXPLORING : session.suspendedPhase();
    }

    public synchronized void reset() {
        session = null;
    }

    public synchronized void registerGraph(String groupId, DialogueGraph graph) {
        graphs.put(groupId, graph);
    }

    public synchronized boolean canStart(String groupId, WorldState worldState) {
        return !hasActiveSession()
                && graphs.containsKey(groupId)
                && !worldState.getBoolean(completedFlag(groupId));
    }

    public synchronized void start(String groupId, GamePhase suspendedPhase) {
        DialogueGraph graph = graphs.get(groupId);
        if (graph == null || graph.startNode() == null) {
            throw new IllegalArgumentException("未知对话图：" + groupId);
        }
        session = new DialogueSession(groupId, graph.startNodeId(), suspendedPhase);
    }

    public synchronized GamePhase advance(PlayerService playerService, WorldState worldState) {
        if (!hasActiveSession()) {
            return GamePhase.EXPLORING;
        }
        DialogueGraph graph = activeGraph();
        DialogueNode node = graph.node(session.currentNodeId());
        if (node == null) {
            return close(worldState);
        }
        if ("CHOICE".equalsIgnoreCase(node.type())) {
            return session.suspendedPhase();
        }
        if ("EVENT_TRIGGER".equalsIgnoreCase(node.type())) {
            effectExecutor.executeEvent(node, playerService, worldState);
        }
        return jump(node.nextNodeId(), worldState);
    }

    public synchronized GamePhase choose(String choiceId, PlayerService playerService, WorldState worldState) {
        if (!hasActiveSession()) {
            return GamePhase.EXPLORING;
        }
        DialogueNode node = activeGraph().node(session.currentNodeId());
        if (node == null || !"CHOICE".equalsIgnoreCase(node.type())) {
            return session.suspendedPhase();
        }
        DialogueChoice choice = node.choices().stream()
                .filter(item -> item.choiceId().equals(choiceId))
                .findFirst()
                .orElse(null);
        if (choice == null) {
            return session.suspendedPhase();
        }
        DialogueConditionEvaluator.Evaluation evaluation = conditionEvaluator.evaluate(choice.conditions(), playerService, worldState);
        if (!evaluation.available()) {
            return session.suspendedPhase();
        }
        effectExecutor.execute(choice.effects(), playerService, worldState);
        return jump(choice.nextNodeId(), worldState);
    }

    public synchronized GameSnapshot.DialogueView view(PlayerService playerService, WorldState worldState) {
        if (!hasActiveSession()) {
            return null;
        }
        DialogueGraph graph = activeGraph();
        DialogueNode node = graph.node(session.currentNodeId());
        if (node == null) {
            return null;
        }
        String speakerSide = normalizeSpeaker(node.speakerSide());
        DialogueCharacter left = node.leftCharacter() == null ? new DialogueCharacter("player", "default") : node.leftCharacter();
        DialogueCharacter right = node.rightCharacter() == null ? new DialogueCharacter("silhouette_fallback", "default") : node.rightCharacter();
        List<GameSnapshot.DialogueChoiceView> choices = "CHOICE".equalsIgnoreCase(node.type())
                ? node.choices().stream()
                .map(choice -> choiceView(choice, playerService, worldState))
                .toList()
                : List.of();

        return new GameSnapshot.DialogueView(
                true,
                "DUAL_SIDE",
                graph.dialogueGroupId(),
                node.nodeId(),
                node.type(),
                dialogueText(node),
                speakerSide,
                node.speakerName(),
                node.audioSfx(),
                characterView(left, "LEFT".equals(speakerSide)),
                characterView(right, "RIGHT".equals(speakerSide)),
                choices
        );
    }

    private GameSnapshot.DialogueChoiceView choiceView(DialogueChoice choice, PlayerService playerService, WorldState worldState) {
        DialogueConditionEvaluator.Evaluation evaluation = conditionEvaluator.evaluate(choice.conditions(), playerService, worldState);
        return new GameSnapshot.DialogueChoiceView(
                choice.choiceId(),
                choice.text(),
                evaluation.available(),
                evaluation.confidence(),
                evaluation.lockedReason(),
                choice.nextNodeId()
        );
    }

    private GameSnapshot.DialogueCharacterView characterView(DialogueCharacter character, boolean speaking) {
        return new GameSnapshot.DialogueCharacterView(
                character.id(),
                character.assetKey(),
                character.expression(),
                speaking
        );
    }

    private String dialogueText(DialogueNode node) {
        if ("EVENT_TRIGGER".equalsIgnoreCase(node.type()) && node.dialogueLog() != null && !node.dialogueLog().isBlank()) {
            return node.dialogueLog();
        }
        return node.text();
    }

    private GamePhase jump(String nextNodeId, WorldState worldState) {
        if (nextNodeId == null || nextNodeId.isBlank() || EXIT.equals(nextNodeId)) {
            return close(worldState);
        }
        session.currentNodeId(nextNodeId);
        return session.suspendedPhase();
    }

    private GamePhase close(WorldState worldState) {
        GamePhase phase = session.suspendedPhase();
        worldState.setBoolean(completedFlag(session.groupId()), true);
        session.close();
        session = null;
        return phase;
    }

    private DialogueGraph activeGraph() {
        return graphs.get(session.groupId());
    }

    private String completedFlag(String groupId) {
        return "dialogue_completed." + groupId;
    }

    private String normalizeSpeaker(String speakerSide) {
        if ("LEFT".equalsIgnoreCase(speakerSide)) {
            return "LEFT";
        }
        if ("RIGHT".equalsIgnoreCase(speakerSide)) {
            return "RIGHT";
        }
        return "NARRATOR";
    }

    private void registerDefaults() {
        Map<String, DialogueNode> nodes = new LinkedHashMap<>();
        nodes.put("node_01", new DialogueNode(
                "node_01", "SPEECH", "RIGHT", "失落学者", "alert",
                "你身上带着不属于这个时空的残渣……那是命运核心的粉尘。你究竟是谁？",
                "sfx_dusty_wind",
                new DialogueCharacter("player", "alert"),
                new DialogueCharacter("scholar", "alert"),
                null, null, null, "node_02", List.of()
        ));
        nodes.put("node_02", new DialogueNode(
                "node_02", "CHOICE", null, null, null, null, null,
                new DialogueCharacter("player", "alert"),
                new DialogueCharacter("scholar", "alert"),
                null, null, null, null,
                List.of(
                        new DialogueChoice(
                                "traveler",
                                "“我只是一个试图打破这永恒折磨的旅人。”",
                                List.of(),
                                List.of(new DialogueEffect("GAIN_HP", null, null, null, 5, null)),
                                "node_choice_a_speech"
                        ),
                        new DialogueChoice(
                                "threat",
                                "“交出你守护的秘密，否则用我的剑来问路。”",
                                List.of(),
                                List.of(new DialogueEffect("LOSE_HP", null, null, null, 10, null)),
                                "node_choice_b_speech"
                        ),
                        new DialogueChoice(
                                "badge_truth",
                                "【展示徽章】“命运已向我昭示真相，放手吧。”",
                                List.of(new DialogueCondition("HAS_ITEM", "nameless_badge", null, null, null)),
                                List.of(),
                                "node_choice_c_perfect"
                        )
                )
        ));
        nodes.put("node_choice_a_speech", new DialogueNode(
                "node_choice_a_speech", "SPEECH", "RIGHT", "失落学者", "default",
                "旅人？那就记住，命运不会怜悯只会逃跑的人。", null,
                new DialogueCharacter("player", "default"),
                new DialogueCharacter("scholar", "default"),
                null, null, null, EXIT, List.of()
        ));
        nodes.put("node_choice_b_speech", new DialogueNode(
                "node_choice_b_speech", "SPEECH", "RIGHT", "失落学者", "angry",
                "鲁莽会留下伤口，而伤口会替命运记账。", null,
                new DialogueCharacter("player", "angry"),
                new DialogueCharacter("scholar", "angry"),
                null, null, null, EXIT, List.of()
        ));
        nodes.put("node_choice_c_perfect", new DialogueNode(
                "node_choice_c_perfect", "EVENT_TRIGGER", "RIGHT", "失落学者", "calm",
                null, null,
                new DialogueCharacter("player", "calm"),
                new DialogueCharacter("scholar", "calm"),
                "BYPASS_PUZZLE",
                "triple-seal-gate-unlock",
                "学者叹了口气，无名徽章散发出的微光拂去了他脸上的戾气。封印门缓缓开启……",
                EXIT,
                List.of()
        ));
        graphs.put("dial_fate_hall_meeting", new DialogueGraph("dial_fate_hall_meeting", "node_01", nodes));
    }
}
