package games.strategy.triplea.ai.mctsclean.oepMcts;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.mctsclean.util.ForwardModel;
import java.util.HashSet;
import java.util.Set;

public class OepAction2 {
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



  public OepAction2(Territory t, Set<Unit> units){
    this.t=t;
    this.units=units;
  }


  public void applyActionEffects(OepWorldModel state){
    long time=System.currentTimeMillis();
    OepAction2 a= this.replace(state.data);
    ForwardModel.doMove(state.data,
        ForwardModel.calculateMoveRoutesNew2(state.data,state.mctsData, state.data.getSequence().getStep().getPlayerId(), a.t, a.units),
        state.moveDel,
        true);
    totalActionExecutions++;
    totalActionTime+=System.currentTimeMillis()-time;
  }

  public OepAction2 replace(GameData data){
    Set<Unit> newUnits=new HashSet<Unit>();
    for (Unit u: units){
      newUnits.add(data.getUnits().get(u.getId()));
    }
    return new OepAction2((Territory) data.getUnitHolder(t.getName(),"T"),newUnits);
  }



  @Override
  public String toString(){
    return "Territory : "+t+", units: "+units;
  }

}