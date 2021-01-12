package games.strategy.triplea.ai.mctsclean.bridgeBurningMcts;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.framework.GameDataUtils;
import games.strategy.triplea.ai.mctsclean.algorithm.NewAction;
import games.strategy.triplea.ai.mctsclean.algorithm.WorldModel;
import games.strategy.triplea.ai.mctsclean.bridgeBurningMcts.BridgeBurningMcts;
import games.strategy.triplea.ai.mctsclean.util.ForwardModel;
import games.strategy.triplea.ai.mctsclean.util.MctsData;
import games.strategy.triplea.ai.pro.logging.ProLogger;
import games.strategy.triplea.ai.weak.WeakAi;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import java.util.ArrayList;
import java.util.logging.Level;
import lombok.Getter;

public class AbstractBridgeBurningMctsAi extends WeakAi {

  private BridgeBurningMcts mcts;
  @Getter
  public final MctsData mctsData;

  public AbstractBridgeBurningMctsAi(final String name, final MctsData mctsData) {
    super(name);
    ProLogger.info("Starting ABSTRACTMCTSAI class");
    this.mctsData=mctsData;
  }


  @Override
  protected void move(
      final boolean nonCombat,
      final IMoveDelegate moveDel,
      final GameData data,
      final GamePlayer player) {

    WorldModel.cloneDataTime=0;
    super.stopGame(); // absolutely MUST call super.stopGame() first
    ProLogger.info("Starting Move phase");
    final GameData dataCopy;
    try {
      data.acquireWriteLock();
      dataCopy = GameDataUtils.cloneGameDataWithoutHistory(data, true);
    } catch (final Throwable t) {
      ProLogger.log(Level.WARNING, "Error trying to clone game data for simulating phases", t);
      throw new NullPointerException();
      //return;
    } finally {
      data.releaseWriteLock();
    }
    dataCopy.isSimulation=true;
    MctsData mctsData= new MctsData(data);
    mctsData.initialize(data);
    WorldModel model=new WorldModel(dataCopy);
    model.generateAlliedTerritories();
    model.generateEnemyTerritories();
    this.mcts=new BridgeBurningMcts(model);
    this.mcts.initializeMCTSearch();
    ProLogger.info("Selection Action");
    ArrayList<NewAction> best=mcts.run();

    if (best == null){
      return;
    }
    if (1>0){
      ProLogger.info(best.size()+"");
      for (NewAction a:best){
        ProLogger.info(a.toString());
      }
    }


    if (best==null){
      if (best==null) {
        throw new NullPointerException();
      }
    }

    final long start = System.currentTimeMillis();

    ArrayList<NewAction> bestR=new ArrayList<>();
    for (NewAction a:best){
      bestR.add(a.replace(mctsData));
    }
    String combat = (data.getSequence().getStep().getName().endsWith("NonCombatMove") ? "Non" : "");
    ForwardModel.doMove(data,ForwardModel.calculateMoveRoutes(mctsData, player, bestR),moveDel, false);

    ProLogger.info(data.getSequence().getStep().getName());

    ProLogger.info(
        player.getName()
            + " time for "+player.getName()+" "+combat+"Combat="
            + nonCombat
            + " time="
            + (System.currentTimeMillis() - start));

  }

}
