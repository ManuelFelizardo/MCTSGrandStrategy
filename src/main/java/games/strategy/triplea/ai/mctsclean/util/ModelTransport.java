package games.strategy.triplea.ai.mctsclean.util;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Getter;

@Getter
public class ModelTransport {
    private final Unit transport;
    private final Map<Territory, Set<Territory>> transportMap;
    private final Map<Territory, Set<Territory>> seaTransportMap;

    ModelTransport(final Unit transport) {
        this.transport = transport;
        transportMap = new HashMap<>();
        seaTransportMap = new HashMap<>();
    }

    void addTerritories(
            final Set<Territory> attackTerritories, final Set<Territory> myUnitsToLoadTerritories) {
        for (final Territory attackTerritory : attackTerritories) {
            if (transportMap.containsKey(attackTerritory)) {
                transportMap.get(attackTerritory).addAll(myUnitsToLoadTerritories);
            } else {
                final Set<Territory> territories = new HashSet<>(myUnitsToLoadTerritories);
                transportMap.put(attackTerritory, territories);
            }
        }
    }

    void addSeaTerritories(
            final Set<Territory> attackTerritories, final Set<Territory> myUnitsToLoadTerritories) {
        for (final Territory attackTerritory : attackTerritories) {
            if (seaTransportMap.containsKey(attackTerritory)) {
                seaTransportMap.get(attackTerritory).addAll(myUnitsToLoadTerritories);
            } else {
                final Set<Territory> territories = new HashSet<>(myUnitsToLoadTerritories);
                seaTransportMap.put(attackTerritory, territories);
            }
        }
    }
}
