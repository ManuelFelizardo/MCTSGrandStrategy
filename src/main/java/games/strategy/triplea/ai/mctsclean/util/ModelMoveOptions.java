package games.strategy.triplea.ai.mctsclean.util;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;

/** The result of an AI movement analysis for its own possible moves. */
@Getter
public class ModelMoveOptions {

    private final Map<Territory, ModelTerritory> territoryMap;
    //mudar isto
    private Map<Unit, Set<Territory>> unitMoveMap;
    private final Map<Unit, Set<Territory>> transportMoveMap;
    private final Map<Unit, Set<Territory>> bombardMap;
    private final List<ModelTransport> transportList;
    private final Map<Unit, Set<Territory>> bomberMoveMap;

    ModelMoveOptions() {
        territoryMap = new HashMap<>();
        unitMoveMap = new HashMap<>();
        transportMoveMap = new HashMap<>();
        bombardMap = new HashMap<>();
        transportList = new ArrayList<>();
        bomberMoveMap = new HashMap<>();
    }

    ModelMoveOptions(final ModelMoveOptions myMoveOptions, final MctsData mctsData) {
        this();
        for (final Territory t : myMoveOptions.territoryMap.keySet()) {
            territoryMap.put(t, new ModelTerritory(myMoveOptions.territoryMap.get(t), mctsData));
        }
        unitMoveMap.putAll(myMoveOptions.unitMoveMap);
        transportMoveMap.putAll(myMoveOptions.transportMoveMap);
        bombardMap.putAll(myMoveOptions.bombardMap);
        transportList.addAll(myMoveOptions.transportList);
        bomberMoveMap.putAll(myMoveOptions.bomberMoveMap);
    }

    public void sortOptions(MctsData mctsData, GamePlayer player){
        //unitMoveMap=ForwardModel.sortUnitNeededOptionsThenAttack(mctsData,player,unitMoveMap,territoryMap);
    }
}
