package net.megavex.scoreboardlibrary.implementation;

import com.google.common.base.Preconditions;
import com.google.common.collect.MapMaker;
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary;
import net.megavex.scoreboardlibrary.api.exception.NoPacketAdapterAvailableException;
import net.megavex.scoreboardlibrary.api.sidebar.Sidebar;
import net.megavex.scoreboardlibrary.implementation.commons.LocaleProvider;
import net.megavex.scoreboardlibrary.implementation.listener.LocaleListener;
import net.megavex.scoreboardlibrary.implementation.packetAdapter.ScoreboardLibraryPacketAdapter;
import net.megavex.scoreboardlibrary.implementation.sidebar.AbstractSidebar;
import net.megavex.scoreboardlibrary.implementation.sidebar.PlayerDependantLocaleSidebar;
import net.megavex.scoreboardlibrary.implementation.sidebar.SidebarUpdaterTask;
import net.megavex.scoreboardlibrary.implementation.sidebar.SingleLocaleSidebar;
import net.megavex.scoreboardlibrary.implementation.team.TeamManagerImpl;
import net.megavex.scoreboardlibrary.implementation.team.TeamUpdaterTask;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ScoreboardLibraryImpl implements ScoreboardLibrary {
  public final Plugin plugin;
  public final ScoreboardLibraryPacketAdapter<?> packetAdapter;
  public final LocaleProvider localeProvider;

  private final Map<Player, ScoreboardLibraryPlayer> playerMap = new MapMaker().weakKeys().makeMap();

  public volatile Set<TeamManagerImpl> teamManagers;
  public volatile Set<AbstractSidebar> sidebars;

  public LocaleListener localeListener;
  public boolean closed;
  public TeamUpdaterTask teamTask;
  public SidebarUpdaterTask sidebarTask;

  private final Object lock = new Object();

  public ScoreboardLibraryImpl(Plugin plugin) throws NoPacketAdapterAvailableException {
    Preconditions.checkNotNull(plugin, "plugin");

    try {
      Class.forName("net.kyori.adventure.Adventure");
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Adventure is not in the classpath");
    }

    this.plugin = plugin;
    this.packetAdapter = PacketAdapterLoader.loadPacketAdapter();
    this.localeProvider = this.packetAdapter.localeProvider;

    try {
      Class.forName("org.bukkit.event.player.PlayerLocaleChangeEvent");
      this.localeListener = new LocaleListener(this);
      plugin.getServer().getPluginManager().registerEvents(this.localeListener, plugin);
    } catch (ClassNotFoundException ignored) {
    }
  }

  @Override
  public @NotNull Plugin plugin() {
    return plugin;
  }

  @Override
  public @NotNull Sidebar createSidebar(int maxLines, @Nullable Locale locale) {
    checkClosed();

    if (maxLines <= 0 || maxLines > Sidebar.MAX_LINES) {
      throw new IllegalArgumentException("maxLines");
    }

    AbstractSidebar sidebar;
    if (locale == null) {
      sidebar = new PlayerDependantLocaleSidebar(this, maxLines);
    } else {
      sidebar = new SingleLocaleSidebar(this, maxLines, locale);
    }

    getSidebars0().add(sidebar);
    return sidebar;
  }

  @Override
  public @NotNull TeamManagerImpl createTeamManager() {
    checkClosed();

    var teamManager = new TeamManagerImpl(this);
    getTeamManagers0().add(teamManager);
    return teamManager;
  }

  @Override
  public void close() {
    if (closed)
      return;

    closed = true;

    if (teamManagers != null) {
      teamTask.cancel();
      for (var teamManager : teamManagers) {
        teamManager.close();
      }
    }

    if (sidebars != null) {
      sidebarTask.cancel();
      for (var sidebar : List.copyOf(sidebars)) {
        sidebar.close();
      }
    }
  }

  @Override
  public boolean closed() {
    return closed;
  }

  public Set<TeamManagerImpl> getTeamManagers0() {
    if (this.teamManagers == null) {
      synchronized (this.lock) {
        if (this.teamManagers == null) {
          this.teamManagers = ConcurrentHashMap.newKeySet(4);
          this.teamTask = new TeamUpdaterTask(this);
        }
      }
    }

    return this.teamManagers;
  }

  public Set<AbstractSidebar> getSidebars0() {
    if (this.sidebars == null) {
      synchronized (this.lock) {
        if (this.sidebars == null) {
          this.sidebars = ConcurrentHashMap.newKeySet(4);
          this.sidebarTask = new SidebarUpdaterTask(this);
        }
      }
    }

    return this.sidebars;
  }

  public @NotNull ScoreboardLibraryPlayer getOrCreatePlayer(@NotNull Player player) {
    return playerMap.computeIfAbsent(player, ScoreboardLibraryPlayer::new);
  }

  public @Nullable ScoreboardLibraryPlayer getPlayer(@NotNull Player player) {
    System.out.println(playerMap);
    return playerMap.get(player);
  }

  private void checkClosed() {
    if (closed) {
      throw new IllegalStateException("ScoreboardLibrary is closed");
    }
  }
}
