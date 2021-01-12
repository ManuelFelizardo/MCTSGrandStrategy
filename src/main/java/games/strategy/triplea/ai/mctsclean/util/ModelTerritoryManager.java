package games.strategy.triplea.ai.mctsclean.util;

import static games.strategy.triplea.delegate.Matches.isTerritoryEnemy;
import static games.strategy.triplea.delegate.Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassableOrRestricted;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ai.AiUtils;
import games.strategy.triplea.ai.pro.ProData;
import games.strategy.triplea.ai.pro.logging.ProLogger;
import games.strategy.triplea.ai.pro.util.ProBattleUtils;
import games.strategy.triplea.ai.pro.util.ProMatches;
import games.strategy.triplea.ai.pro.util.ProTransportUtils;
import games.strategy.triplea.ai.pro.util.ProUtils;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.triplea.delegate.battle.ScrambleLogic;
import games.strategy.triplea.delegate.move.validation.MoveValidator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import lombok.Getter;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.util.Tuple;

/** Manages info about territories. */
public class ModelTerritoryManager {

    private final MctsData mctsData;
    private final GamePlayer player;

    private ModelMoveOptions attackOptions;
    private ModelMoveOptions defendOptions;

    public ModelTerritoryManager(final MctsData mctsData) {
        this.mctsData = mctsData;
        player = mctsData.getData().getSequence().getStep().getPlayerId();
        ProLogger.info("Model Territory player - "+player.getName());
        attackOptions = new ModelMoveOptions();
        defendOptions = new ModelMoveOptions();
    }

    public ModelTerritoryManager(
            final MctsData mctsData,
            final ModelTerritoryManager territoryManager) {
        this(mctsData);
        attackOptions = new ModelMoveOptions(territoryManager.attackOptions, mctsData);
        defendOptions = new ModelMoveOptions(territoryManager.defendOptions, mctsData);
    }

    /** Sets 'alliedAttackOptions' field to possible available attack options. */
    public void populateAttackOptions() {
        findAttackOptions(
                mctsData,
                player,
                mctsData.getMyUnitTerritories(),
                attackOptions.getTerritoryMap(),
                attackOptions.getUnitMoveMap(),
                attackOptions.getTransportMoveMap(),
                attackOptions.getBombardMap(),
                attackOptions.getTransportList(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                false,
                false);
        findBombingOptions();
    }

    public void populateDefenseOptions(final List<Territory> clearedTerritories) {
        findDefendOptions(
            mctsData,
            player,
            mctsData.getMyUnitTerritories(),
            defendOptions.getTerritoryMap(),
            defendOptions.getUnitMoveMap(),
            defendOptions.getTransportMoveMap(),
            defendOptions.getTransportList(),
            clearedTerritories,
            false);
    }

    public ModelMoveOptions getAttackOptions() {
        return attackOptions;
    }

    public ModelMoveOptions getDefenseOptions() {
        return defendOptions;
    }


    private static void findAttackOptions(
            final MctsData mctsData,
            final GamePlayer player,
            final List<Territory> myUnitTerritories,
            final Map<Territory, ModelTerritory> moveMap,
            final Map<Unit, Set<Territory>> unitMoveMap,
            final Map<Unit, Set<Territory>> transportMoveMap,
            final Map<Unit, Set<Territory>> bombardMap,
            final List<ModelTransport> transportMapList,
            final List<Territory> enemyTerritories,
            final List<Territory> alliedTerritories,
            final List<Territory> territoriesToCheck,
            final boolean isCheckingEnemyAttacks,
            final boolean isIgnoringRelationships) {
        final GameData data = mctsData.getData();

        final Map<Territory, Set<Territory>> landRoutesMap = new HashMap<>();
        final List<Territory> territoriesThatCantBeHeld = new ArrayList<>(enemyTerritories);
        territoriesThatCantBeHeld.addAll(territoriesToCheck);
        findLandMoveOptions(
                mctsData,
                player,
                myUnitTerritories,
                moveMap,
                unitMoveMap,
                landRoutesMap,
                ProMatches.territoryIsEnemyOrCantBeHeld(player, data, territoriesThatCantBeHeld),
                enemyTerritories,
                alliedTerritories,
                true,
                isCheckingEnemyAttacks,
                isIgnoringRelationships);
        /*
        findAirMoveOptions(
                mctsData,
                player,
                myUnitTerritories,
                moveMap,
                unitMoveMap,
                ProMatches.territoryHasEnemyUnitsOrCantBeHeld(player, data, territoriesThatCantBeHeld),
                enemyTerritories,
                alliedTerritories,
                true,
                isCheckingEnemyAttacks,
                isIgnoringRelationships);
        findBombardOptions(
                mctsData,
                player,
                myUnitTerritories,
                moveMap,
                bombardMap,
                transportMapList,
                isCheckingEnemyAttacks);
                
         */


    }

    private static void findDefendOptions(
        final MctsData mctsData,
        final GamePlayer player,
        final List<Territory> myUnitTerritories,
        final Map<Territory, ModelTerritory> moveMap,
        final Map<Unit, Set<Territory>> unitMoveMap,
        final Map<Unit, Set<Territory>> transportMoveMap,
        final List<ModelTransport> transportMapList,
        final List<Territory> clearedTerritories,
        final boolean isCheckingEnemyAttacks) {
        final GameData data = mctsData.getData();

        final Map<Territory, Set<Territory>> landRoutesMap = new HashMap<>();
        findLandMoveOptions(
            mctsData,
            player,
            myUnitTerritories,
            moveMap,
            unitMoveMap,
            landRoutesMap,
            Matches.isTerritoryAllied(player, data),
            new ArrayList<>(),
            clearedTerritories,
            false,
            isCheckingEnemyAttacks,
            false);
        /*
        findAirMoveOptions(
            mctsData,
            player,
            myUnitTerritories,
            moveMap,
            unitMoveMap,
            ProMatches.territoryCanLandAirUnits(
                player, data, false, new ArrayList<>(), new ArrayList<>()),
            new ArrayList<>(),
            new ArrayList<>(),
            false,
            isCheckingEnemyAttacks,
            false);

         */
    }



    private void findBombingOptions() {
        for (final Unit unit : attackOptions.getUnitMoveMap().keySet()) {
            if (Matches.unitIsStrategicBomber().test(unit)) {
                attackOptions
                        .getBomberMoveMap()
                        .put(unit, new HashSet<>(attackOptions.getUnitMoveMap().get(unit)));
            }
        }
    }

    private static void findLandMoveOptions(
            final MctsData mctsData,
            final GamePlayer player,
            final List<Territory> myUnitTerritories,
            final Map<Territory, ModelTerritory> moveMap,
            final Map<Unit, Set<Territory>> unitMoveMap,
            final Map<Territory, Set<Territory>> landRoutesMap,
            final Predicate<Territory> moveToTerritoryMatch,
            final List<Territory> enemyTerritories,
            final List<Territory> clearedTerritories,
            final boolean isCombatMove,
            final boolean isCheckingEnemyAttacks,
            final boolean isIgnoringRelationships) {

        //ProLogger.info("findLandMoveOptions");
        //ProLogger.info("My unit territories size: "+myUnitTerritories.size());
        final GameData data = mctsData.getData();

        for (final Territory myUnitTerritory : myUnitTerritories) {
            if (mctsData.getData().getSequence().getStep().getName().contains("talians")){
                throw new NullPointerException();
            }
            ProLogger.info("iterated");
            // Find my land units that have movement left
            final List<Unit> myLandUnits =
                    myUnitTerritory
                            .getUnitCollection()
                            .getMatches(ProMatches.unitCanBeMovedAndIsOwnedLand(player, isCombatMove));

            // Check each land unit individually since they can have different ranges
            for (final Unit myLandUnit : myLandUnits) {

                if(mctsData.getUnitTerritory(myLandUnit)==null){
                    throw new NullPointerException(myLandUnit.toString()+", "+player+", "+mctsData.getData().getSequence().getStep().getPlayerId());
                }
                final Territory startTerritory = mctsData.getUnitTerritory(myLandUnit);
                final BigDecimal range = myLandUnit.getMovementLeft();
                Set<Territory> possibleMoveTerritories =
                        data.getMap()
                                .getNeighborsByMovementCost(
                                        myUnitTerritory,
                                        myLandUnit,
                                        range,
                                        ProMatches.territoryCanMoveSpecificLandUnit(
                                                player, data, isCombatMove, myLandUnit));
                if (isIgnoringRelationships) {
                    possibleMoveTerritories =
                            data.getMap()
                                    .getNeighborsByMovementCost(
                                            myUnitTerritory,
                                            myLandUnit,
                                            range,
                                            ProMatches.territoryCanPotentiallyMoveSpecificLandUnit(
                                                    player, data, myLandUnit));
                }
                possibleMoveTerritories.add(myUnitTerritory);
                final Set<Territory> potentialTerritories =
                        new HashSet<>(
                                CollectionUtils.getMatches(possibleMoveTerritories, moveToTerritoryMatch));
                if (!isCombatMove) {
                    potentialTerritories.add(myUnitTerritory);
                }
                for (final Territory potentialTerritory : potentialTerritories) {

                    // Find route over land checking whether unit can blitz
                    Route myRoute =
                            data.getMap()
                                    .getRouteForUnit(
                                            myUnitTerritory,
                                            potentialTerritory,
                                            ProMatches.territoryCanMoveLandUnitsThrough(
                                                    player, data, myLandUnit, startTerritory, isCombatMove, enemyTerritories),
                                            myLandUnit,
                                            player);
                    if (isCheckingEnemyAttacks) {
                        myRoute =
                                data.getMap()
                                        .getRouteForUnit(
                                                myUnitTerritory,
                                                potentialTerritory,
                                                ProMatches.territoryCanMoveLandUnitsThroughIgnoreEnemyUnits(
                                                        player,
                                                        data,
                                                        myLandUnit,
                                                        startTerritory,
                                                        isCombatMove,
                                                        enemyTerritories,
                                                        clearedTerritories),
                                                myLandUnit,
                                                player);
                    }
                    if (myRoute == null) {
                        continue;
                    }
                    if (myRoute.hasMoreThenOneStep()
                            && myRoute.getMiddleSteps().stream().anyMatch(Matches.isTerritoryEnemy(player, data))
                            && Matches.unitIsOfTypes(
                            TerritoryEffectHelper.getUnitTypesThatLostBlitz(myRoute.getAllTerritories()))
                            .test(myLandUnit)) {
                        continue; // If blitzing then make sure none of the territories cause blitz ability to
                        // be lost
                    }
                    final BigDecimal myRouteLength = myRoute.getMovementCost(myLandUnit);
                    if (myRouteLength.compareTo(range) > 0) {
                        continue;
                    }

                    // Add to route map
                    if (landRoutesMap.containsKey(potentialTerritory)) {
                        landRoutesMap.get(potentialTerritory).add(myUnitTerritory);
                    } else {
                        final Set<Territory> territories = new HashSet<>();
                        territories.add(myUnitTerritory);
                        landRoutesMap.put(potentialTerritory, territories);
                    }

                    // Populate territories with land units
                    if (moveMap.containsKey(potentialTerritory)) {
                        moveMap.get(potentialTerritory).addUnit(myLandUnit);
                    } else {
                        final ModelTerritory moveTerritoryData = new ModelTerritory(potentialTerritory, mctsData);
                        moveTerritoryData.addUnit(myLandUnit);
                        moveMap.put(potentialTerritory, moveTerritoryData);
                    }

                    // Populate unit move options map
                    if (unitMoveMap.containsKey(myLandUnit)) {
                        unitMoveMap.get(myLandUnit).add(potentialTerritory);
                    } else {
                        final Set<Territory> unitMoveTerritories = new HashSet<>();
                        unitMoveTerritories.add(potentialTerritory);
                        unitMoveTerritories.add(myUnitTerritory);
                        unitMoveMap.put(myLandUnit, unitMoveTerritories);
                    }
                }
            }
        }
    }

    public static ArrayList<Territory> getNeighboringEnemyTerritories(
        final Collection<Territory> list, final GamePlayer player, final GameData data) {

        final ArrayList<Territory> neighbors = new ArrayList<>();
        for (Territory t:list){
            neighbors.addAll(data.getMap().getNeighbors(t, isTerritoryEnemyAndNotUnownedWaterOrImpassableOrRestricted(player, data)));
        }

        return neighbors;
    }

    public static boolean canAttackSuccessfully(Set<Unit> units, Territory t){
        final float enemyStrength = AiUtils.strength(t.getUnits(), false, false);
        final float ourStrength = AiUtils.strength(units, true, false);
        if (ourStrength>1.1*enemyStrength){
            return true;
        }
        return false;
    }


    public static Set<Unit> findLandMoveOptionsSingular(
        final MctsData mctsData,
        final GamePlayer player,
        final List<Territory> myUnitTerritories,
        final List<Territory> enemyTerritories,
        final List<Territory> clearedTerritories,
        final boolean isCombatMove,
        final boolean isCheckingEnemyAttacks,
        final Territory potentialTerritory) {

        Set<Unit> possibleUnits= new HashSet<>();

        //ProLogger.info("findLandMoveOptions");
        //ProLogger.info("My unit territories size: "+myUnitTerritories.size());
        final GameData data = mctsData.getData();

        for (final Territory myUnitTerritory : myUnitTerritories) {
            if (myUnitTerritory.equals(potentialTerritory)){
                continue;
            }
            // Find my land units that have movement left
            final List<Unit> myLandUnits =
                myUnitTerritory
                    .getUnitCollection()
                    .getMatches(ProMatches.unitCanBeMovedAndIsOwnedLand(player, isCombatMove));

            // Check each land unit individually since they can have different ranges
            for (final Unit myLandUnit : myLandUnits) {

                if(mctsData.getUnitTerritory(myLandUnit)==null){
                    throw new NullPointerException(myLandUnit.toString()+", "+player+", "+mctsData.getData().getSequence().getStep().getPlayerId());
                }
                final Territory startTerritory = mctsData.getUnitTerritory(myLandUnit);
                final BigDecimal range = myLandUnit.getMovementLeft();



                // Find route over land checking whether unit can blitz
                Route myRoute =
                    data.getMap()
                        .getRouteForUnit(
                            myUnitTerritory,
                            potentialTerritory,
                            ProMatches.territoryCanMoveLandUnitsThrough(
                                player, data, myLandUnit, startTerritory, isCombatMove, enemyTerritories),
                            myLandUnit,
                            player);
                if (isCheckingEnemyAttacks) {
                    myRoute =
                        data.getMap()
                            .getRouteForUnit(
                                myUnitTerritory,
                                potentialTerritory,
                                ProMatches.territoryCanMoveLandUnitsThroughIgnoreEnemyUnits(
                                    player,
                                    data,
                                    myLandUnit,
                                    startTerritory,
                                    isCombatMove,
                                    enemyTerritories,
                                    clearedTerritories),
                                myLandUnit,
                                player);
                }
                if (myRoute == null) {
                    continue;
                }
                if (myRoute.hasMoreThenOneStep()
                    && myRoute.getMiddleSteps().stream().anyMatch(Matches.isTerritoryEnemy(player, data))
                    && Matches.unitIsOfTypes(
                    TerritoryEffectHelper.getUnitTypesThatLostBlitz(myRoute.getAllTerritories()))
                    .test(myLandUnit)) {
                    continue; // If blitzing then make sure none of the territories cause blitz ability to
                    // be lost
                }
                final BigDecimal myRouteLength = myRoute.getMovementCost(myLandUnit);
                if (myRouteLength.compareTo(range) > 0) {
                    continue;
                }

                possibleUnits.add(myLandUnit);

            }
        }
        return possibleUnits;
    }

    public static Set<Unit> findLandMoveOptionsSingular(
        final MctsData mctsData,
        final GamePlayer player,
        final List<Territory> myUnitTerritories,
        final List<Territory> enemyTerritories,
        final List<Territory> clearedTerritories,
        final boolean isCombatMove,
        final boolean isCheckingEnemyAttacks,
        final Territory potentialTerritory,
        final Set<Unit> excludedUnits) {

        Set<Unit> possibleUnits= new HashSet<>();

        //ProLogger.info("findLandMoveOptions");
        //ProLogger.info("My unit territories size: "+myUnitTerritories.size());
        final GameData data = mctsData.getData();

        for (final Territory myUnitTerritory : myUnitTerritories) {
            if (myUnitTerritory.equals(potentialTerritory)){
                continue;
            }
            // Find my land units that have movement left
            final List<Unit> myLandUnits =
                myUnitTerritory
                    .getUnitCollection()
                    .getMatches(ProMatches.unitCanBeMovedAndIsOwnedLand(player, isCombatMove));
            myLandUnits.removeAll(excludedUnits);

            // Check each land unit individually since they can have different ranges
            for (final Unit myLandUnit : myLandUnits) {

                if(mctsData.getUnitTerritory(myLandUnit)==null){
                    throw new NullPointerException(myLandUnit.toString()+", "+player+", "+mctsData.getData().getSequence().getStep().getPlayerId());
                }
                final Territory startTerritory = mctsData.getUnitTerritory(myLandUnit);
                final BigDecimal range = myLandUnit.getMovementLeft();



                // Find route over land checking whether unit can blitz
                Route myRoute =
                    data.getMap()
                        .getRouteForUnit(
                            myUnitTerritory,
                            potentialTerritory,
                            ProMatches.territoryCanMoveLandUnitsThrough(
                                player, data, myLandUnit, startTerritory, isCombatMove, enemyTerritories),
                            myLandUnit,
                            player);
                if (isCheckingEnemyAttacks) {
                    myRoute =
                        data.getMap()
                            .getRouteForUnit(
                                myUnitTerritory,
                                potentialTerritory,
                                ProMatches.territoryCanMoveLandUnitsThroughIgnoreEnemyUnits(
                                    player,
                                    data,
                                    myLandUnit,
                                    startTerritory,
                                    isCombatMove,
                                    enemyTerritories,
                                    clearedTerritories),
                                myLandUnit,
                                player);
                }
                if (myRoute == null) {
                    continue;
                }
                if (myRoute.hasMoreThenOneStep()
                    && myRoute.getMiddleSteps().stream().anyMatch(Matches.isTerritoryEnemy(player, data))
                    && Matches.unitIsOfTypes(
                    TerritoryEffectHelper.getUnitTypesThatLostBlitz(myRoute.getAllTerritories()))
                    .test(myLandUnit)) {
                    continue; // If blitzing then make sure none of the territories cause blitz ability to
                    // be lost
                }
                final BigDecimal myRouteLength = myRoute.getMovementCost(myLandUnit);
                if (myRouteLength.compareTo(range) > 0) {
                    continue;
                }

                possibleUnits.add(myLandUnit);

            }
        }
        return possibleUnits;
    }

    public static List<Set<Unit>> findLandMoveOptionsSingularSet(
        final MctsData mctsData,
        final GamePlayer player,
        final List<Territory> myUnitTerritories,
        final List<Territory> enemyTerritories,
        final List<Territory> clearedTerritories,
        final boolean isCombatMove,
        final boolean isCheckingEnemyAttacks,
        final Territory potentialTerritory) {

        List<Set<Unit>> unitSets= new ArrayList<>();

        //ProLogger.info("findLandMoveOptions");
        //ProLogger.info("My unit territories size: "+myUnitTerritories.size());
        final GameData data = mctsData.getData();

        for (final Territory myUnitTerritory : myUnitTerritories) {
            if (myUnitTerritory.equals(potentialTerritory)){
                continue;
            }
            // Find my land units that have movement left
            final List<Unit> myLandUnits =
                myUnitTerritory
                    .getUnitCollection()
                    .getMatches(ProMatches.unitCanBeMovedAndIsOwnedLand(player, isCombatMove));
            Set<Unit> tunits = new HashSet<>();

            // Check each land unit individually since they can have different ranges
            for (final Unit myLandUnit : myLandUnits) {

                if(mctsData.getUnitTerritory(myLandUnit)==null){
                    throw new NullPointerException(myLandUnit.toString()+", "+player+", "+mctsData.getData().getSequence().getStep().getPlayerId());
                }
                final Territory startTerritory = mctsData.getUnitTerritory(myLandUnit);
                final BigDecimal range = myLandUnit.getMovementLeft();



                // Find route over land checking whether unit can blitz
                Route myRoute =
                    data.getMap()
                        .getRouteForUnit(
                            myUnitTerritory,
                            potentialTerritory,
                            ProMatches.territoryCanMoveLandUnitsThrough(
                                player, data, myLandUnit, startTerritory, isCombatMove, enemyTerritories),
                            myLandUnit,
                            player);
                if (isCheckingEnemyAttacks) {
                    myRoute =
                        data.getMap()
                            .getRouteForUnit(
                                myUnitTerritory,
                                potentialTerritory,
                                ProMatches.territoryCanMoveLandUnitsThroughIgnoreEnemyUnits(
                                    player,
                                    data,
                                    myLandUnit,
                                    startTerritory,
                                    isCombatMove,
                                    enemyTerritories,
                                    clearedTerritories),
                                myLandUnit,
                                player);
                }
                if (myRoute == null) {
                    continue;
                }
                if (myRoute.hasMoreThenOneStep()
                    && myRoute.getMiddleSteps().stream().anyMatch(Matches.isTerritoryEnemy(player, data))
                    && Matches.unitIsOfTypes(
                    TerritoryEffectHelper.getUnitTypesThatLostBlitz(myRoute.getAllTerritories()))
                    .test(myLandUnit)) {
                    continue; // If blitzing then make sure none of the territories cause blitz ability to
                    // be lost
                }
                final BigDecimal myRouteLength = myRoute.getMovementCost(myLandUnit);
                if (myRouteLength.compareTo(range) > 0) {
                    continue;
                }
                tunits.add(myLandUnit);

            }

            if (tunits.size()>0){
                unitSets.add(tunits);
            }
        }

        return unitSets;
    }


    public static List<Set<Unit>> findLandMoveOptionsSingularSet(
        final MctsData mctsData,
        final GamePlayer player,
        final List<Territory> myUnitTerritories,
        final List<Territory> enemyTerritories,
        final List<Territory> clearedTerritories,
        final boolean isCombatMove,
        final boolean isCheckingEnemyAttacks,
        final Territory potentialTerritory,
        final Set<Unit> excludedUnits) {

        List<Set<Unit>> unitSets= new ArrayList<>();

        //ProLogger.info("findLandMoveOptions");
        //ProLogger.info("My unit territories size: "+myUnitTerritories.size());
        final GameData data = mctsData.getData();

        for (final Territory myUnitTerritory : myUnitTerritories) {
            if (myUnitTerritory.equals(potentialTerritory)){
                continue;
            }
            // Find my land units that have movement left
            final List<Unit> myLandUnits =
                myUnitTerritory
                    .getUnitCollection()
                    .getMatches(ProMatches.unitCanBeMovedAndIsOwnedLand(player, isCombatMove));
            Set<Unit> tunits = new HashSet<>();

            myLandUnits.removeAll(excludedUnits);

            // Check each land unit individually since they can have different ranges
            for (final Unit myLandUnit : myLandUnits) {

                if(mctsData.getUnitTerritory(myLandUnit)==null){
                    throw new NullPointerException(myLandUnit.toString()+", "+player+", "+mctsData.getData().getSequence().getStep().getPlayerId());
                }
                final Territory startTerritory = mctsData.getUnitTerritory(myLandUnit);
                final BigDecimal range = myLandUnit.getMovementLeft();



                // Find route over land checking whether unit can blitz
                Route myRoute =
                    data.getMap()
                        .getRouteForUnit(
                            myUnitTerritory,
                            potentialTerritory,
                            ProMatches.territoryCanMoveLandUnitsThrough(
                                player, data, myLandUnit, startTerritory, isCombatMove, enemyTerritories),
                            myLandUnit,
                            player);
                if (isCheckingEnemyAttacks) {
                    myRoute =
                        data.getMap()
                            .getRouteForUnit(
                                myUnitTerritory,
                                potentialTerritory,
                                ProMatches.territoryCanMoveLandUnitsThroughIgnoreEnemyUnits(
                                    player,
                                    data,
                                    myLandUnit,
                                    startTerritory,
                                    isCombatMove,
                                    enemyTerritories,
                                    clearedTerritories),
                                myLandUnit,
                                player);
                }
                if (myRoute == null) {
                    continue;
                }
                if (myRoute.hasMoreThenOneStep()
                    && myRoute.getMiddleSteps().stream().anyMatch(Matches.isTerritoryEnemy(player, data))
                    && Matches.unitIsOfTypes(
                    TerritoryEffectHelper.getUnitTypesThatLostBlitz(myRoute.getAllTerritories()))
                    .test(myLandUnit)) {
                    continue; // If blitzing then make sure none of the territories cause blitz ability to
                    // be lost
                }
                final BigDecimal myRouteLength = myRoute.getMovementCost(myLandUnit);
                if (myRouteLength.compareTo(range) > 0) {
                    continue;
                }
                tunits.add(myLandUnit);

            }

            if (tunits.size()>0){
                unitSets.add(tunits);
            }
        }

        return unitSets;
    }

    private static void findAirMoveOptions(
            final MctsData mctsData,
            final GamePlayer player,
            final List<Territory> myUnitTerritories,
            final Map<Territory, ModelTerritory> moveMap,
            final Map<Unit, Set<Territory>> unitMoveMap,
            final Predicate<Territory> moveToTerritoryMatch,
            final List<Territory> enemyTerritories,
            final List<Territory> alliedTerritories,
            final boolean isCombatMove,
            final boolean isCheckingEnemyAttacks,
            final boolean isIgnoringRelationships) {
        final GameData data = mctsData.getData();

        // TODO: add carriers to landing possibilities for non-enemy attacks
        // Find possible carrier landing territories
        final Set<Territory> possibleCarrierTerritories = new HashSet<>();
        if (isCheckingEnemyAttacks || !isCombatMove) {
            final Map<Unit, Set<Territory>> unitMoveMap2 = new HashMap<>();
            for (final Unit u : unitMoveMap2.keySet()) {
                if (Matches.unitIsCarrier().test(u)) {
                    possibleCarrierTerritories.addAll(unitMoveMap2.get(u));
                }
            }
            for (final Territory t : data.getMap().getTerritories()) {
                if (t.getUnitCollection().anyMatch(Matches.unitIsAlliedCarrier(player, data))) {
                    possibleCarrierTerritories.add(t);
                }
            }
        }

        for (final Territory myUnitTerritory : myUnitTerritories) {

            // Find my air units that have movement left
            final List<Unit> myAirUnits =
                    myUnitTerritory
                            .getUnitCollection()
                            .getMatches(ProMatches.unitCanBeMovedAndIsOwnedAir(player, isCombatMove));

            // Check each air unit individually since they can have different ranges
            for (final Unit myAirUnit : myAirUnits) {

                // Find range
                BigDecimal range = myAirUnit.getMovementLeft();
                if (isCheckingEnemyAttacks) {
                    range = new BigDecimal(UnitAttachment.get(myAirUnit.getType()).getMovement(player));
                    if (Matches.unitCanBeGivenBonusMovementByFacilitiesInItsTerritory(
                            myUnitTerritory, player, data)
                            .test(myAirUnit)) {
                        range = range.add(BigDecimal.ONE); // assumes bonus of +1 for now
                    }
                }

                // Find potential territories to move to
                Set<Territory> possibleMoveTerritories =
                        data.getMap()
                                .getNeighborsByMovementCost(
                                        myUnitTerritory,
                                        myAirUnit,
                                        range,
                                        ProMatches.territoryCanMoveAirUnits(player, data, isCombatMove));
                if (isIgnoringRelationships) {
                    possibleMoveTerritories =
                            data.getMap()
                                    .getNeighborsByMovementCost(
                                            myUnitTerritory,
                                            myAirUnit,
                                            range,
                                            ProMatches.territoryCanPotentiallyMoveAirUnits(player, data));
                }
                possibleMoveTerritories.add(myUnitTerritory);
                final Set<Territory> potentialTerritories =
                        new HashSet<>(
                                CollectionUtils.getMatches(possibleMoveTerritories, moveToTerritoryMatch));
                if (!isCombatMove && Matches.unitCanLandOnCarrier().test(myAirUnit)) {
                    potentialTerritories.addAll(
                            CollectionUtils.getMatches(
                                    possibleMoveTerritories, Matches.territoryIsInList(possibleCarrierTerritories)));
                }

                for (final Territory potentialTerritory : potentialTerritories) {

                    // Find route ignoring impassable and territories with AA
                    Predicate<Territory> canFlyOverMatch =
                            ProMatches.territoryCanMoveAirUnitsAndNoAa(player, data, isCombatMove);
                    if (isCheckingEnemyAttacks) {
                        canFlyOverMatch = ProMatches.territoryCanMoveAirUnits(player, data, isCombatMove);
                    }
                    final Route myRoute =
                            data.getMap()
                                    .getRouteForUnit(
                                            myUnitTerritory, potentialTerritory, canFlyOverMatch, myAirUnit, player);
                    if (myRoute == null) {
                        continue;
                    }
                    final BigDecimal myRouteLength = myRoute.getMovementCost(myAirUnit);
                    final BigDecimal remainingMoves = range.subtract(myRouteLength);
                    if (remainingMoves.compareTo(BigDecimal.ZERO) < 0) {
                        continue;
                    }

                    // Check if unit can land
                    if (isCombatMove
                            && (remainingMoves.compareTo(myRouteLength) < 0 || myUnitTerritory.isWater())) {
                        final Set<Territory> possibleLandingTerritories =
                                data.getMap()
                                        .getNeighborsByMovementCost(
                                                potentialTerritory, myAirUnit, remainingMoves, canFlyOverMatch);
                        final List<Territory> landingTerritories =
                                CollectionUtils.getMatches(
                                        possibleLandingTerritories,
                                        ProMatches.territoryCanLandAirUnits(
                                                player, data, isCombatMove, enemyTerritories, alliedTerritories));
                        List<Territory> carrierTerritories = new ArrayList<>();
                        if (Matches.unitCanLandOnCarrier().test(myAirUnit)) {
                            carrierTerritories =
                                    CollectionUtils.getMatches(
                                            possibleLandingTerritories,
                                            Matches.territoryIsInList(possibleCarrierTerritories));
                        }
                        if (landingTerritories.isEmpty() && carrierTerritories.isEmpty()) {
                            continue;
                        }
                    }

                    // Populate enemy territories with air unit
                    if (moveMap.containsKey(potentialTerritory)) {
                        moveMap.get(potentialTerritory).addMaxUnit(myAirUnit);
                    } else {
                        final ModelTerritory moveTerritoryData = new ModelTerritory(potentialTerritory, mctsData);
                        moveTerritoryData.addMaxUnit(myAirUnit);
                        moveMap.put(potentialTerritory, moveTerritoryData);
                    }

                    // Populate unit attack options map
                    if (unitMoveMap.containsKey(myAirUnit)) {
                        unitMoveMap.get(myAirUnit).add(potentialTerritory);
                    } else {
                        final Set<Territory> unitMoveTerritories = new HashSet<>();
                        unitMoveTerritories.add(potentialTerritory);
                        unitMoveMap.put(myAirUnit, unitMoveTerritories);
                    }
                }
            }
        }
    }

    private static void findBombardOptions(
            final MctsData mctsData,
            final GamePlayer player,
            final List<Territory> myUnitTerritories,
            final Map<Territory, ModelTerritory> moveMap,
            final Map<Unit, Set<Territory>> bombardMap,
            final List<ModelTransport> transportMapList,
            final boolean isCheckingEnemyAttacks) {
        final GameData data = mctsData.getData();

        // Find all transport unload from and to territories
        final Set<Territory> unloadFromTerritories = new HashSet<>();
        final Set<Territory> unloadToTerritories = new HashSet<>();
        for (final ModelTransport amphibData : transportMapList) {
            unloadFromTerritories.addAll(amphibData.getSeaTransportMap().keySet());
            unloadToTerritories.addAll(amphibData.getTransportMap().keySet());
        }

        // Loop through territories with my units
        for (final Territory myUnitTerritory : myUnitTerritories) {

            // Find my bombard units that have movement left
            final List<Unit> mySeaUnits =
                    myUnitTerritory
                            .getUnitCollection()
                            .getMatches(ProMatches.unitCanBeMovedAndIsOwnedBombard(player));

            // Check each sea unit individually since they can have different ranges
            for (final Unit mySeaUnit : mySeaUnits) {

                // Find range
                BigDecimal range = mySeaUnit.getMovementLeft();
                if (isCheckingEnemyAttacks) {
                    range = new BigDecimal(UnitAttachment.get(mySeaUnit.getType()).getMovement(player));
                    if (Matches.unitCanBeGivenBonusMovementByFacilitiesInItsTerritory(
                            myUnitTerritory, player, data)
                            .test(mySeaUnit)) {
                        range = range.add(BigDecimal.ONE); // assumes bonus of +1 for now
                    }
                }

                // Find list of potential territories to move to
                final Set<Territory> potentialTerritories =
                        data.getMap()
                                .getNeighborsByMovementCost(
                                        myUnitTerritory,
                                        mySeaUnit,
                                        range,
                                        ProMatches.territoryCanMoveSeaUnits(player, data, true));
                potentialTerritories.add(myUnitTerritory);
                potentialTerritories.retainAll(unloadFromTerritories);
                for (final Territory bombardFromTerritory : potentialTerritories) {

                    // Find route over water with no enemy units blocking
                    Route myRoute =
                            data.getMap()
                                    .getRouteForUnit(
                                            myUnitTerritory,
                                            bombardFromTerritory,
                                            ProMatches.territoryCanMoveSeaUnitsThrough(player, data, true),
                                            mySeaUnit,
                                            player);
                    if (isCheckingEnemyAttacks) {
                        myRoute =
                                data.getMap()
                                        .getRouteForUnit(
                                                myUnitTerritory,
                                                bombardFromTerritory,
                                                ProMatches.territoryCanMoveSeaUnits(player, data, true),
                                                mySeaUnit,
                                                player);
                    }
                    if (myRoute == null) {
                        continue;
                    }
                    final BigDecimal myRouteLength = myRoute.getMovementCost(mySeaUnit);
                    if (myRouteLength.compareTo(range) > 0) {
                        continue;
                    }

                    // Find potential unload to territories
                    final Set<Territory> bombardToTerritories =
                            new HashSet<>(data.getMap().getNeighbors(bombardFromTerritory));
                    bombardToTerritories.retainAll(unloadToTerritories);

                    // Populate attack territories with bombard unit
                    for (final Territory bombardToTerritory : bombardToTerritories) {
                        if (moveMap.containsKey(bombardToTerritory)) { // Should always contain it
                            moveMap.get(bombardToTerritory).addMaxBombardUnit(mySeaUnit);
                            moveMap.get(bombardToTerritory).addBombardOptionsMap(mySeaUnit, bombardFromTerritory);
                        }
                    }

                    // Populate bombard options map
                    if (bombardMap.containsKey(mySeaUnit)) {
                        bombardMap.get(mySeaUnit).addAll(bombardToTerritories);
                    } else {
                        bombardMap.put(mySeaUnit, bombardToTerritories);
                    }
                }
            }
        }
    }
}
