package games.strategy.triplea.ai.mctsclean.nonExploringMcts;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.mctsclean.algorithm.ActionUtils;
import games.strategy.triplea.ai.mctsclean.algorithm.NewAction;
import games.strategy.triplea.ai.mctsclean.algorithm.Reward;
import games.strategy.triplea.ai.mctsclean.algorithm.WorldModel;
import games.strategy.triplea.ai.weak.WeakAi;
import games.strategy.triplea.delegate.Matches;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import org.triplea.java.collections.CollectionUtils;

public class NonExploringMctsRandomPlayout extends NonExploringMcts{
  public NonExploringMctsRandomPlayout(final WorldModel worldModel) {
    super(worldModel);
  }
  @Override
  protected Reward Playout(WorldModel initialPlayoutState)
  {
    ArrayList<NewAction> executableActions;
    int randomIndex;
    long time=System.currentTimeMillis();
    WorldModel state = initialPlayoutState.generateChildWorldModel();
    generateWorldTime+=System.currentTimeMillis()-time;
    int i=0;

    WeakAi.ss="";
    while(!state.isTerminal() && i<40)
    {
      i++;
      pi=i;
      time=System.currentTimeMillis();
      state.generateRandomActions(state);
      doMoveTime+=System.currentTimeMillis()-time;
      time=System.currentTimeMillis();
      ActionUtils.advancePlayout(state);
      advanceTime+=System.currentTimeMillis()-time;
    }

    if (state.isTerminal()){
      if (state.victors.contains(initialNode.state.data.getSequence().getStep().getPlayerId())){
        return new Reward(0, 0.6+0.4*((40-i)/40.0));
      }
      //throw new NullPointerException(WeakAi.ss);
      return new Reward(0, -0.6-0.4*((40-i)/40.0));
    }

    time=System.currentTimeMillis();
    //float territorySize=state.data.getMap().getTerritoriesOwnedBy(initialNode.state.data.getSequence().getStep().getPlayerId()).size();
    //float totalTerritorySize=state.data.getMap().getTerritories().size();
    Set<Unit> set = new HashSet<>();
    for (Territory t:state.data.getMap()){
      set.addAll(t.getUnits());
    }
    float unitSize= CollectionUtils.getMatches(set, Matches.unitOwnedBy(initialNode.state.data.getSequence().getStep().getPlayerId())).size();
    float enemyUnitSize=set.size()-unitSize;
    countUnitsTime+=System.currentTimeMillis()-time;


    return new Reward(0,  (unitSize-enemyUnitSize)/(set.size() * 2.5));
  }
}
