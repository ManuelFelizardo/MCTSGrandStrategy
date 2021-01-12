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

public class Oep2 {

  static final int GENE_SIZE=25;
  static final int executionTime=1000;
  static final double mutationP=0.5;
  private static final WeakAi weakAi= new WeakAi("");
  private static GamePlayer player;
  OepWorldModel initialState;
  public static double E=0.5;

  public Oep2(){

  };

  public static double eval(OepWorldModel state){
    return Playout(state);
  }

  protected static double Playout(OepWorldModel state)
  {
    int i=0;

    WeakAi.ss="";
    OepWorldModel.advancePlayout(state);
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
    //ProLogger.info("Weak ai ss " + WeakAi.ss);


    if (state.isTerminal()){
      if (state.victors.contains(player)){
        return 0.6+0.4*((40-i)/40.0);
      }
      return -0.6-0.4*((40-i)/40.0);
    }

    Set<Unit> set = new HashSet<>();
    for (Territory t:state.data.getMap()){
      set.addAll(t.getUnits());
    }
    float unitSize= CollectionUtils.getMatches(set, Matches.unitOwnedBy(player)).size();
    float enemyUnitSize=set.size()-unitSize;
    //ProLogger.info("rest time - "+(System.currentTimeMillis()-time));
    return (unitSize-enemyUnitSize)/(set.size() * 2.5);
  }

  public ArrayList<OepAction2> run(OepWorldModel state){
    initialState=state;
    int newgenomeN=0;
    player=state.data.getSequence().getStep().getPlayerId();
    List<Genome> genes= new ArrayList<>();
    init(genes, state);
    long startTime=System.currentTimeMillis();
    int iterations=0;
    while (true){
      long time=System.currentTimeMillis();
      for(Genome g:genes){
        if (g.visits==0) {
          OepWorldModel newState = state.generateChildWorldModel();
          //ProLogger.info("generate world time - "+(System.currentTimeMillis()-time));
          //time=System.currentTimeMillis();
          g.executeActions(newState);
          //ProLogger.info("execute actions time - "+(System.currentTimeMillis()-time));
          //time=System.currentTimeMillis();
          g.visits++;
          if (g.visits > 0) {
            ProLogger.info("current gene:" + g.actions.toString());
            g.value = eval(newState);
            ProLogger.info("\ng value -"+g.value);
            //ProLogger.info("eval time - "+(System.currentTimeMillis()-time));
            //time=System.currentTimeMillis();

          }
        }
      }
      if (System.currentTimeMillis()-startTime>executionTime){
        break;
      }

      newgenomeN++;
      ProLogger.info(System.currentTimeMillis()-startTime+"");
      ProLogger.info(genes.toString());
      ProLogger.info("playouts time - "+(System.currentTimeMillis()-time));
      time=System.currentTimeMillis();
      Collections.sort(genes,Collections.reverseOrder());
      int size=genes.size();
      List<Genome> newGenes= genes.subList(0,size/2);
      ProLogger.info("sort time - "+(System.currentTimeMillis()-time));
      time=System.currentTimeMillis();
      genes=procreate(newGenes);
      ProLogger.info("new genes time - "+(System.currentTimeMillis()-time));
      ProLogger.info("created new genome");
      iterations++;
    }
    if (genes.get(0).value>0.75){
      //throw new NullPointerException(genes.get(0).value+" - "+iterations);
    }
    ProLogger.info(" total iterations nr - "+iterations);
    return new ArrayList(genes.get(0).actions.values());


  }

  public List<Genome> procreate(List<Genome> genes){
    ArrayList<Genome> newGenes=new ArrayList<>();
    for(Genome g:genes){
      //ArrayList<OepAction2> newActions=new ArrayList<>();
      //Genome otherGenome=genes.get((int)(Math.random()*genes.size()));
      Genome newGenome= new Genome();
      Genome otherGenome=genes.get((int)(Math.random()*genes.size()));
      ArrayList<Integer> indexes= new ArrayList<>();
      for(int i=0;i<g.actions.size();i++){
        indexes.add(i);
      }
      Collections.shuffle(indexes);
      ArrayList<Territory> ts= new ArrayList<>(g.actions.keySet());
      for(int i:indexes){
        double n=Math.random()*(1+E);
        if(n<0.5){
          if (newGenome.addActions(g.actions.get(ts.get(i)))){
            continue;
          }
        } else if (n<1) {
          if (newGenome.addActions(otherGenome.actions.get(ts.get(i)))){
            continue;
          }
        }
        newGenome.randomActionForTerritory(g.actions.get(ts.get(i)).getT(),initialState);

      }
      newGenes.add(newGenome);

    }
    genes.addAll(newGenes);
    //throw new NullPointerException();
    return genes;
  }

  public void init(List<Genome> genes, OepWorldModel state){
    for (int i=0;i<GENE_SIZE;i++){
      Genome g= new Genome(state);
      ProLogger.info("created new gene");
      ProLogger.info(System.currentTimeMillis()+"");
      genes.add(g);
    }
  }
}
