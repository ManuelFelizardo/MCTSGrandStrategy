package games.strategy.triplea.ai.mctsclean.algorithm;

import static games.strategy.triplea.ai.mctsclean.util.ModelTerritoryManager.*;
import static games.strategy.triplea.delegate.Matches.isTerritoryOwnedBy;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.framework.GameDataUtils;
import games.strategy.triplea.ai.AiUtils;
import games.strategy.triplea.ai.mctsclean.oepMcts.OepAction2;
import games.strategy.triplea.ai.mctsclean.oepMcts.OepWorldModel;
import games.strategy.triplea.ai.mctsclean.util.*;
import games.strategy.triplea.ai.pro.logging.ProLogger;
import games.strategy.triplea.ai.pro.util.ProMatches;
import games.strategy.triplea.ai.weak.WeakAi;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.triplea.java.collections.IntegerMap;

public class WorldModel {

    public static long cloneDataTime=0;
    public static long worldModelCreateTime=0;


    //private final WeakAi weakAi;
    boolean isGameOver=false;
    public GameData data;
    public MctsData mctsData;
    IMoveDelegate moveDel;
    public GamePlayer player;
    private ModelTerritoryManager territoryManager;
    private Set<Territory> excluded= new HashSet<>();
    List<Territory> alliedTerritories;
    List<Territory> enemyTerritories;
    public int counter=0;
    ArrayList<NewAction> actions;
    int actionIndex=0;
    public static int cloneDataN=0;
    public Collection<GamePlayer> victors= new ArrayList<>();

    public WorldModel(GameData data){
        this.data=data;
        this.mctsData=new MctsData();
        mctsData.initialize(data);
        this.moveDel= DelegateFinder.moveDelegate(data);
        final IDelegateBridge bridge = new MctsDummyDelegateBridge(ForwardModel.mctsAi, data.getPlayerList().getPlayerId(data.getSequence().getStep().getPlayerId().getName()), data);
        moveDel.setDelegateBridgeAndPlayer(bridge);
    }

    public void addExcluded(Territory t){
        excluded.add(t);
    }

    public void addExcluded(Set<Territory> t){
        excluded.addAll(t);
    }

    public void resetExcluded(){
        this.excluded=new HashSet<>();
    }

    //implementar
    public boolean isTerminal(){
        return isGameOver;
    }

    public void doMoveWeakAi(WeakAi ai){
        boolean nonCombat=data.getSequence().getStep().getName().endsWith("NonCombatMove") ? true : false;
        final IDelegateBridge bridge =new MctsDummyDelegateBridge(ForwardModel.mctsAi, data.getPlayerList().getPlayerId(data.getSequence().getStep().getPlayerId().getName()), data);
        moveDel.setDelegateBridgeAndPlayer(bridge);
        ai.moveSimple(nonCombat,moveDel,data,data.getSequence().getStep().getPlayerId());
    }

    public void setEnemyTerritories(List<Territory> enemyTerritories){
        this.enemyTerritories=enemyTerritories;
    }

    public void setAlliedTerritories(List<Territory> alliedTerritories){
        this.alliedTerritories=alliedTerritories;
    }

    public void generateAlliedTerritories(){
        alliedTerritories= data.getMap().getTerritories().stream().filter(isTerritoryOwnedBy(data.getSequence().getStep().getPlayerId())).collect(Collectors.toList());
    }

    public void generateEnemyTerritories(){
        enemyTerritories=getNeighboringEnemyTerritories(alliedTerritories,data.getSequence().getStep().getPlayerId(),data);
    }

    public int getActionsSize(){

        int actionSize=0;
        String stepName=data.getSequence().getStep().getName();
        if (stepName.endsWith("NonCombatMove")){
            Territory t;
            for (Territory t2:alliedTerritories){
                t=(Territory) data.getUnitHolder(t2.getName(),"T");
                if (!excluded.contains(t) ) {
                    actionSize++;
                }
            }

        } else if (stepName.endsWith("CombatMove") && !stepName.endsWith("AirborneCombatMove")) {
            Set<Unit> units;
            Territory t;
            for (Territory t2:enemyTerritories){
                t=(Territory) data.getUnitHolder(t2.getName(),"T");
                if (!excluded.contains(t)) {
                    units = findLandMoveOptionsSingular(mctsData, data.getSequence().getStep().getPlayerId(), mctsData.getMyUnitTerritories(), new ArrayList<>(), new ArrayList<>(), true, false, t);
                    if (canAttackSuccessfully(units,t)){
                        actionSize++;
                    }

                }
            }
        }
        return actionSize;
    }


    public void generateActionsNE(){
        mctsData.updateData();
        actions=new ArrayList<>();

        String stepName=data.getSequence().getStep().getName();
        if (stepName.endsWith("NonCombatMove")){
            Territory t;
            for (Territory t2:alliedTerritories){
                t=(Territory) data.getUnitHolder(t2.getName(),"T");
                if (!excluded.contains(t) ) {
                    List<Set<Unit>> unitSet = findLandMoveOptionsSingularSet(mctsData, data.getSequence().getStep().getPlayerId(), mctsData.getMyUnitTerritories(), new ArrayList<>(), new ArrayList<>(), false, false, t);
                    unitSet=generateCombinations(unitSet);
                    for(Set<Unit> units:unitSet){
                        actions.add(new NewAction(t,units));
                    }
                    actions.add(new NewActionSkip(t, new HashSet()));
                    return;
                }
            }
            actions.add(new NewActionAdvance());

        } else if (stepName.endsWith("CombatMove") && !stepName.endsWith("AirborneCombatMove")) {
            Set<Unit> units;
            Territory t;
            for (Territory t2:enemyTerritories){
                t=(Territory) data.getUnitHolder(t2.getName(),"T");
                if (!excluded.contains(t)) {
                    units = findLandMoveOptionsSingular(mctsData, data.getSequence().getStep().getPlayerId(), mctsData.getMyUnitTerritories(), new ArrayList<>(), new ArrayList<>(), true, false, t);
                    if (canAttackSuccessfully(units,t)){
                        List<Set<Unit>> sets=getUnitsUpToStrengthSetNE(AiUtils.strength(t.getUnits(), false, false),new ArrayList<>(units));
                        for (Set<Unit> unitSet:sets){
                            actions.add(new NewAction(t,unitSet));
                        }
                        actions.add(new NewActionSkip(t, new HashSet()));
                        return;
                    } else {
                        excluded.add(t);
                        continue;
                    }

                }
            }
            actions.add(new NewActionAdvance());
        }

    }

    public void generateRandomActions(WorldModel state){
        List<NewAction> newActions= new ArrayList<>();
        Random rand = new Random();
        String stepName=state.data.getSequence().getStep().getName();
        if (stepName.endsWith("NonCombatMove")){
            for (Territory t2:state.alliedTerritories){
                //meter as unidades usadas na função moveoptionsingular set

                List<Set<Unit>> unitSet = findLandMoveOptionsSingularSet(state.mctsData, state.data.getSequence().getStep().getPlayerId(), state.mctsData.getMyUnitTerritories(), new ArrayList<>(), new ArrayList<>(), false, false, t2);
                unitSet=generateCombinations(unitSet);
                Set<Unit> units;
                unitSet.add(new HashSet<Unit>());
                units=unitSet.get(rand.nextInt(unitSet.size()));
                NewAction a= new NewAction(t2,units);
                a.applyActionEffects(state);
            }

        } else if (stepName.endsWith("CombatMove") && !stepName.endsWith("AirborneCombatMove")) {
            Set<Unit> units;
            for (Territory t2:state.enemyTerritories){

                //meter as unidades usadas na função moveoptionsingular set

                units = findLandMoveOptionsSingular(state.mctsData, state.data.getSequence().getStep().getPlayerId(), state.mctsData.getMyUnitTerritories(), new ArrayList<>(), new ArrayList<>(), true, false, t2);
                if (canAttackSuccessfully(units,t2)){
                    List<Set<Unit>> sets=getUnitsUpToStrengthSetNE(AiUtils.strength(t2.getUnits(), false, false),new ArrayList<>(units));
                    sets.add(new HashSet<Unit>());
                    Set<Unit> units2=sets.get(rand.nextInt(sets.size()));
                    NewAction a= new NewAction(t2,units2);
                    a.applyActionEffects(state);
                }
            }

        }
    }



    public void generateActions(){
        mctsData.updateData();
        actions=new ArrayList<>();

        String stepName=data.getSequence().getStep().getName();
        if (stepName.endsWith("NonCombatMove")){
            Territory t;
            for (Territory t2:alliedTerritories){
                t=(Territory) data.getUnitHolder(t2.getName(),"T");
                if (!excluded.contains(t) ) {
                    List<Set<Unit>> unitSet = findLandMoveOptionsSingularSet(mctsData, data.getSequence().getStep().getPlayerId(), mctsData.getMyUnitTerritories(), new ArrayList<>(), new ArrayList<>(), false, false, t);
                    unitSet=generateCombinations(unitSet);
                    for(Set<Unit> units:unitSet){
                        actions.add(new NewAction(t,units));
                    }
                    actions.add(new NewActionSkip(t, new HashSet()));
                    return;
                }
            }
            actions.add(new NewActionAdvance());

        } else if (stepName.endsWith("CombatMove") && !stepName.endsWith("AirborneCombatMove")) {
            Set<Unit> units;
            Territory t;
            for (Territory t2:enemyTerritories){
                t=(Territory) data.getUnitHolder(t2.getName(),"T");
                if (!excluded.contains(t)) {
                    units = findLandMoveOptionsSingular(mctsData, data.getSequence().getStep().getPlayerId(), mctsData.getMyUnitTerritories(), new ArrayList<>(), new ArrayList<>(), true, false, t);
                    if (canAttackSuccessfully(units,t)){
                        List<Set<Unit>> sets=getUnitsUpToStrengthSet(AiUtils.strength(t.getUnits(), false, false),units);
                        for (Set<Unit> unitSet:sets){
                            actions.add(new NewAction(t,unitSet));
                        }
                        actions.add(new NewActionSkip(t, new HashSet()));
                        return;
                    } else {
                        excluded.add(t);
                        continue;
                    }

                }
            }
            actions.add(new NewActionAdvance());
        }

    }

    public List<Set<Unit>> generateCombinations(List<Set<Unit>> sets){
        List<Set<Unit>>  combinations = new ArrayList<>();
        int r=sets.size();
        int[] combination;
        int n= sets.size();
        while (r>0) {

            combination= new int[r];
            // initialize with lowest lexicographic combination
            for (int i = 0; i < r; i++) {
                combination[i] = i;
            }

            while (combination[r - 1] < n) {
                Set<Unit> newSet= new HashSet<>();
                for (int i:combination){
                    newSet.addAll(sets.get(i));
                }
                combinations.add(newSet);

                // generate next combination in lexicographic order
                int t = r - 1;
                while (t != 0 && combination[t] == n - r + t) {
                    t--;
                }
                combination[t]++;
                for (int i = t + 1; i < r; i++) {
                    combination[i] = combination[i - 1] + 1;
                }
            }
            r--;
        }

        return combinations;
    }


    static List<Set<Unit>> getUnitsUpToStrengthSetNE( double maxStrength, final List<Unit> units) {

        List<Set<Unit>> sets = new ArrayList<Set<Unit>>();
        double str=1.1;
        Set<Unit> unitsUpToStrength = new HashSet<>();
        Set<Unit> newSet = new HashSet<>();
        boolean full=false;
        int shuffleCount=0;
        while(sets.size()<20 && shuffleCount<10 && newSet.size()!=units.size()) {
            unitsUpToStrength = new HashSet<>();
            for (final Unit u : units) {
                unitsUpToStrength.add(u);
                full = false;
                double ourStrength = AiUtils.strength(unitsUpToStrength, true, false);
                if (ourStrength > str * maxStrength && ourStrength < (2.5 * maxStrength) + 4) {
                    full = true;
                    newSet = new HashSet<>();
                    newSet.addAll(unitsUpToStrength);
                    sets.add(newSet);
                    str += 0.2;
                }
                if (sets.size()>=20 || ourStrength >= (2.5 * maxStrength) + 4){
                    break;
                }
            }
            Collections.shuffle(units);
            shuffleCount++;
        }

        if (!full){
            newSet = new HashSet<>();
            newSet.addAll(unitsUpToStrength);
            sets.add(newSet);
        }
        Collections.reverse(sets);
        return sets;
    }

    static List<Set<Unit>> getUnitsUpToStrengthSet( double maxStrength, final Collection<Unit> units) {
        List<Set<Unit>> sets = new ArrayList<Set<Unit>>();
        double str=1.1;
        final Set<Unit> unitsUpToStrength = new HashSet<>();
        Set<Unit> newSet = new HashSet<>();
        boolean full=false;
        for (final Unit u : units) {
            unitsUpToStrength.add(u);
            full=false;
            float ourStrength = AiUtils.strength(unitsUpToStrength, true, false);
            if (ourStrength > str*maxStrength && ourStrength < ((3.5 * maxStrength) + 4)) {
                full=true;
                newSet = new HashSet<>();
                newSet.addAll(unitsUpToStrength);
                sets.add(newSet);
                str+=0.1;
            }
        }

        if (!full){
            newSet = new HashSet<>();
            newSet.addAll(unitsUpToStrength);
            sets.add(newSet);
        }
        Collections.reverse(sets);
        return sets;
    }


    //implementar
    public ArrayList<NewAction> getExecutableActions(){
        return actions;
    }

    public NewAction getNextAction(){
        if (actionIndex<actions.size()){
            return actions.get(actionIndex++);
        }
        else {
            return null;
        }
    }


    //implementar
    public WorldModel generateChildWorldModel(){
        GameData dataCopy;
        long time=System.currentTimeMillis();
        dataCopy = GameDataUtils.cloneGameDataWithoutHistoryFast(data, true);
        cloneDataTime+=System.currentTimeMillis()-time;
        cloneDataN++;
        time=System.currentTimeMillis();
        WorldModel newModel =new WorldModel(dataCopy);
        worldModelCreateTime=System.currentTimeMillis()-time;
        newModel.addExcluded(excluded);
        newModel.setAlliedTerritories(alliedTerritories);
        newModel.setEnemyTerritories(enemyTerritories);
        return newModel;
    }


}