package games.strategy.triplea.ai.mctsclean.algorithm;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.mctsclean.util.ForwardModel;
import games.strategy.triplea.ai.mctsclean.util.MctsData;
import games.strategy.triplea.ai.mctsclean.util.ModelTerritory;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;

@Getter
public  class NewAction {
  static boolean intermediate=true;
  public static long totalActionTime=0;
  public static int totalActionExecutions=1;
  protected Territory t;

  public Territory getT() {
    return t;
  }

  public Set<Unit> getUnits() {
    return units;
  }

  protected Set<Unit> units;



  public NewAction(Territory t, Set<Unit> units){
    this.t=t;
    this.units=units;
  }


  public void applyActionEffects(WorldModel state){
    long time=System.currentTimeMillis();
    NewAction a= this.replace(state.mctsData);
    ForwardModel.doMove(state.mctsData.getData(),
        ForwardModel.calculateMoveRoutesNew(state.mctsData, state.data.getSequence().getStep().getPlayerId(), a.t, a.units),
        state.moveDel,
        true);
    totalActionExecutions++;
    totalActionTime+=System.currentTimeMillis()-time;
    state.addExcluded(t);
  }

  public NewAction replace(MctsData data){
    Set<Unit> newUnits=new HashSet<Unit>();
    for (Unit u: units){
      newUnits.add(data.getData().getUnits().get(u.getId()));
    }
    return new NewAction((Territory) data.getData().getUnitHolder(t.getName(),"T"),newUnits);
  }



  @Override
  public String toString(){
    return "Territory : "+t+", units: "+units;
  }
}