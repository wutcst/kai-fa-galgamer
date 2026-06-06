package cn.edu.whut.sept.zuul.save;

import cn.edu.whut.sept.zuul.model.GamePhase;

import java.util.List;
import java.util.Map;

public interface SaveStateAccess {

    String currentRoomId();

    int playerHp();

    List<String> inventoryItems();

    Map<String, Boolean> flags();

    Map<String, Integer> counters();

    GamePhase phaseForSave();

    BossSaveData bossForSave();

    EndingSaveData endingForSave();

    boolean saveBlocked();

    void restoreWorldState(Map<String, Boolean> flags, Map<String, Integer> counters);

    void restoreSpatialContext(String currentRoomId);

    void restorePlayer(int hp, List<String> inventoryItems);

    void restoreTransientState(BossSaveData bossState, EndingSaveData endingState);

    void restorePhase(GamePhase phase);
}
