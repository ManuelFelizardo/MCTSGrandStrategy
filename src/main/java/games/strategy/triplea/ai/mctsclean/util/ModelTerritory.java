package games.strategy.triplea.ai.mctsclean.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ai.pro.util.ProMatches;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TransportTracker;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.triplea.java.collections.CollectionUtils;

/** The result of an AI territory analysis. */
public class ModelTerritory {

    private final MctsData mctsData;
    private final Territory territory;
    private final List<Unit> maxUnits;
    private final List<Unit> units;
    private final List<Unit> bombers;
    private double value;
    private double seaValue;
    private boolean canHold;
    private boolean canAttack;
    private double strengthEstimate;

    // Amphib variables
    private final List<Unit> maxAmphibUnits;
    private final Map<Unit, List<Unit>> amphibAttackMap;
    private final Map<Unit, Territory> transportTerritoryMap;
    private boolean needAmphibUnits;
    private boolean strafing;
    private final Map<Unit, Boolean> isTransportingMap;
    private final Set<Unit> maxBombardUnits;
    private final Map<Unit, Set<Territory>> bombardOptionsMap;
    private final Map<Unit, Territory> bombardTerritoryMap;

    // Determine territory to attack variables
    private boolean currentlyWins;

    // Non-combat move variables
    private final List<Unit> cantMoveUnits;
    private List<Unit> maxEnemyUnits;
    private Set<Unit> maxEnemyBombardUnits;
    private final List<Unit> tempUnits;
    private final Map<Unit, List<Unit>> tempAmphibAttackMap;
    private double loadValue;

    // Scramble variables
    private final List<Unit> maxScrambleUnits;

    public ModelTerritory(final Territory territory, final MctsData mctsData) {
        this.territory = territory;
        this.mctsData = mctsData;
        maxUnits = new ArrayList<>();
        units = new ArrayList<>();
        bombers = new ArrayList<>();
        value = 0;
        seaValue = 0;
        canHold = true;
        canAttack = false;
        strengthEstimate = Double.POSITIVE_INFINITY;

        maxAmphibUnits = new ArrayList<>();
        amphibAttackMap = new HashMap<>();
        transportTerritoryMap = new HashMap<>();
        needAmphibUnits = false;
        strafing = false;
        isTransportingMap = new HashMap<>();
        maxBombardUnits = new HashSet<>();
        bombardOptionsMap = new HashMap<>();
        bombardTerritoryMap = new HashMap<>();

        currentlyWins = false;

        cantMoveUnits = new ArrayList<>();
        maxEnemyUnits = new ArrayList<>();
        maxEnemyBombardUnits = new HashSet<>();
        tempUnits = new ArrayList<>();
        tempAmphibAttackMap = new HashMap<>();
        loadValue = 0;

        maxScrambleUnits = new ArrayList<>();
    }

    ModelTerritory(final ModelTerritory patd, final MctsData mctsData) {
        this.territory = patd.getTerritory();
        this.mctsData = mctsData;
        maxUnits = new ArrayList<>(patd.getMaxUnits());
        units = new ArrayList<>(patd.getUnits());
        bombers = new ArrayList<>(patd.getBombers());
        value = patd.getValue();
        seaValue = patd.getSeaValue();
        canHold = patd.isCanHold();
        canAttack = patd.isCanAttack();
        strengthEstimate = patd.getStrengthEstimate();

        maxAmphibUnits = new ArrayList<>(patd.getMaxAmphibUnits());
        amphibAttackMap = new HashMap<>(patd.getAmphibAttackMap());
        transportTerritoryMap = new HashMap<>(patd.getTransportTerritoryMap());
        needAmphibUnits = patd.isNeedAmphibUnits();
        strafing = patd.isStrafing();
        isTransportingMap = new HashMap<>(patd.getIsTransportingMap());
        maxBombardUnits = new HashSet<>(patd.getMaxBombardUnits());
        bombardOptionsMap = new HashMap<>(patd.getBombardOptionsMap());
        bombardTerritoryMap = new HashMap<>(patd.getBombardTerritoryMap());

        currentlyWins = patd.isCurrentlyWins();

        cantMoveUnits = new ArrayList<>(patd.getCantMoveUnits());
        maxEnemyUnits = new ArrayList<>(patd.getMaxEnemyUnits());
        maxEnemyBombardUnits = new HashSet<>(patd.getMaxEnemyBombardUnits());
        tempUnits = new ArrayList<>(patd.getTempUnits());
        tempAmphibAttackMap = new HashMap<>(patd.getTempAmphibAttackMap());
        loadValue = patd.getLoadValue();

        maxScrambleUnits = new ArrayList<>(patd.getMaxScrambleUnits());
    }

    public List<Unit> getAllDefenders() {
        final List<Unit> defenders = new ArrayList<>(units);
        defenders.addAll(cantMoveUnits);
        defenders.addAll(tempUnits);
        return defenders;
    }

    public List<Unit> getAllDefendersForCarrierCalcs(final GameData data, final GamePlayer player) {
        if (Properties.getProduceNewFightersOnOldCarriers(data)) {
            return getAllDefenders();
        }

        final List<Unit> defenders =
                CollectionUtils.getMatches(cantMoveUnits, ProMatches.unitIsOwnedCarrier(player).negate());
        defenders.addAll(units);
        defenders.addAll(tempUnits);
        return defenders;
    }

    public List<Unit> getMaxDefenders() {
        final List<Unit> defenders = new ArrayList<>(maxUnits);
        defenders.addAll(cantMoveUnits);
        return defenders;
    }

    public List<Unit> getMaxEnemyDefenders(final GamePlayer player, final GameData data) {
        final List<Unit> defenders =
                territory.getUnitCollection().getMatches(Matches.enemyUnit(player, data));
        defenders.addAll(maxScrambleUnits);
        return defenders;
    }

    public void addUnit(final Unit unit) {
        this.units.add(unit);
    }

    public void addUnits(final List<Unit> units) {
        this.units.addAll(units);
    }

    void addMaxAmphibUnits(final List<Unit> amphibUnits) {
        this.maxAmphibUnits.addAll(amphibUnits);
    }

    void addMaxUnit(final Unit unit) {
        this.maxUnits.add(unit);
    }

    void addMaxUnits(final List<Unit> units) {
        this.maxUnits.addAll(units);
    }

    public Territory getTerritory() {
        return territory;
    }

    public List<Unit> getMaxUnits() {
        return maxUnits;
    }

    public void setValue(final double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    public List<Unit> getUnits() {
        return units;
    }

    public void setCanHold(final boolean canHold) {
        this.canHold = canHold;
    }

    public boolean isCanHold() {
        return canHold;
    }

    public List<Unit> getMaxAmphibUnits() {
        return maxAmphibUnits;
    }

    public void setNeedAmphibUnits(final boolean needAmphibUnits) {
        this.needAmphibUnits = needAmphibUnits;
    }

    public boolean isNeedAmphibUnits() {
        return needAmphibUnits;
    }

    public boolean isStrafing() {
        return strafing;
    }

    public void setStrafing(final boolean strafing) {
        this.strafing = strafing;
    }

    public Map<Unit, List<Unit>> getAmphibAttackMap() {
        return amphibAttackMap;
    }

    public void putAllAmphibAttackMap(final Map<Unit, List<Unit>> amphibAttackMap) {
        for (final Unit u : amphibAttackMap.keySet()) {
            putAmphibAttackMap(u, amphibAttackMap.get(u));
        }
    }

    public void putAmphibAttackMap(final Unit transport, final List<Unit> amphibUnits) {
        this.amphibAttackMap.put(transport, amphibUnits);
        this.isTransportingMap.put(transport, TransportTracker.isTransporting(transport));
    }

    public void setCanAttack(final boolean canAttack) {
        this.canAttack = canAttack;
    }

    public boolean isCanAttack() {
        return canAttack;
    }

    public void setStrengthEstimate(final double strengthEstimate) {
        this.strengthEstimate = strengthEstimate;
    }

    public double getStrengthEstimate() {
        return strengthEstimate;
    }

    public boolean isCurrentlyWins() {
        return currentlyWins;
    }

    public List<Unit> getCantMoveUnits() {
        return cantMoveUnits;
    }

    public void addCantMoveUnit(final Unit unit) {
        this.cantMoveUnits.add(unit);
    }

    public void setMaxEnemyUnits(final List<Unit> maxEnemyUnits) {
        this.maxEnemyUnits = maxEnemyUnits;
    }

    public List<Unit> getMaxEnemyUnits() {
        return maxEnemyUnits;
    }

    public List<Unit> getTempUnits() {
        return tempUnits;
    }

    public void addTempUnit(final Unit unit) {
        this.tempUnits.add(unit);
    }

    public void addTempUnits(final List<Unit> units) {
        this.tempUnits.addAll(units);
    }

    public Map<Unit, List<Unit>> getTempAmphibAttackMap() {
        return tempAmphibAttackMap;
    }

    public void putTempAmphibAttackMap(final Unit transport, final List<Unit> amphibUnits) {
        this.tempAmphibAttackMap.put(transport, amphibUnits);
    }

    public Map<Unit, Territory> getTransportTerritoryMap() {
        return transportTerritoryMap;
    }

    public void setLoadValue(final double loadValue) {
        this.loadValue = loadValue;
    }

    public double getLoadValue() {
        return loadValue;
    }

    public Map<Unit, Boolean> getIsTransportingMap() {
        return isTransportingMap;
    }

    public void setSeaValue(final double seaValue) {
        this.seaValue = seaValue;
    }

    public double getSeaValue() {
        return seaValue;
    }

    public Map<Unit, Territory> getBombardTerritoryMap() {
        return bombardTerritoryMap;
    }

    public Set<Unit> getMaxBombardUnits() {
        return maxBombardUnits;
    }

    void addMaxBombardUnit(final Unit unit) {
        this.maxBombardUnits.add(unit);
    }

    public Map<Unit, Set<Territory>> getBombardOptionsMap() {
        return bombardOptionsMap;
    }

    void addBombardOptionsMap(final Unit unit, final Territory t) {
        if (bombardOptionsMap.containsKey(unit)) {
            bombardOptionsMap.get(unit).add(t);
        } else {
            final Set<Territory> territories = new HashSet<>();
            territories.add(t);
            bombardOptionsMap.put(unit, territories);
        }
    }

    public void setMaxEnemyBombardUnits(final Set<Unit> maxEnemyBombardUnits) {
        this.maxEnemyBombardUnits = maxEnemyBombardUnits;
    }

    public Set<Unit> getMaxEnemyBombardUnits() {
        return maxEnemyBombardUnits;
    }

    public List<Unit> getMaxScrambleUnits() {
        return maxScrambleUnits;
    }

    public List<Unit> getBombers() {
        return bombers;
    }

    @Override
    public String toString(){
        String string="";
        string+=territory.getName()+" :";
        for(Unit u:units){
            string+=u.toString()+"; ";
        }
        return string;
    }
}
