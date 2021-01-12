package games.strategy.triplea.ai.mctsclean.nonExploringMcts;

import games.strategy.triplea.ai.mctsclean.MctsAi;
import games.strategy.triplea.ai.mctsclean.util.ForwardModel;
import games.strategy.triplea.ai.mctsclean.util.MctsData;
import games.strategy.triplea.ai.pro.logging.ProLogger;

public class NonExploringMctsAi extends AbstractNonExploringMctsAi{

  public static AbstractNonExploringMctsAi currentInstance;

  public NonExploringMctsAi(String name) {
    super(name, new MctsData());
    NonExploringMcts.runCount=0;
    ForwardModel.mctsAi=this;
    ProLogger.info("Starting MCTSAI class");
    currentInstance=this;
  }
}
