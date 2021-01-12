package games.strategy.triplea.ai.mctsclean;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.framework.GameDataUtils;
import games.strategy.triplea.ai.mctsclean.algorithm.*;
import games.strategy.triplea.ai.mctsclean.util.*;
import games.strategy.triplea.ai.pro.logging.ProLogger;
import games.strategy.triplea.ai.weak.WeakAi;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import java.util.ArrayList;
import java.util.logging.Level;
import lombok.Getter;
import org.checkerframework.checker.units.qual.A;

public class AbstractMctsAi extends WeakAi {


    private Mcts mcts;
    @Getter
    public final MctsData mctsData;

    public AbstractMctsAi(final String name, final MctsData mctsData) {
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
        MctsData mctsData= new MctsData(data);
        mctsData.initialize(data);
        WorldModel model=new WorldModel(dataCopy);
        model.generateAlliedTerritories();
        model.generateEnemyTerritories();
        this.mcts=new Mcts(model);
        this.mcts.initializeMCTSearch();
        ProLogger.info("Selection Action");
        ArrayList<NewAction> best=mcts.run();

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

        if (1>0){
            //throw new NullPointerException(best+"");
        }
        ProLogger.info(
            player.getName()
                + " time for "+player.getName()+" "+combat+"Combat="
                + nonCombat
                + " time="
                + (System.currentTimeMillis() - start));

    }

}
