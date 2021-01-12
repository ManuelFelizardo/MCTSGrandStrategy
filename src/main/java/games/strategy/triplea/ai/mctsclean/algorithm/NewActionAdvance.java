package games.strategy.triplea.ai.mctsclean.algorithm;

public class NewActionAdvance extends NewAction {
  public NewActionAdvance() {
    super(null, null);
  }

  @Override
  public void applyActionEffects(WorldModel state){
    ActionUtils.advance(state);
  }



  @Override
  public String toString(){
    return "Advance Action";
  }


}
