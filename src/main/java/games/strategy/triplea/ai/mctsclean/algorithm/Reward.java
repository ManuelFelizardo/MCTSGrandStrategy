package games.strategy.triplea.ai.mctsclean.algorithm;


public class Reward {

    public int id;
    public double value;
    public Reward(int id, double value){
        this.id=id;
        this.value=value;
    }
    //implementar
    public double getRewardForNode(MCTSNode node){
        return value;
    }
}