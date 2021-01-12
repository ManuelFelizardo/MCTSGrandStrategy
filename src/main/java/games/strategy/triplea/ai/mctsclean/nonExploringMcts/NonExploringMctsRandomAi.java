package games.strategy.triplea.ai.mctsclean.nonExploringMcts;

import games.strategy.triplea.ai.mctsclean.util.ForwardModel;
import games.strategy.triplea.ai.mctsclean.util.MctsData;
import games.strategy.triplea.ai.pro.logging.ProLogger;

public class NonExploringMctsRandomAi extends AbstractNonExploringMctsRandomAi {
  public static AbstractNonExploringMctsRandomAi currentInstance;

  public NonExploringMctsRandomAi(String name) {
    super(name, new MctsData());
    NonExploringMcts.runCount = 0;
    ForwardModel.mctsAi = this;
    ProLogger.info("Starting MCTSAI class");
    currentInstance = this;
  }
}
