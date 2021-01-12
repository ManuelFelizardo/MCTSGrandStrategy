package games.strategy.triplea.ai.mctsclean.oepMcts;

import games.strategy.triplea.ai.mctsclean.nonExploringMcts.AbstractNonExploringMctsAi;
import games.strategy.triplea.ai.mctsclean.nonExploringMcts.NonExploringMcts;
import games.strategy.triplea.ai.mctsclean.util.ForwardModel;
import games.strategy.triplea.ai.mctsclean.util.MctsData;
import games.strategy.triplea.ai.pro.logging.ProLogger;

public class OepAi extends AbstractOepAi{

  public static AbstractOepAi currentInstance;

  public OepAi(String name) {
    super(name, new MctsData());
    ForwardModel.mctsAi=this;
    ProLogger.info("Starting MCTSAI class");
    currentInstance=this;
  }
}
