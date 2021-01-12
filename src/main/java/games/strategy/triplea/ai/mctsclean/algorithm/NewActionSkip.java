package games.strategy.triplea.ai.mctsclean.algorithm;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.mctsclean.util.ForwardModel;
import java.util.Set;

public class NewActionSkip extends NewAction {
  public NewActionSkip(final Territory t, final Set<Unit> units) {
    super(t, units);
  }


  @Override
  public void applyActionEffects(WorldModel state){
    state.addExcluded(t);
  }



  @Override
  public String toString(){
    return "Skip Territory : "+t;
  }


}
