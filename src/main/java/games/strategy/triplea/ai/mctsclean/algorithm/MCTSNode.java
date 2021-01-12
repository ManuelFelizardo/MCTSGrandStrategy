package games.strategy.triplea.ai.mctsclean.algorithm;

import games.strategy.engine.data.GamePlayer;
import games.strategy.triplea.ai.mctsclean.util.*;
import java.util.ArrayList;

public class MCTSNode
{
    public WorldModel state;
    public MCTSNode parent ;
    public NewAction action ;
    public int PlayerID ;
    public ArrayList<MCTSNode> ChildNodes;
    public int N ;
    public int NRAVE ;
    public float Q ;
    public float QRAVE ;
    public float H ;
    public double AcumulatedE;
    GamePlayer player;

    public MCTSNode(WorldModel state, GamePlayer player, NewAction action)
    {
        this.ChildNodes= new ArrayList<MCTSNode>();
        this.state = state;
        this.player=player;
        this.action=action;
        this.Q=0;
        this.N=0;
        this.H=0;
    }
}