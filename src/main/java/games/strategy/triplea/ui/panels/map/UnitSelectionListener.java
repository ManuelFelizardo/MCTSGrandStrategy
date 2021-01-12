package games.strategy.triplea.ui.panels.map;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ui.MouseDetails;
import java.util.List;

public interface UnitSelectionListener {
  /**
   * Note, if the mouse is clicked where there are no units, units will be empty but territory will
   * still be correct.
   */
  void unitsSelected(List<Unit> units, Territory territory, MouseDetails md);
}
