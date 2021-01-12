package games.strategy.triplea.ai.mctsclean.oepMcts;

import static games.strategy.triplea.ai.mctsclean.util.ModelTerritoryManager.canAttackSuccessfully;
import static games.strategy.triplea.ai.mctsclean.util.ModelTerritoryManager.findLandMoveOptionsSingular;
import static games.strategy.triplea.ai.mctsclean.util.ModelTerritoryManager.findLandMoveOptionsSingularSet;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.AiUtils;
import games.strategy.triplea.ai.mctsclean.algorithm.NewAction;
import games.strategy.triplea.ai.mctsclean.algorithm.NewActionAdvance;
import games.strategy.triplea.ai.mctsclean.algorithm.NewActionSkip;
import games.strategy.triplea.ai.mctsclean.util.ModelMoveOptions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.triplea.java.collections.CollectionUtils;

public class Genome implements Comparable<Genome>{
    //ArrayList<OepAction2> actions=new ArrayList<>();
    HashMap<Territory,OepAction2> actions=new HashMap<>();
    int visits=0;
    double value=0;
    Set<Unit> usedUnits=new HashSet<>();


    Genome(OepWorldModel state){
      generateRandomActions(state);
    }

    Genome(){

    }

    Genome(Genome genome){

      //this.actions.addAll(genome.actions);
      //this.usedUnits.addAll(genome.usedUnits);

    }

    Genome(HashMap<Territory,OepAction2> actions){
      this.actions=actions;
    }

    public boolean addActions(OepAction2 action){
      if (usedUnits==null){
        throw new NullPointerException();
      }else if (action==null){
        throw new NullPointerException();
      }else if (action.units==null){
        throw new NullPointerException();
      }
      if (!CollectionUtils.intersection(usedUnits, action.units).isEmpty()){
        return false;
      }
      actions.put(action.getT(),action);
      usedUnits.addAll(action.units);
      return true;
    }

    public Set<Unit> randomSubset(Set<Unit> units){
      return null;
    }

    public void generateRandomActions(OepWorldModel state){
      //implementar random actions
      Random rand = new Random();
      String stepName=state.data.getSequence().getStep().getName();
      if (stepName.endsWith("NonCombatMove")){
        for (Territory t2:state.alliedTerritories){
          //meter as unidades usadas na função moveoptionsingular set

          List<Set<Unit>> unitSet = findLandMoveOptionsSingularSet(state.mctsData, state.data.getSequence().getStep().getPlayerId(), state.mctsData.getMyUnitTerritories(), new ArrayList<>(), new ArrayList<>(), false, false, t2, usedUnits);
          unitSet=generateCombinations(unitSet);
          Set<Unit> units;
          if (unitSet.size()>0){
            unitSet.add(new HashSet<Unit>());
            units=unitSet.get(rand.nextInt(unitSet.size()));
          }else {
            units= new HashSet<Unit>();
          }
          actions.put(t2,new OepAction2(t2,units));
          usedUnits.addAll(units);
        }

      } else if (stepName.endsWith("CombatMove") && !stepName.endsWith("AirborneCombatMove")) {
        Set<Unit> units;
        for (Territory t2:state.enemyTerritories){

          //meter as unidades usadas na função moveoptionsingular set

          units = findLandMoveOptionsSingular(state.mctsData, state.data.getSequence().getStep().getPlayerId(), state.mctsData.getMyUnitTerritories(), new ArrayList<>(), new ArrayList<>(), true, false, t2, usedUnits);
          if (canAttackSuccessfully(units,t2)){
            List<Set<Unit>> sets=getUnitsUpToStrengthSetNE(AiUtils.strength(t2.getUnits(), false, false),new ArrayList<>(units));
            sets.add(new HashSet<Unit>());
            Set<Unit> units2=sets.get(rand.nextInt(sets.size()));
            actions.put(t2,new OepAction2(t2, units2));
            usedUnits.addAll(units2);
          } else {
            actions.put(t2,new OepAction2(t2, new HashSet<>()));
          }
        }

      }
    }

  static List<Set<Unit>> getUnitsUpToStrengthSet( double maxStrength, final List<Unit> units) {

    List<Set<Unit>> sets = new ArrayList<Set<Unit>>();
    double str=1.1;
    final Set<Unit> unitsUpToStrength = new HashSet<>();
    Set<Unit> newSet = new HashSet<>();
    boolean full=false;
    int shuffleCount=0;
    while(sets.size()<10 && shuffleCount<5 && newSet.size()!=units.size()) {

      for (final Unit u : units) {
        unitsUpToStrength.add(u);
        full = false;
        double ourStrength = AiUtils.strength(unitsUpToStrength, true, false);
        if (ourStrength > str * maxStrength && ourStrength < (2.5 * maxStrength) + 4) {
          full = true;
          newSet = new HashSet<>();
          newSet.addAll(unitsUpToStrength);
          sets.add(newSet);
          str += 0.1;
        }
        if (sets.size()>=10 || ourStrength >= (2.5 * maxStrength) + 4){
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
  static List<Set<Unit>> getUnitsUpToStrengthSetNE( double maxStrength, final Collection<Unit> unitsCollection) {
    List<Set<Unit>> sets = new ArrayList<Set<Unit>>();
    ArrayList<Unit> units=new ArrayList<>(unitsCollection);
    double str=1.1;
    final Set<Unit> unitsUpToStrength = new HashSet<>();
    Set<Unit> newSet = new HashSet<>();
    boolean full=false;
    Collections.shuffle(units);
    for (final Unit u : units) {
      unitsUpToStrength.add(u);
      full=false;
      double ourStrength = AiUtils.strength(unitsUpToStrength, true, false);
      if (ourStrength > str * maxStrength && ourStrength < (3.5 * maxStrength) + 4) {
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
    return sets;
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


    public void executeActions(OepWorldModel newState){
      for (OepAction2 a:actions.values()){
        a.applyActionEffects(newState);
      }
    }

    public void randomActionForTerritory(Territory t2, OepWorldModel state){

      Random rand = new Random();
      String stepName=state.data.getSequence().getStep().getName();

      if (stepName.endsWith("NonCombatMove")){
        //meter as unidades usadas na função moveoptionsingular set

        List<Set<Unit>> unitSet = findLandMoveOptionsSingularSet(state.mctsData, state.data.getSequence().getStep().getPlayerId(), state.mctsData.getMyUnitTerritories(), new ArrayList<>(), new ArrayList<>(), false, false, t2, usedUnits);
        unitSet=generateCombinations(unitSet);
        Set<Unit> units;
        if (unitSet.size()>0){
          unitSet.add(new HashSet<>());
          units=unitSet.get(rand.nextInt(unitSet.size()));
        }else {
          units= new HashSet<>();
        }
        actions.put(t2,new OepAction2(t2,units));
        usedUnits.addAll(units);

      } else if (stepName.endsWith("CombatMove") && !stepName.endsWith("AirborneCombatMove")) {
        Set<Unit> units;

        //meter as unidades usadas na função moveoptionsingular set

        units = findLandMoveOptionsSingular(state.mctsData, state.data.getSequence().getStep().getPlayerId(), state.mctsData.getMyUnitTerritories(), new ArrayList<>(), new ArrayList<>(), true, false, t2, usedUnits);
        if (canAttackSuccessfully(units,t2)){
          List<Set<Unit>> sets=getUnitsUpToStrengthSetNE(AiUtils.strength(t2.getUnits(), false, false),new ArrayList<>(units));
          sets.add(new HashSet<>());
          Set<Unit> units2=sets.get(rand.nextInt(sets.size()));
          actions.put(t2, new OepAction2(t2, units2));
          usedUnits.addAll(units2);

        } else {
          actions.put(t2,new OepAction2(t2,new HashSet<>()));
        }
      }



    }


    @Override public String toString(){
      return (value+" - "+ actions.toString()+" - "+usedUnits.toString()+"\n\n\n");
    }


    @Override
    public int compareTo(final Genome g) {
      if(value>g.value){
        return 1;
      } else if (value==g.value){
        return 0;
      } else {
        return -1;
      }

    }
}
