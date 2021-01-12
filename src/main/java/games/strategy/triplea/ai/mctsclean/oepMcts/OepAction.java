package games.strategy.triplea.ai.mctsclean.oepMcts;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MoveDescription;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.mctsclean.util.ForwardModel;
import games.strategy.triplea.ai.mctsclean.util.MctsData;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import java.util.ArrayList;

public class OepAction {
  public Unit getU() {
    return u;
  }

  private Unit u;

  public Territory getT() {
    return t;
  }

  private Territory t;
  private MoveDescription move;

  public OepAction(Unit u, Territory t){
    this.u=u;
    this.t=t;

  }

  public static void ApplyActionEffects(OepWorldModel state, ArrayList<OepAction> actions, MctsData mctsData, IMoveDelegate moveDel, GamePlayer player){
    ForwardModel.doMove(state.data,ForwardModel.calculateMoveRoutesOep(mctsData, player,state.data, actions),moveDel, true);

  }

  public OepAction replace(GameData data){
    return new OepAction(data.getUnits().get(u.getId()),(Territory) data.getUnitHolder(t.getName(),"T"));
  }

  @Override
  public String toString(){
    return "OEPAction u - "+u.toString()+"; t - "+t.toString()+";";
  }

}
