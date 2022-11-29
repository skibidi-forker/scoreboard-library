package net.megavex.scoreboardlibrary.implementation.sidebar.line;

import net.kyori.adventure.text.Component;
import net.megavex.scoreboardlibrary.implementation.packetAdapter.TeamsPacketAdapter;
import net.megavex.scoreboardlibrary.implementation.sidebar.BetterAbstractSidebar;
import org.bukkit.ChatColor;

public class GlobalLineInfo {
  private static final String[] lineColors = new String[15];

  static {
    var values = ChatColor.values();
    for (int i = 0; i < lineColors.length; i++) {
      lineColors[i] = values[i].toString();
    }
  }

  public final int line;
  public final TeamsPacketAdapter<?, ?> bridge;
  public Component value;
  public int objectiveScore;
  public boolean update, updateTeams, updateScore;

  public GlobalLineInfo(BetterAbstractSidebar sidebar, int line) {
    this.line = line;
    this.bridge = sidebar.scoreboardLibrary().packetAdapter.createTeamPacketAdapter("_l" + line);
  }

  public String player() {
    return lineColors[line];
  }
}
