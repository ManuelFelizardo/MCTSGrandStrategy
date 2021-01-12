package games.strategy.triplea.ai.mctsclean.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MoveDescription;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.ai.AbstractAi;
import games.strategy.triplea.ai.mctsclean.algorithm.Mcts;
import games.strategy.triplea.ai.mctsclean.algorithm.NewAction;
import games.strategy.triplea.ai.mctsclean.oepMcts.OepAction;
import games.strategy.triplea.ai.mctsclean.oepMcts.OepAction2;
import games.strategy.triplea.ai.pro.logging.ProLogger;
import games.strategy.triplea.ai.pro.util.ProMatches;
import games.strategy.triplea.ai.weak.WeakAi;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.triplea.util.Tuple;

public class ForwardModel {

    public static WeakAi mctsAi;

    private ForwardModel(){

    };

    public static void doMove(
        final GameData data, final List<MoveDescription> moves, final IMoveDelegate moveDel, boolean isSimulation) {
        if(isSimulation){
            final IDelegateBridge bridge =new MctsDummyDelegateBridge(mctsAi, data.getSequence().getStep().getPlayerId(), data);
            moveDel.setDelegateBridgeAndPlayer(bridge);
        }

        final boolean noTransportLoads =
            moves.stream().allMatch(move -> move.getUnitsToTransports().isEmpty());

        if (noTransportLoads) {
            for (int i = 0; i < moves.size(); i++) {
                final Route r = moves.get(i).getRoute();
                for (int j = i + 1; j < moves.size(); j++) {
                    final Route r2 = moves.get(j).getRoute();
                    if (r.equals(r2)) {
                        final var mergedUnits = new ArrayList<Unit>();
                        mergedUnits.addAll(moves.get(j).getUnits());
                        mergedUnits.addAll(moves.get(i).getUnits());
                        moves.set(j, new MoveDescription(mergedUnits, r));
                        moves.remove(i);
                        i--;
                        break;
                    }
                }
            }
        }

        for (final MoveDescription move : moves) {
            final String result = moveDel.performMove(move);
            if (!isSimulation) {
                AbstractAi.movePause();
            }
        }
    }

    public static List<MoveDescription> calculateMoveRoutes(
        final MctsData mctsData,
        final GamePlayer player,
        final ArrayList<NewAction> actions) {

        final GameData data = mctsData.getData();
        final GameMap map = data.getMap();
        boolean isCombatMove;
        if (data.getSequence().getStep().getName().endsWith("NonCombatMove")){
            isCombatMove=false;
        } else {
            isCombatMove=true;
        }

        final var moves = new ArrayList<MoveDescription>();
        // Loop through each unit that is attacking the current territory
        Tuple<Territory, Unit> lastLandTransport = Tuple.of(null, null);
        for(NewAction a:actions) {
            for (final Unit u : a.getUnits()) {


                // Skip if unit is already in move to territory
                final Territory startTerritory = mctsData.getUnitTerritory(u);
                if (startTerritory == null || startTerritory.equals(a.getT())) {
                    continue;
                }

                // Add unit to move list
                final var unitList = new ArrayList<Unit>();
                unitList.add(u);
                if (Matches.unitIsLandTransport().test(u)) {
                    lastLandTransport = Tuple.of(startTerritory, u);
                }

                // Determine route and add to move list
                Route route = null;

                if (!unitList.isEmpty() && unitList.stream().allMatch(Matches.unitIsLand())) {

                    // Land unit
                    route =
                        map.getRouteForUnit(
                            startTerritory,
                            a.getT(),
                            ProMatches.territoryCanMoveLandUnitsThrough(
                                player, data, u, startTerritory, isCombatMove, List.of()),
                            u,
                            player);
                    if (route == null && startTerritory.equals(lastLandTransport.getFirst())) {
                        route =
                            map.getRouteForUnit(
                                startTerritory,
                                a.getT(),
                                ProMatches.territoryCanMoveLandUnitsThrough(
                                    player,
                                    data,
                                    lastLandTransport.getSecond(),
                                    startTerritory,
                                    isCombatMove,
                                    List.of()),
                                u,
                                player);
                    }
                } else if (!unitList.isEmpty() && unitList.stream().allMatch(Matches.unitIsAir())) {

                    // Air unit
                    route =
                        map.getRouteForUnit(
                            startTerritory,
                            a.getT(),
                            ProMatches.territoryCanMoveAirUnitsAndNoAa(player, data, isCombatMove),
                            u,
                            player);
                }
                if (route == null) {
                    ProLogger.warn(
                        data.getSequence().getRound()
                            + "-"
                            + data.getSequence().getStep().getName()
                            + ": route is null (could not calculate route)"
                            + startTerritory
                            + " to "
                            + a.getT()
                            + ", units="
                            + unitList);
                } else {
                    moves.add(new MoveDescription(unitList, route));
                }
            }

        }

        return moves;
    }

    public static List<MoveDescription> calculateMoveRoutesOep2(
        final MctsData mctsData,
        final GamePlayer player,
        final ArrayList<OepAction2> actions) {

        final GameData data = mctsData.getData();
        final GameMap map = data.getMap();
        boolean isCombatMove;
        if (data.getSequence().getStep().getName().endsWith("NonCombatMove")){
            isCombatMove=false;
        } else {
            isCombatMove=true;
        }

        final var moves = new ArrayList<MoveDescription>();
        // Loop through each unit that is attacking the current territory
        Tuple<Territory, Unit> lastLandTransport = Tuple.of(null, null);
        for(OepAction2 a:actions) {
            for (final Unit u : a.getUnits()) {


                // Skip if unit is already in move to territory
                final Territory startTerritory = mctsData.getUnitTerritory(u);
                if (startTerritory == null || startTerritory.equals(a.getT())) {
                    continue;
                }

                // Add unit to move list
                final var unitList = new ArrayList<Unit>();
                unitList.add(u);
                if (Matches.unitIsLandTransport().test(u)) {
                    lastLandTransport = Tuple.of(startTerritory, u);
                }

                // Determine route and add to move list
                Route route = null;

                if (!unitList.isEmpty() && unitList.stream().allMatch(Matches.unitIsLand())) {

                    // Land unit
                    route =
                        map.getRouteForUnit(
                            startTerritory,
                            a.getT(),
                            ProMatches.territoryCanMoveLandUnitsThrough(
                                player, data, u, startTerritory, isCombatMove, List.of()),
                            u,
                            player);
                    if (route == null && startTerritory.equals(lastLandTransport.getFirst())) {
                        route =
                            map.getRouteForUnit(
                                startTerritory,
                                a.getT(),
                                ProMatches.territoryCanMoveLandUnitsThrough(
                                    player,
                                    data,
                                    lastLandTransport.getSecond(),
                                    startTerritory,
                                    isCombatMove,
                                    List.of()),
                                u,
                                player);
                    }
                } else if (!unitList.isEmpty() && unitList.stream().allMatch(Matches.unitIsAir())) {

                    // Air unit
                    route =
                        map.getRouteForUnit(
                            startTerritory,
                            a.getT(),
                            ProMatches.territoryCanMoveAirUnitsAndNoAa(player, data, isCombatMove),
                            u,
                            player);
                }
                if (route == null) {
                    ProLogger.warn(
                        data.getSequence().getRound()
                            + "-"
                            + data.getSequence().getStep().getName()
                            + ": route is null (could not calculate route)"
                            + startTerritory
                            + " to "
                            + a.getT()
                            + ", units="
                            + unitList);
                } else {
                    moves.add(new MoveDescription(unitList, route));
                }
            }

        }

        return moves;
    }

    public static List<MoveDescription> calculateMoveRoutesOep(
        final MctsData mctsData,
        final GamePlayer player,
        GameData data,
        final ArrayList<OepAction> actions) {
        //long time= System.currentTimeMillis();
        final GameMap map = data.getMap();
        boolean isCombatMove;
        if (data.getSequence().getStep().getName().endsWith("NonCombatMove")){
            isCombatMove=false;
        } else {
            isCombatMove=true;
        }

        final var moves = new ArrayList<MoveDescription>();
        // Loop through each unit that is attacking the current territory
        Tuple<Territory, Unit> lastLandTransport = Tuple.of(null, null);
        for(OepAction a:actions) {
            a=a.replace(data);
            Unit u=a.getU();

                // Skip if unit is already in move to territory
                final Territory startTerritory = mctsData.getUnitTerritory(u);
                if (startTerritory == null || startTerritory.equals(a.getT())) {
                    continue;
                }

                // Add unit to move list
                final var unitList = new ArrayList<Unit>();
                unitList.add(u);
                if (Matches.unitIsLandTransport().test(u)) {
                    lastLandTransport = Tuple.of(startTerritory, u);
                }

                // Determine route and add to move list
                Route route = null;

                if (!unitList.isEmpty() && unitList.stream().allMatch(Matches.unitIsLand())) {

                    // Land unit
                    route =
                        map.getRouteForUnit(
                            startTerritory,
                            a.getT(),
                            ProMatches.territoryCanMoveLandUnitsThrough(
                                player, data, u, startTerritory, isCombatMove, List.of()),
                            u,
                            player);
                    if (route == null && startTerritory.equals(lastLandTransport.getFirst())) {
                        route =
                            map.getRouteForUnit(
                                startTerritory,
                                a.getT(),
                                ProMatches.territoryCanMoveLandUnitsThrough(
                                    player,
                                    data,
                                    lastLandTransport.getSecond(),
                                    startTerritory,
                                    isCombatMove,
                                    List.of()),
                                u,
                                player);
                    }
                } else if (!unitList.isEmpty() && unitList.stream().allMatch(Matches.unitIsAir())) {

                    // Air unit
                    route =
                        map.getRouteForUnit(
                            startTerritory,
                            a.getT(),
                            ProMatches.territoryCanMoveAirUnitsAndNoAa(player, data, isCombatMove),
                            u,
                            player);
                }
                if (route == null) {
                    ProLogger.warn(
                        data.getSequence().getRound()
                            + "-"
                            + data.getSequence().getStep().getName()
                            + ": route is null (could not calculate route)"
                            + startTerritory
                            + " to "
                            + a.getT()
                            + ", units="
                            + unitList);
                } else {
                    moves.add(new MoveDescription(unitList, route));
                }
            }

        //ProLogger.info("calculate routes time - "+(System.currentTimeMillis()-time));
        return moves;
    }

    public static List<MoveDescription> calculateMoveRoutesNew(
        final MctsData mctsData,
        final GamePlayer player,
        final Territory t,
        Set<Unit> units) {

        final GameData data = mctsData.getData();
        final GameMap map = data.getMap();
        boolean isCombatMove;
        if (data.getSequence().getStep().getName().endsWith("NonCombatMove")){
            isCombatMove=false;
        } else {
            isCombatMove=true;
        }

        final var moves = new ArrayList<MoveDescription>();
        // Loop through each unit that is attacking the current territory
        Tuple<Territory, Unit> lastLandTransport = Tuple.of(null, null);
        for (final Unit u : units) {



            // Skip if unit is already in move to territory
            final Territory startTerritory = mctsData.getUnitTerritory(u);
            if (startTerritory == null || startTerritory.equals(t)) {
                continue;
            }

            // Add unit to move list
            final var unitList = new ArrayList<Unit>();
            unitList.add(u);
            if (Matches.unitIsLandTransport().test(u)) {
                lastLandTransport = Tuple.of(startTerritory, u);
            }

            // Determine route and add to move list
            Route route = null;
            if (!unitList.isEmpty() && unitList.stream().allMatch(Matches.unitIsLand())) {

                // Land unit
                route =
                    map.getRouteForUnit(
                        startTerritory,
                        t,
                        ProMatches.territoryCanMoveLandUnitsThrough(
                            player, data, u, startTerritory, isCombatMove, List.of()),
                        u,
                        player);
                if (route == null && startTerritory.equals(lastLandTransport.getFirst())) {
                    route =
                        map.getRouteForUnit(
                            startTerritory,
                            t,
                            ProMatches.territoryCanMoveLandUnitsThrough(
                                player,
                                data,
                                lastLandTransport.getSecond(),
                                startTerritory,
                                isCombatMove,
                                List.of()),
                            u,
                            player);
                }
            } else if (!unitList.isEmpty() && unitList.stream().allMatch(Matches.unitIsAir())) {

                // Air unit
                route =
                    map.getRouteForUnit(
                        startTerritory,
                        t,
                        ProMatches.territoryCanMoveAirUnitsAndNoAa(player, data, isCombatMove),
                        u,
                        player);
            }
            if (route == null) {
                ProLogger.warn(
                    data.getSequence().getRound()
                        + "-"
                        + data.getSequence().getStep().getName()
                        + ": route is null (could not calculate route)"
                        + startTerritory
                        + " to "
                        + t
                        + ", units="
                        + unitList);
            } else {
                moves.add(new MoveDescription(unitList, route));
            }
        }
        return moves;
    }

    public static List<MoveDescription> calculateMoveRoutesNew2(
        final GameData data,
        final MctsData mctsData,
        final GamePlayer player,
        final Territory t,
        Set<Unit> units) {

        final GameMap map = data.getMap();
        boolean isCombatMove;
        if (data.getSequence().getStep().getName().endsWith("NonCombatMove")){
            isCombatMove=false;
        } else {
            isCombatMove=true;
        }

        final var moves = new ArrayList<MoveDescription>();
        // Loop through each unit that is attacking the current territory
        Tuple<Territory, Unit> lastLandTransport = Tuple.of(null, null);
        for (final Unit u : units) {



            // Skip if unit is already in move to territory
            final Territory startTerritory = mctsData.getUnitTerritory(u);
            if (startTerritory == null || startTerritory.equals(t)) {
                continue;
            }

            // Add unit to move list
            final var unitList = new ArrayList<Unit>();
            unitList.add(u);
            if (Matches.unitIsLandTransport().test(u)) {
                lastLandTransport = Tuple.of(startTerritory, u);
            }

            // Determine route and add to move list
            Route route = null;
            if (!unitList.isEmpty() && unitList.stream().allMatch(Matches.unitIsLand())) {

                // Land unit
                route =
                    map.getRouteForUnit(
                        startTerritory,
                        t,
                        ProMatches.territoryCanMoveLandUnitsThrough(
                            player, data, u, startTerritory, isCombatMove, List.of()),
                        u,
                        player);
                if (route == null && startTerritory.equals(lastLandTransport.getFirst())) {
                    route =
                        map.getRouteForUnit(
                            startTerritory,
                            t,
                            ProMatches.territoryCanMoveLandUnitsThrough(
                                player,
                                data,
                                lastLandTransport.getSecond(),
                                startTerritory,
                                isCombatMove,
                                List.of()),
                            u,
                            player);
                }
            } else if (!unitList.isEmpty() && unitList.stream().allMatch(Matches.unitIsAir())) {

                // Air unit
                route =
                    map.getRouteForUnit(
                        startTerritory,
                        t,
                        ProMatches.territoryCanMoveAirUnitsAndNoAa(player, data, isCombatMove),
                        u,
                        player);
            }
            if (route == null) {
                ProLogger.warn(
                    data.getSequence().getRound()
                        + "-"
                        + data.getSequence().getStep().getName()
                        + ": route is null (could not calculate route)"
                        + startTerritory
                        + " to "
                        + t
                        + ", units="
                        + unitList);
            } else {
                moves.add(new MoveDescription(unitList, route));
            }
        }
        return moves;
    }


}
