package games.strategy.triplea.ai.mctsclean.algorithm;
import static games.strategy.triplea.ai.mctsclean.util.ModelTerritoryManager.getNeighboringEnemyTerritories;
import static games.strategy.triplea.delegate.Matches.isTerritoryOwnedBy;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.MoveDescription;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.ai.fast.FastOddsEstimator;
import games.strategy.triplea.ai.mctsclean.MctsAi;
import games.strategy.triplea.ai.mctsclean.oepMcts.OepWorldModel;
import games.strategy.triplea.ai.mctsclean.util.ForwardModel;
import games.strategy.triplea.ai.mctsclean.util.MctsData;
import games.strategy.triplea.ai.mctsclean.util.MctsDummyDelegateBridge;
import games.strategy.triplea.ai.mctsclean.util.ModelTerritory;
import games.strategy.triplea.ai.pro.ProData;
import games.strategy.triplea.ai.pro.data.ProBattleResult;
import games.strategy.triplea.ai.pro.logging.ProLogger;
import games.strategy.triplea.ai.pro.simulate.ProSimulateTurnUtils;
import games.strategy.triplea.ai.pro.util.ProOddsCalculator;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.triplea.delegate.EndRoundDelegate;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleDelegate;
import games.strategy.triplea.delegate.battle.BattleTracker;
import games.strategy.triplea.delegate.battle.IBattle;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import org.triplea.java.collections.CollectionUtils;
import games.strategy.triplea.ai.weak.WeakAi;

@Getter
public  class ActionUtils {

  static boolean intermediate = false;
  static long totalActionTime=0;
  static int totalActionExecutions=0;

  static long battleTime=0;
  static long placeTime=0;
  static long bidTime=0;
  static long purchaseTime=0;
  static int battleExecutions=0;
  static int placeExecutions=0;
  static int bidExecutions=0;
  static int purchaseExecutions=0;

  public static GameStep getNextStep(WorldModel state){
    if (state.data.getSequence().getStepIndex()+1>=state.data.getSequence().size()){
      return state.data.getSequence().getStep(state.data.getSequence().getStepIndex()+1-state.data.getSequence().size());
    }
    return state.data.getSequence().getStep(state.data.getSequence().getStepIndex()+1);
  }

  public static GameStep getNextStep(OepWorldModel state){
    if (state.data.getSequence().getStepIndex()+1>=state.data.getSequence().size()){
      return state.data.getSequence().getStep(state.data.getSequence().getStepIndex()+1-state.data.getSequence().size());
    }
    return state.data.getSequence().getStep(state.data.getSequence().getStepIndex()+1);
  }


  public static void advance(WorldModel state){
    boolean nonCombat = false;
    if (state.data.getSequence().getStep().getName().endsWith("NonCombatMove")){
      state.data.getSequence().getStep().getDelegate().start();
      state.data.getSequence().getStep().getDelegate().end();
      nonCombat=true;
    }
    GameStep gameStep =getNextStep(state);
    state.data.getSequence().next();

    long time;

    while(!gameStep.getName().endsWith("NonCombatMove") && !gameStep.getName().endsWith("CombatMove")){


      //ProLogger.info("-------------------------------------->>  stage " + state.data.getSequence().getStep().getName());
      if (gameStep.getName().endsWith("Battle")) {

        final BattleDelegate battleDelegate = DelegateFinder.battleDelegate(state.data);
        final IDelegateBridge bridge2 = new MctsDummyDelegateBridge(MctsAi.currentInstance, state.data.getSequence().getStep().getPlayerId(), state.data);
        battleDelegate.setDelegateBridgeAndPlayer(bridge2);

        battleExecutions++;
        time=System.currentTimeMillis();
        simulateBattles(state.data,state.data.getSequence().getStep().getPlayerId(), state.moveDel.getBridge());
        battleTime+=System.currentTimeMillis()-time;
        gameStep = getNextStep(state);
        state.data.getSequence().next();
        continue;
      }
      if (nonCombat) {
        final IDelegateBridge bridge1 = new MctsDummyDelegateBridge(MctsAi.currentInstance, state.data.getSequence().getStep().getPlayerId(), state.data);
        state.data.getSequence().getStep().getDelegate().setDelegateBridgeAndPlayer(bridge1);
        state.data.getSequence().getStep().getDelegate().end();
      }

      gameStep = getNextStep(state);
      state.data.getSequence().next();
    }
    final IDelegateBridge bridge = new games.strategy.triplea.ai.mctsclean.util.MctsDummyDelegateBridge(ForwardModel.mctsAi, state.data.getPlayerList().getPlayerId(state.data.getSequence().getStep().getPlayerId().getName()), state.data);
    state.moveDel.setDelegateBridgeAndPlayer(bridge);
    if(state.data.getSequence().getStepIndex()==6){

      final EndRoundDelegateMcts delegate = new EndRoundDelegateMcts();
      //(EndRoundDelegateMcts) state.data.getDelegate("endRound");
      delegate.setDelegateBridgeAndPlayer(state.moveDel.getBridge());
      delegate.start();
      state.isGameOver=delegate.gameOver;
      state.victors=delegate.getWinners();
      //throw new NullPointerException(state.data.getSequence().getStep().getName());
    }
    state.resetExcluded();
    state.generateAlliedTerritories();
    state.generateEnemyTerritories();

  }

  public static void advancePlayout(WorldModel state){
    boolean nonCombat = false;
    if (state.data.getSequence().getStep().getName().endsWith("NonCombatMove")){
      state.data.getSequence().getStep().getDelegate().start();
      state.data.getSequence().getStep().getDelegate().end();
      nonCombat=true;
    }
    //state.data.getSequence().getStep().getDelegate().end();
    GameStep gameStep =getNextStep(state);
    state.data.getSequence().next();

    long time;

    while(!gameStep.getName().endsWith("NonCombatMove") && !gameStep.getName().endsWith("CombatMove")){


      //ProLogger.info("-------------------------------------->>  stage " + state.data.getSequence().getStep().getName());
      if (gameStep.getName().endsWith("Battle")) {

        final BattleDelegate battleDelegate = DelegateFinder.battleDelegate(state.data);
        final IDelegateBridge bridge2 = new MctsDummyDelegateBridge(MctsAi.currentInstance, state.data.getSequence().getStep().getPlayerId(), state.data);
        battleDelegate.setDelegateBridgeAndPlayer(bridge2);

        battleExecutions++;
        time=System.currentTimeMillis();
        simulateBattles(state.data,state.data.getSequence().getStep().getPlayerId(), state.moveDel.getBridge());
        battleTime+=System.currentTimeMillis()-time;
        gameStep = getNextStep(state);
        state.data.getSequence().next();
        continue;
      }
      if (nonCombat) {
        final IDelegateBridge bridge1 = new MctsDummyDelegateBridge(MctsAi.currentInstance, state.data.getSequence().getStep().getPlayerId(), state.data);
        state.data.getSequence().getStep().getDelegate().setDelegateBridgeAndPlayer(bridge1);
        state.data.getSequence().getStep().getDelegate().end();
      }

      gameStep = getNextStep(state);
      state.data.getSequence().next();
    }

    final IDelegateBridge bridge = new games.strategy.triplea.ai.mctsclean.util.MctsDummyDelegateBridge(ForwardModel.mctsAi, state.data.getSequence().getStep().getPlayerId(), state.data);
    state.moveDel.setDelegateBridgeAndPlayer(bridge);
    if(state.data.getSequence().getStepIndex()==6){

      final EndRoundDelegateMcts delegate = new EndRoundDelegateMcts();
      //(EndRoundDelegateMcts) state.data.getDelegate("endRound");
      delegate.setDelegateBridgeAndPlayer(state.moveDel.getBridge());
      delegate.start();
      state.isGameOver=delegate.gameOver;
      state.victors=delegate.getWinners();
    }

  }



  public static void simulateBattles(
      final GameData data,
      final GamePlayer player,
      final IDelegateBridge delegateBridge) {
    ProData proData= new ProData();
    proData.hiddenInitialize(data, player, true);
    //ProLogger.info("Starting battle simulation phase");
    final ProOddsCalculator calc= new ProOddsCalculator(new FastOddsEstimator(proData));

    final BattleDelegate battleDelegate = DelegateFinder.battleDelegate(data);
    final Map<IBattle.BattleType, Collection<Territory>> battleTerritories =
        battleDelegate.getBattles().getBattles();
    for (final Map.Entry<IBattle.BattleType, Collection<Territory>> entry : battleTerritories.entrySet()) {
      for (final Territory t : entry.getValue()) {
        final IBattle battle =
            battleDelegate
                .getBattleTracker()
                .getPendingBattle(t, entry.getKey().isBombingRun(), entry.getKey());
        final Collection<Unit> attackers = new ArrayList<>(battle.getAttackingUnits());
        attackers.retainAll(t.getUnits());
        final Collection<Unit> defenders = new ArrayList<>(battle.getDefendingUnits());
        defenders.retainAll(t.getUnits());
        final Collection<Unit> bombardingUnits = battle.getBombardingUnits();
        boolean lost=false;
        if (!defenders.isEmpty()){
          final ProBattleResult result =
              calc.callBattleCalc(proData, t, attackers, defenders, bombardingUnits);
          final Collection<Unit> remainingAttackers = result.getAverageAttackersRemaining();
          final Collection<Unit> remainingDefenders = result.getAverageDefendersRemaining();
          final List<Unit> attackersToRemove = new ArrayList<>(attackers);
          attackersToRemove.removeAll(remainingAttackers);

          final List<Unit> defendersToRemove =
              CollectionUtils.getMatches(defenders, Matches.unitIsInfrastructure().negate());
          defendersToRemove.removeAll(remainingDefenders);
          final List<Unit> infrastructureToChangeOwner =
              CollectionUtils.getMatches(defenders, Matches.unitIsInfrastructure());
          final Change attackersKilledChange = ChangeFactory.removeUnits(t, attackersToRemove);
          delegateBridge.addChange(attackersKilledChange);
          final Change defendersKilledChange = ChangeFactory.removeUnits(t, defendersToRemove);
          delegateBridge.addChange(defendersKilledChange);
          if ((remainingAttackers.size()==0 && remainingDefenders.size()!=0)){
            lost=true;
          }
        }

        if (!lost){
          BattleTracker.captureOrDestroyUnits(t, player, player, delegateBridge, null);
          if (!ProSimulateTurnUtils.checkIfCapturedTerritoryIsAlliedCapital(t, data, player, delegateBridge)) {
            delegateBridge.addChange(ChangeFactory.changeOwner(t, player));
          }
          battleDelegate.getBattleTracker().getConquered().add(t);

        }
        battleDelegate.getBattleTracker().removeBattle(battle, data);
        final Territory updatedTerritory = data.getMap().getTerritory(t.getName());
      }
    }
  }

  @Override
  public String toString() {
    return "action "; //+move.toString();
  }
}