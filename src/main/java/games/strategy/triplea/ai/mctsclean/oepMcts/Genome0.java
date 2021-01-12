package games.strategy.triplea.ai.mctsclean.oepMcts;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.mctsclean.util.ModelMoveOptions;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Genome0 implements Comparable<Genome0>{
  ArrayList<OepAction> actions;
  int visits=0;
  double value=0;


  Genome0(OepWorldModel state){
    this.actions=generateRandomActions(state);

  }

  Genome0(ArrayList<OepAction> actions){
    this.actions=actions;
  }

  public ArrayList<OepAction> generateRandomActions(OepWorldModel state){
    //implementar random actions
    ArrayList<OepAction> actions = new ArrayList<>();
    Random rand = new Random();
    ModelMoveOptions attackOptions;
    if (state.data.getSequence().getStep().getName().endsWith("NonCombatMove")){
      attackOptions=state.getTerritoryManager().getDefenseOptions();
    } else {
      attackOptions=state.getTerritoryManager().getAttackOptions();
    }
    for (Unit u: attackOptions.getUnitMoveMap().keySet()){
      List<Territory> lista = new ArrayList<>(attackOptions.getUnitMoveMap().get(u));
      actions.add(new OepAction(u,lista.get(rand.nextInt(lista.size()))));
    }
    return actions;
  }

  public void executeActions(OepWorldModel newState){
    OepAction.ApplyActionEffects(newState,actions,newState.mctsData,newState.moveDel,newState.data.getSequence().getStep().getPlayerId());
  }

  @Override
  public int compareTo(final Genome0 g) {
    if(value>g.value){
      return 1;
    } else if (value==g.value){
      return 0;
    } else {
      return -1;
    }

  }
}
