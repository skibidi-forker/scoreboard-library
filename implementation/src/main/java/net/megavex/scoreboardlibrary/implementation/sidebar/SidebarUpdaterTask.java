package net.megavex.scoreboardlibrary.implementation.sidebar;

import com.google.common.base.Preconditions;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.megavex.scoreboardlibrary.implementation.ScoreboardLibraryImpl;
import org.bukkit.scheduler.BukkitRunnable;

public class SidebarUpdaterTask extends BukkitRunnable {
  private final Logger logger;
  private final Set<AbstractSidebar> sidebars;

  public SidebarUpdaterTask(ScoreboardLibraryImpl manager) {
    Preconditions.checkNotNull(manager);
    this.logger = manager.plugin().getLogger();
    this.sidebars = manager.sidebars;

    runTaskTimerAsynchronously(manager.plugin(), 1, 1);
  }

  @Override
  public void run() {
    for (var sidebar : sidebars) {
      try {
        sidebar.tick();
      } catch (Exception e) {
        logger.log(Level.WARNING, "an error occured while updating Sidebar", e);
      }
    }
  }
}
