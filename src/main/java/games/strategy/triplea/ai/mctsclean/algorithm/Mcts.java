package games.strategy.triplea.ai.mctsclean.algorithm;

import games.strategy.triplea.ai.pro.logging.ProLogger;
import games.strategy.triplea.delegate.Matches;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Random;
import org.triplea.java.collections.CollectionUtils;

public class Mcts{
  public static final float C = 1.4f;
  public boolean InProgress;
  public int MaxIterations ;
  public int MaxIterationsProcessedPerFrame ;
  public int MaxPlayoutDepthReached ;
  public int MaxSelectionDepthReached ;
  public float TotalProcessingTime ;
  public MCTSNode BestFirstChild ;
  public ArrayList<NewAction> BestActionSequence ;
  public static int runCount=0;
  public static int iterations=0;



  protected int CurrentIterations ;
  protected int CurrentIterationsInFrame ;
  protected int CurrentDepth ;

  protected WorldModel worldModel ;
  protected MCTSNode initialNode ;
  protected Random RandomGenerator ;


  long totalSelectionTime= 0;
  long totalExpandTime= 0;
  long totalPlayoutTime= 0;
  long totalBackpropagateTime= 0;

  long generateActionsTime=0;
  long applyActionsEffectsTime=0;
  long generateWorldTime=0;
  long countUnitsTime=0;



  public Mcts(WorldModel worldModel){
    this.InProgress = false;
    this.worldModel = worldModel;
    this.MaxIterations = 150;
    this.MaxIterationsProcessedPerFrame = 150;
    this.RandomGenerator = new Random();
  }


  public void initializeMCTSearch()
  {
    this.MaxPlayoutDepthReached = 0;
    this.MaxSelectionDepthReached = 0;
    this.CurrentIterations = 0;
    this.CurrentIterationsInFrame = 0;
    this.TotalProcessingTime = 0.0f;
    worldModel.generateActions();
    this.initialNode = new MCTSNode(this.worldModel, this.worldModel.player, null);
    this.InProgress = true;
    this.BestFirstChild = null;
    this.BestActionSequence = new ArrayList<NewAction>();
  }

  public ArrayList<NewAction> run()
  {
    runCount++;
    iterations=0;
    MCTSNode selectedNode;
    Reward reward;

    this.CurrentIterationsInFrame = 0;

    while(this.CurrentIterationsInFrame < this.MaxIterationsProcessedPerFrame && this.CurrentIterations < this.MaxIterations)
    {
      this.CurrentDepth = 0;
      iterations++;
      //selection and expansion
      long time=System.currentTimeMillis();
      selectedNode = this.Selection(this.initialNode);
      totalSelectionTime+=System.currentTimeMillis()-time;

      //debug info
      if(this.CurrentDepth > this.MaxSelectionDepthReached)
      {
        this.MaxSelectionDepthReached = this.CurrentDepth;
      }

      time=System.currentTimeMillis();
      reward = this.Playout(selectedNode.state);
      totalPlayoutTime+=System.currentTimeMillis()-time;

      if(this.CurrentDepth > this.MaxPlayoutDepthReached)
      {
        this.MaxPlayoutDepthReached = this.CurrentDepth;
      }
      time=System.currentTimeMillis();
      this.Backpropagate(selectedNode, reward);
      totalBackpropagateTime+=System.currentTimeMillis()-time;

      this.CurrentIterationsInFrame++;
      this.CurrentIterations++;
    }


    if(this.CurrentIterations >= this.MaxIterations-1)
    {
      this.InProgress = false;
      ProLogger.info("mcts end return action");
      printStructure(initialNode,runCount);
      return this.BestFinalActions(this.initialNode);
    }
    else
    {
      ProLogger.info("mcts end return null");
      throw new NullPointerException();
    }
  }

  protected MCTSNode Selection(MCTSNode initialNode)
  {
    NewAction nextAction;
    MCTSNode currentNode = initialNode;
    MCTSNode bestChild;

    int i=0;
    while(!currentNode.state.isTerminal() && i++<100)
    {

      this.CurrentDepth++;
      nextAction = currentNode.state.getNextAction();

      if(nextAction != null )
      {
        ProLogger.info("------------------------------------> expanded new node");
        return this.Expand(currentNode, nextAction);

      }
      else
      {
        bestChild = this.bestUCTChild(currentNode);
        if (bestChild == null)
        {
          ProLogger.info("------------------------------------> returned final tree node");
          return currentNode;

        }
        else
        {
          ProLogger.info("------------------------------------> selected new tree node");
          currentNode = bestChild;
          ProLogger.info(currentNode.action.toString());
        }
      }
    }

    return currentNode;
  }

  protected Reward Playout(WorldModel initialPlayoutState)
  {
    ArrayList<NewAction> executableActions;
    int randomIndex;
    long time=System.currentTimeMillis();
    WorldModel state = initialPlayoutState.generateChildWorldModel();
    generateWorldTime+=System.currentTimeMillis()-time;
    int i=0;
    while(!state.isTerminal() && i<30)
    {
      ProLogger.info("iteration "+i);
      time=System.currentTimeMillis();
      state.generateActions();
      generateActionsTime+=System.currentTimeMillis()-time;
      i++;
      executableActions = state.getExecutableActions();
      if(executableActions.size() > 0)
      {
        this.CurrentDepth++;
        randomIndex = this.RandomGenerator.nextInt(executableActions.size());
        time=System.currentTimeMillis();
        executableActions.get(randomIndex).applyActionEffects(state);
        applyActionsEffectsTime+=System.currentTimeMillis()-time;
      }
      else
      {
        ActionUtils.advance(state);
        ProLogger.info("advance");
        //break;
      }
    }

    time=System.currentTimeMillis();
    float unitSize=CollectionUtils.getMatches(state.data.getUnits().getUnits(), Matches.unitOwnedBy(state.data.getSequence().getStep().getPlayerId())).size();
    float enemyUnitSize=state.data.getUnits().getUnits().size()-unitSize;
    countUnitsTime+=System.currentTimeMillis()-time;
    if (unitSize>enemyUnitSize){
      return new Reward(0, 1);
    }
    return new Reward(0, -1);
  }


  protected void Backpropagate(MCTSNode node, Reward reward)
  {
    ProLogger.info("backpropagate");
    while(node != null)
    {
      ProLogger.info("iterated over "+node.action);
      node.N++;
      node.Q += reward.getRewardForNode(node);
      ProLogger.info("new n - "+node.N);
      node = node.parent;
    }
  }

  protected MCTSNode Expand(MCTSNode parent, NewAction action)
  {

    long time=System.currentTimeMillis();
    WorldModel newState = parent.state.generateChildWorldModel();
    action.applyActionEffects(newState);
    newState.generateActions();

    var child = new MCTSNode(newState, newState.player, action);
    child.parent=parent;

    parent.ChildNodes.add(child);
    ProLogger.info("added child Node");
    totalExpandTime+=System.currentTimeMillis()-time;
    return child;
  }

  protected MCTSNode bestUCTChild(MCTSNode node)
  {
    float UCTValue;
    float bestUCT = -10000000000f;
    MCTSNode bestNode = null;
    for(MCTSNode child : node.ChildNodes)
    {

      if (child==null){
        throw new NullPointerException();
      }
      if (child.parent==null){
        throw new NullPointerException();
      }
      UCTValue = (child.Q / child.N) + Mcts.C * (float)Math.sqrt(Math.log(child.parent.N) / child.N);
      if (UCTValue > bestUCT)
      {
        ProLogger.info("new best");
        bestUCT = UCTValue;
        bestNode = child;
      }

      ProLogger.info("parent N " + child.parent.N);
      //ProLogger.info("child Q " + child.Q);
      ProLogger.info("child N " + child.N);
      //ProLogger.info("uct value - "+UCTValue);
      //ProLogger.info("best uct - "+bestUCT);

    }

    return bestNode;
  }

  //this method is very similar to the bestUCTChild, but it is used to return the final action of the MCTS search, and so we do not care about
  //the exploration factor
  protected MCTSNode BestChild(MCTSNode node)
  {
    float averageQ;
    float bestAverageQ = Integer.MIN_VALUE;
    MCTSNode bestNode = null;
    //ProLogger.info("number of childnodes " + node.ChildNodes.size());
    for(MCTSNode child : node.ChildNodes)
    {

      //ProLogger.info("child N " + child.Q);
      //ProLogger.info("child Q " + child.N);
      //averageQ = (child.Q / child.N)+multiplier;
      averageQ = (child.Q / child.N);
      //ProLogger.info("average Q " + averageQ);
      //ProLogger.info("bestaverage Q " + bestAverageQ);
      if (averageQ > bestAverageQ)
      {
        bestAverageQ = averageQ;
        bestNode = child;
      }
    }

    return bestNode;
  }


  protected NewAction BestFinalAction(MCTSNode node)
  {
    MCTSNode bestChild = this.BestChild(node);
    if (bestChild == null) return null;

    this.BestFirstChild = bestChild;

    //this is done for debugging proposes only
    this.BestActionSequence = new ArrayList<NewAction>();
    this.BestActionSequence.add(bestChild.action);
    node = bestChild;

    while(!node.state.isTerminal())
    {
      bestChild = this.BestChild(node);
      if (bestChild == null) break;
      this.BestActionSequence.add(bestChild.action);
      node = bestChild;
    }

    return this.BestFirstChild.action;
  }

  protected ArrayList<NewAction> BestFinalActions(MCTSNode node)
  {
    ProLogger.info("starting best final action wrapper");
    MCTSNode bestChild = this.BestChild(node);
    if (bestChild == null) {
      throw new NullPointerException();
      //return null;
    }


    this.BestFirstChild = bestChild;

    //this is done for debugging proposes only
    this.BestActionSequence = new ArrayList<NewAction>();
    ProLogger.info("starting iterating");
    while(true)
    {

      ProLogger.info("iteration");
      bestChild = this.BestChild(node);
      if (bestChild == null || (bestChild.action instanceof NewActionAdvance)) break;

      ProLogger.info(bestChild.action+"");
      if (!(bestChild.action instanceof NewActionSkip)){
        this.BestActionSequence.add(bestChild.action);
      }
      node = bestChild;
    }

    return BestActionSequence;
  }

  public void printStructure(MCTSNode initialNode, int i){

    try {
      File myObj = new File("Estrutura"+i+".txt");
      Writer myWriter = Files.newBufferedWriter(myObj.toPath(), Charset.defaultCharset());
      myWriter.write("TotalSelectionTime : "+totalSelectionTime+"\n"
          +"TotalPlayoutTime : "+totalPlayoutTime+"\n"
          +"TotalBackpropagateTime : "+totalBackpropagateTime+"\n"
          +"TotalExpandTime : "+totalExpandTime+"\n"
          +"GenerateActionsTime : "+generateActionsTime+"\n"
          +"ApplyActionsEffectsTime : "+applyActionsEffectsTime+"\n"
              +"GenerateWorldTime: "+generateWorldTime+"\n"
              +"CountUnitTime : "+countUnitsTime+"\n\n\n"
              +"CloneDataTime : "+WorldModel.cloneDataTime+"\n"
              +"CreateWorldTime : "+WorldModel.worldModelCreateTime+"\n\n\n"
          +"TotalActionsTime : "+NewAction.totalActionTime/NewAction.totalActionExecutions+"\n"
          //+"TotalIntermediateActionsTime : "+IntermediateAction.totalIntermediateActionTime/IntermediateAction.totalIntermediateActionExecutions+"\n"
          //+"TotalBattleTime: "+Action.battleTime/Action.battleExecutions+"\n"
          //+"TotalPlaceTime: "+Action.placeTime/Action.placeExecutions+"\n"
          //+"TotalBidTime: "+Action.bidTime/Action.bidExecutions+"\n"
          //+"TotalPurchaseeTime: "+Action.purchaseTime/Action.purchaseExecutions+"\n\n\n"
      );
      myWriter.write(initialNode.state.data.getSequence().getStep().getName());
      printRecursion(initialNode,myWriter, "    ");
      myWriter.close();
      System.out.println("Successfully wrote to the file.");
    } catch (IOException e) {
      System.out.println("An error occurred.");
      e.printStackTrace();
      System.out.println("Deu erro 1");
    }
  }

  public void printRecursion(MCTSNode node, Writer writer, String ident){
    try {
      for (MCTSNode n : node.ChildNodes) {
        if (n.action!=null){
          writer.write("\n"+ident+"-- N: "+n.N+", Q: "+n.Q+", "+n.action+" -> "+ n.state.data.getSequence().getStep().getName());
          //writer.write("\n"+ident + node.action.getU()+ " to "+node.action.getT()+" -> " + n.state.data.getSequence().getStep().getName());
        } else {
          writer.write("\n"+ident +"---"+ "null action -> " + n.state.data.getSequence().getStep().getName());
        }

        printRecursion(n, writer, ident + "    ");
      }
    } catch (IOException e){
      System.out.println("Deu erro 2");
    }
  }

}


