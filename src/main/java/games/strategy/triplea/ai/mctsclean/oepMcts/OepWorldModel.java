package games.strategy.triplea.ai.mctsclean.oepMcts;

import static games.strategy.triplea.ai.mctsclean.util.ModelTerritoryManager.getNeighboringEnemyTerritories;
import static games.strategy.triplea.delegate.Matches.isTerritoryOwnedBy;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.framework.GameDataUtils;
import games.strategy.triplea.ai.mctsclean.algorithm.ActionUtils;
import games.strategy.triplea.ai.mctsclean.algorithm.EndRoundDelegateMcts;
import games.strategy.triplea.ai.mctsclean.util.*;
import games.strategy.triplea.ai.weak.WeakAi;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.triplea.delegate.battle.BattleDelegate;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.triplea.java.collections.IntegerMap;

public class OepWorldModel {

  public static long cloneDataTime=0;
  public static long worldModelCreateTime=0;
  public Collection<GamePlayer> victors= new ArrayList<>();


  //private final WeakAi weakAi;
  boolean isGameOver=false;
  public GameData data;
  public MctsData mctsData;
  public IMoveDelegate moveDel;
  public GamePlayer player;

  ModelTerritoryManager getTerritoryManager() {
    return territoryManager;
  }


  private ModelTerritoryManager territoryManager;
  private Set<Territory> excluded= new HashSet<>();
  List<Territory> alliedTerritories;
  List<Territory> enemyTerritories;
  public int counter=0;

  public OepWorldModel(GameData data){
    this.data=data;
    this.mctsData=new MctsData();
    mctsData.initialize(data);
    this.moveDel= DelegateFinder.moveDelegate(data);
    final IDelegateBridge bridge = new MctsDummyDelegateBridge(ForwardModel.mctsAi, data.getPlayerList().getPlayerId(data.getSequence().getStep().getPlayerId().getName()), data);
    moveDel.setDelegateBridgeAndPlayer(bridge);
    territoryManager=new ModelTerritoryManager(mctsData);
    generateMoves();
    generateAlliedTerritories();
    generateEnemyTerritories();

  }

  public OepWorldModel(ModelTerritoryManager manager, GameData data, MctsData mctsData){
    this.data=data;
    this.mctsData=mctsData;
    this.moveDel= DelegateFinder.moveDelegate(data);
    final IDelegateBridge bridge = new MctsDummyDelegateBridge(ForwardModel.mctsAi, data.getPlayerList().getPlayerId(data.getSequence().getStep().getPlayerId().getName()), data);
    moveDel.setDelegateBridgeAndPlayer(bridge);
    territoryManager=manager;
  }

  public void generateAlliedTerritories(){
    alliedTerritories= data.getMap().getTerritories().stream().filter(isTerritoryOwnedBy(data.getSequence().getStep().getPlayerId())).collect(Collectors.toList());
  }

  public void generateEnemyTerritories(){
    enemyTerritories=getNeighboringEnemyTerritories(alliedTerritories,data.getSequence().getStep().getPlayerId(),data);
  }

  public void setEnemyTerritories(List<Territory> enemyTerritories){
    this.enemyTerritories=enemyTerritories;
  }

  public void setAlliedTerritories(List<Territory> alliedTerritories){
    this.alliedTerritories=alliedTerritories;
  }

  public void generateMoves(){
    territoryManager.populateAttackOptions();
    territoryManager.populateDefenseOptions(new ArrayList<>());
  }

  public static void advancePlayout(OepWorldModel state){
    boolean nonCombat = false;
    if (state.data.getSequence().getStep().getName().endsWith("NonCombatMove")){
      state.data.getSequence().getStep().getDelegate().start();
      state.data.getSequence().getStep().getDelegate().end();
      nonCombat=true;
    }
    //state.data.getSequence().getStep().getDelegate().end();
    GameStep gameStep = ActionUtils.getNextStep(state);
    state.data.getSequence().next();

    long time;

    while(!gameStep.getName().endsWith("NonCombatMove") && !gameStep.getName().endsWith("CombatMove")){


      //ProLogger.info("-------------------------------------->>  stage " + state.data.getSequence().getStep().getName());
      if (gameStep.getName().endsWith("Battle")) {

        final BattleDelegate battleDelegate = DelegateFinder.battleDelegate(state.data);
        final IDelegateBridge bridge2 = new MctsDummyDelegateBridge(OepAi.currentInstance, state.data.getSequence().getStep().getPlayerId(), state.data);
        battleDelegate.setDelegateBridgeAndPlayer(bridge2);

        time=System.currentTimeMillis();
        ActionUtils.simulateBattles(state.data,state.data.getSequence().getStep().getPlayerId(), state.moveDel.getBridge());
        gameStep = ActionUtils.getNextStep(state);
        state.data.getSequence().next();
        continue;
      }
      if (nonCombat) {
        final IDelegateBridge bridge1 = new MctsDummyDelegateBridge(OepAi.currentInstance, state.data.getSequence().getStep().getPlayerId(), state.data);
        state.data.getSequence().getStep().getDelegate().setDelegateBridgeAndPlayer(bridge1);
        state.data.getSequence().getStep().getDelegate().end();
      }

      gameStep = ActionUtils.getNextStep(state);
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



  public void doMoveWeakAi(WeakAi ai){
    boolean nonCombat=data.getSequence().getStep().getName().endsWith("NonCombatMove") ? true : false;
    final IDelegateBridge bridge =new MctsDummyDelegateBridge(ForwardModel.mctsAi, data.getPlayerList().getPlayerId(data.getSequence().getStep().getPlayerId().getName()), data);
    moveDel.setDelegateBridgeAndPlayer(bridge);
    ai.moveSimple(nonCombat,moveDel,data,data.getSequence().getStep().getPlayerId());
  }



  public boolean isTerminal(){
    return isGameOver;
  }

  //implementar
  public OepWorldModel generateChildWorldModel(){
    GameData dataCopy;
    long time=System.currentTimeMillis();
    dataCopy = GameDataUtils.cloneGameDataWithoutHistoryFast(data, true);
    cloneDataTime+=System.currentTimeMillis()-time;
    time=System.currentTimeMillis();
    OepWorldModel newModel =new OepWorldModel(territoryManager,dataCopy, mctsData);
    newModel.setAlliedTerritories(alliedTerritories);
    newModel.setEnemyTerritories(enemyTerritories);
    worldModelCreateTime=System.currentTimeMillis()-time;
    return newModel;
  }

  public double getWorldHeuristic(GamePlayer player){
    int unitN=0;
    IntegerMap<UnitType> map= data.getPlayerList().getPlayerId(player.getName()).getUnitCollection().getUnitsByType();
        /*
        for (UnitType t:map.keySet()){
            t.getClass().getAnnotations()
            unitN=data.getSequence().getStep().getPlayerId().getUnitCollection().getUnitsByType()

        }
         */
    unitN=data.getSequence().getStep().getPlayerId().getUnitCollection().size();
    return unitN;
  }


}