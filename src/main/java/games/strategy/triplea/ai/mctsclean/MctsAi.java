package games.strategy.triplea.ai.mctsclean;

import games.strategy.triplea.ai.mctsclean.util.MctsData;
import games.strategy.triplea.ai.mctsclean.util.ForwardModel;
import games.strategy.triplea.ai.pro.logging.ProLogger;

public class MctsAi extends AbstractMctsAi {
    public static MctsAi currentInstance;

    public MctsAi(String name) {

        super(name, new MctsData());
        ForwardModel.mctsAi=this;
        ProLogger.info("Starting MCTSAI class");
        currentInstance=this;
    }
}
