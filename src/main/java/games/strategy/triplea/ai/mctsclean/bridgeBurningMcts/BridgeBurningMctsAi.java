package games.strategy.triplea.ai.mctsclean.bridgeBurningMcts;

import games.strategy.triplea.ai.mctsclean.nonExploringMcts.AbstractNonExploringMctsAi;
import games.strategy.triplea.ai.mctsclean.nonExploringMcts.NonExploringMcts;
import games.strategy.triplea.ai.mctsclean.util.ForwardModel;
import games.strategy.triplea.ai.mctsclean.util.MctsData;
import games.strategy.triplea.ai.pro.logging.ProLogger;

public class BridgeBurningMctsAi extends AbstractBridgeBurningMctsAi {

  public static BridgeBurningMctsAi currentInstance;

  public BridgeBurningMctsAi(String name) {
    super(name, new MctsData());
    NonExploringMcts.runCount=0;
    ForwardModel.mctsAi=this;
    ProLogger.info("Starting MCTSAI class");
    currentInstance=this;
  }
}
