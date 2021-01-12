package games.strategy.triplea.ai.mctsclean.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.pro.logging.ProLogger;
import games.strategy.triplea.delegate.Matches;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.type.ArrayType;
import lombok.Getter;
import org.triplea.java.collections.CollectionUtils;

/** Pro AI data. */
@Getter
public final class MctsData {

    public static long mctsDataInitializeTime=0;
    public static int mctsDataInitializeN=0;
    // Default values
    private Map<Unit, Territory> unitTerritoryMap = new HashMap<>();

    private List<Territory> myUnitTerritories = new ArrayList<>();

    private GameData data;

    public MctsData() {}

    public MctsData(GameData data) {
        this.data=data;
    }

    public void initialize(GameData data) {
        hiddenInitialize(data);
    }

    public Territory getUnitTerritory(final Unit unit) {
        return unitTerritoryMap.get(unit);
    }


    private void hiddenInitialize
            (GameData data) {
        long time=System.currentTimeMillis();
        ProLogger.info("Initializing Mcts Data");
        this.data=data;
        unitTerritoryMap=newUnitTerritoryMap(data);
        generateUnitTerritories(data.getSequence().getStep().getPlayerId());
        myUnitTerritories =
                CollectionUtils.getMatches(
                        data.getMap().getTerritories(), Matches.territoryHasUnitsOwnedBy(data.getSequence().getStep().getPlayerId()));
        ProLogger.info("territory number: "+data.getMap().getTerritories().size());
        ProLogger.info("player String: "+data.getSequence().getStep().getPlayerId().toString());
        ProLogger.info("player Territories "+CollectionUtils.getMatches(
            data.getMap().getTerritories(), Matches.territoryHasUnitsOwnedBy(data.getSequence().getStep().getPlayerId())).size());

        mctsDataInitializeTime+=System.currentTimeMillis()-time;
        mctsDataInitializeN++;
    }

    public void Initialize(MctsData data){

    }



    public void updateData(){
        unitTerritoryMap=newUnitTerritoryMap(data);
        generateUnitTerritories(data.getSequence().getStep().getPlayerId());
        myUnitTerritories =
            CollectionUtils.getMatches(
                data.getMap().getTerritories(), Matches.territoryHasUnitsOwnedBy(data.getSequence().getStep().getPlayerId()));

    }

    private static Map<Unit, Territory> newUnitTerritoryMap(final GameData data) {
        final Map<Unit, Territory> unitTerritoryMap = new HashMap<>();
        for (final Territory t : data.getMap().getTerritories()) {
            for (final Unit u : t.getUnits()) {
                unitTerritoryMap.put(u, t);
            }
        }
        return unitTerritoryMap;
    }


    public List<Territory> getMyUnitTerritories(GamePlayer player){
        return CollectionUtils.getMatches(
                data.getMap().getTerritories(), Matches.territoryHasUnitsOwnedBy(player));

    }

    public void generateUnitTerritories(GamePlayer player){
        myUnitTerritories = CollectionUtils.getMatches(data.getMap().getTerritories(), Matches.territoryHasUnitsOwnedBy(player));
    }

}
