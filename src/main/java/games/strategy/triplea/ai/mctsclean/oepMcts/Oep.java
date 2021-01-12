package games.strategy.triplea.ai.mctsclean.oepMcts;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.pro.logging.ProLogger;
import games.strategy.triplea.ai.weak.WeakAi;
import games.strategy.triplea.delegate.Matches;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.triplea.java.collections.CollectionUtils;

public class Oep {

  static final int GENE_SIZE=50;
  static final int executionTime=100000;
  static final double mutationP=0.5;
  private static final WeakAi weakAi= new WeakAi("");
  private static GamePlayer player;
  OepWorldModel initialState;

  public Oep(){

  };

  public static double eval(OepWorldModel state){
    return Playout(state);
  }

  protected static double Playout(OepWorldModel state)
  {
    int i=0;

    WeakAi.ss="";
    //long time=System.currentTimeMillis();
    while(!state.isTerminal() && i<40)
    {
      i++;

      state.doMoveWeakAi(weakAi);
      //ProLogger.info("do move time - "+(System.currentTimeMillis()-time));
      //time=System.currentTimeMillis();
      OepWorldModel.advancePlayout(state);
      //ProLogger.info("advance time - "+(System.currentTimeMillis()-time));
      //time=System.currentTimeMillis();
    }

    if (state.isTerminal()){
      if (state.victors.contains(player)){
        return 0.5+0.5*((40-i)/40);
      }
      return -0.5-0.5*((40-i)/40);
    }

    Set<Unit> set = new HashSet<>();
    for (Territory t:state.data.getMap()){
      set.addAll(t.getUnits());
    }
    float unitSize= CollectionUtils.getMatches(set, Matches.unitOwnedBy(player)).size();
    float enemyUnitSize=set.size()-unitSize;
    //ProLogger.info("rest time - "+(System.currentTimeMillis()-time));
    return (unitSize-enemyUnitSize)/(set.size() * 2);
  }

  public ArrayList<OepAction> run(OepWorldModel state){
    initialState=state;
    int newgenomeN=0;
    player=state.data.getSequence().getStep().getPlayerId();
    List<Genome0> genes= new ArrayList<>();
    init(genes, state);
    float startTime=System.currentTimeMillis();
    while (true){
      long time=System.currentTimeMillis();
      for(Genome0 g:genes){
        OepWorldModel newState=state.generateChildWorldModel();
        //ProLogger.info("generate world time - "+(System.currentTimeMillis()-time));
        //time=System.currentTimeMillis();
        g.executeActions(newState);
        //ProLogger.info("execute actions time - "+(System.currentTimeMillis()-time));
        //time=System.currentTimeMillis();
        g.visits++;
        if (g.visits>0){
          g.value=eval(newState);
          //ProLogger.info("eval time - "+(System.currentTimeMillis()-time));
          //time=System.currentTimeMillis();

        }
      }
      if (System.currentTimeMillis()-startTime>executionTime){
        break;
      }

      newgenomeN++;
      ProLogger.info("playouts time - "+(System.currentTimeMillis()-time));
      time=System.currentTimeMillis();
      Collections.sort(genes,Collections.reverseOrder());
      int size=genes.size();
      List<Genome0> newGenes= genes.subList(0,size/2);
      ProLogger.info("sort time - "+(System.currentTimeMillis()-time));
      time=System.currentTimeMillis();
      genes=procreate(newGenes);
      ProLogger.info("new genes time - "+(System.currentTimeMillis()-time));
      ProLogger.info("created new genome");
    }
    if (1<2){

    }
    return genes.get(0).actions;


  }

  public List<Genome0> procreate(List<Genome0> genes){
    ArrayList<Genome0> newGenes=new ArrayList<>();
    for(Genome0 g:genes){
      ArrayList<OepAction> newActions=new ArrayList<>();
      Genome0 otherGenome=genes.get((int)(Math.random()*genes.size()));
      for(int i=0;i<g.actions.size();i++){
        double n=Math.random();
        if(n>0.5){
          newActions.add(g.actions.get(i));
        } else if (n>0.05) {
          newActions.add(otherGenome.actions.get(i));
        } else {
          //mudar
          //newActions.add(Genome0.randomActionForUnit(g.actions.get(i).getU(),initialState));
        }
      }

      if(Math.random()>mutationP){

        //Gerar ação random e mudar
      }
      newGenes.add(new Genome0(newActions));

    }
    genes.addAll(newGenes);
    return genes;
  }

  public void init(List<Genome0> genes, OepWorldModel state){
    for (int i=0;i<GENE_SIZE;i++){
      Genome0 g= new Genome0(state);
      ProLogger.info("created new gene");
      ProLogger.info(System.currentTimeMillis()+"");
      genes.add(g);
    }
  }
}
