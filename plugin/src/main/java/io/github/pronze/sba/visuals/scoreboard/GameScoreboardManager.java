package io.github.pronze.sba.visuals.scoreboard;

import io.github.pronze.sba.SBA;
import io.github.pronze.sba.config.SBAConfig;
import io.github.pronze.sba.data.GamePlayerData;
import io.github.pronze.sba.game.GameManagerImpl;
import io.github.pronze.sba.game.GamePlayerImpl;
import io.github.pronze.sba.lang.LangKeys;
import io.github.pronze.sba.service.DateProviderService;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.screamingsandals.bedwars.api.RunningTeam;
import org.screamingsandals.bedwars.api.Team;
import org.screamingsandals.bedwars.api.events.BedwarsGameEndEvent;
import org.screamingsandals.bedwars.api.events.BedwarsGameStartedEvent;
import org.screamingsandals.bedwars.api.events.BedwarsPlayerJoinedEvent;
import org.screamingsandals.bedwars.api.events.BedwarsPlayerLeaveEvent;
import org.screamingsandals.bedwars.api.game.Game;
import org.screamingsandals.bedwars.api.game.GameStatus;
import org.screamingsandals.bedwars.game.TeamColor;
import org.screamingsandals.lib.lang.Message;
import org.screamingsandals.lib.player.PlayerMapper;
import org.screamingsandals.lib.player.PlayerWrapper;
import org.screamingsandals.lib.sidebar.Sidebar;
import org.screamingsandals.lib.tasker.Tasker;
import org.screamingsandals.lib.tasker.TaskerTime;
import org.screamingsandals.lib.tasker.task.TaskBase;
import org.screamingsandals.lib.utils.AdventureHelper;
import org.screamingsandals.lib.utils.annotations.Service;
import org.screamingsandals.lib.utils.annotations.methods.OnPostEnable;
import org.screamingsandals.lib.utils.annotations.methods.OnPreDisable;
import org.screamingsandals.lib.utils.logger.LoggerWrapper;
import java.util.*;

@RequiredArgsConstructor
@Service
public final class GameScoreboardManager implements Listener {
    private final Map<Game, Sidebar> sidebarMap = new HashMap<>();
    private final LoggerWrapper logger;
    private final SBA plugin;
    private final SBAConfig config;
    private final DateProviderService dateProviderService;
    private final GameManagerImpl gameManager;

    @OnPostEnable
    public void onPostEnable() {
        if (!config.node("game-scoreboard", "enabled").getBoolean(true)) {
            return;
        }

        plugin.registerListener(this);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBedWarsGameStartedEvent(BedwarsGameStartedEvent event) {
        final var game = (org.screamingsandals.bedwars.game.Game) event.getGame();
        final var maybeWrappedGame = gameManager.getWrappedGame(game);
        if (maybeWrappedGame.isEmpty()) {
            return;
        }

        final var gameWrapper = maybeWrappedGame.get();
        final var sidebar = Sidebar.of();
        final var title = config
                .node("game-scoreboard", "title")
                .getString("&e&lBED WARS");
        sidebar.title(AdventureHelper.toComponent(title));

        Tasker.build(taskBase -> new SidebarTeamUpdateTask(game, sidebar, taskBase))
                .repeat(1L, TaskerTime.SECONDS)
                .start();

        Message scoreboardLines;
        if (game.countAvailableTeams() > 5) {
            scoreboardLines = Message.of(LangKeys.SCOREBOARD_LINES_5);
        } else {
            scoreboardLines = Message.of(LangKeys.SCOREBOARD_LINES_DEFAULT);
        }

        //final var generatorTask = game.getTask(GeneratorTask.class).orElseThrow();
        scoreboardLines
                .placeholder("beds", wrapper -> gameWrapper
                        .getPlayerData(wrapper.as(GamePlayerImpl.class).getUuid())
                        .map(GamePlayerData::getBedDestroys)
                        .map(String::valueOf)
                        .map(AdventureHelper::toComponent)
                        .orElse(null))
                .placeholder("deaths", wrapper -> gameWrapper
                        .getPlayerData(wrapper.as(GamePlayerImpl.class).getUuid())
                        .map(GamePlayerData::getDeaths)
                        .map(String::valueOf)
                        .map(AdventureHelper::toComponent)
                        .orElse(null))
                .placeholder("finalkills", wrapper -> gameWrapper
                        .getPlayerData(wrapper.as(GamePlayerImpl.class).getUuid())
                        .map(GamePlayerData::getFinalKills)
                        .map(String::valueOf)
                        .map(AdventureHelper::toComponent)
                        .orElse(null))
                .placeholder("kills", wrapper -> gameWrapper
                        .getPlayerData(wrapper.as(GamePlayerImpl.class).getUuid())
                        .map(GamePlayerData::getKills)
                        .map(String::valueOf)
                        .map(AdventureHelper::toComponent)
                        .orElse(null))
                .placeholder("time", () -> AdventureHelper.toComponent(game.getFormattedTimeLeft()))
                .placeholder("game", game.getName())
                .placeholder("date", dateProviderService.getFormattedDate())
                .placeholder("team_bed_status", wrapper -> Optional
                        .ofNullable(game.getTeamOfPlayer(wrapper.as(Player.class)))
                        .map(this::getTeamBedStatus)
                        .map(AdventureHelper::toComponent)
                        .orElse(Component.empty()))
                // TODO:
              //.placeholder("tier", generatorTask.getNextTierName().replace("-", " ") + " in §a" + generatorTask.getTimeLeftForNextEvent())
                .placeholder("sba_version", plugin.getPluginDescription().getVersion())
                .placeholder("team_status", wrapper -> {
                    final var playerWrapper = wrapper.as(GamePlayerImpl.class);
                    final var playerTeam = game.getTeamOfPlayer(playerWrapper.as(Player.class));

                    var component = Component.empty();
                    for (int i = 0; i < 5; i++) {
                        if (game.getAvailableTeams().size() <= i) {
                            break;
                        }

                        var team = game.getAvailableTeams().get(i);
                        String you = "";
                        if (playerTeam != null
                                && playerTeam.getName().equalsIgnoreCase(team.getName())) {
                            you = AdventureHelper.toLegacy(Message.of(LangKeys.SCOREBOARD_YOU_MESSAGE).asComponent());
                        }

                        component = component.append(
                                AdventureHelper.toComponent("\n" + getTeamStatusFormat(game, team).replace("%you%", you))
                        );
                    }

                    return component;
                });

        sidebar.bottomLine(scoreboardLines);
        sidebarMap.put(game, sidebar);

        game.getConnectedPlayers()
                .stream()
                .map(PlayerMapper::wrapPlayer)
                .forEach(sidebar::addViewer);
    }

    private void addViewer(@NotNull PlayerWrapper playerWrapper, @NotNull Game game) {
        if (!sidebarMap.containsKey(game)) {
            return;
        }

        final var sidebar = sidebarMap.get(game);
        sidebar.addViewer(playerWrapper);
    }

    private void removeViewer(@NotNull PlayerWrapper playerWrapper) {
        logger.trace("Removing viewer: {} from game scoreboard!", playerWrapper.getName());
        sidebarMap.values()
                .stream()
                .filter(sidebar -> sidebar.getViewers().contains(playerWrapper))
                .forEach(sidebar -> sidebar.removeViewer(playerWrapper));
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void onBedWarsPlayerLeaveEvent(BedwarsPlayerLeaveEvent event) {
        final var game = event.getGame();

        if (game.getStatus() != GameStatus.RUNNING
                || !sidebarMap.containsKey(game)) {
            return;
        }

        removeViewer(PlayerMapper.wrapPlayer(event.getPlayer()));
    }

    @EventHandler
    public void onBedWarsPlayerJoinedEvent(BedwarsPlayerJoinedEvent event) {
        final var game = event.getGame();

        if (game.getStatus() != GameStatus.RUNNING
                || !sidebarMap.containsKey(game)) {
            return;
        }

        // joined as spectator, let's give him a scoreboard.
        addViewer(PlayerMapper.wrapPlayer(event.getPlayer()), game);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBedWarsGameEndEvent(BedwarsGameEndEvent event) {
        sidebarMap.computeIfPresent(event.getGame(), ((game, sidebar) -> {
            sidebar.destroy();
            return null;
        }));
    }

    @OnPreDisable
    public void onPreDisable() {
        sidebarMap
                .values()
                .forEach(Sidebar::destroy);
        sidebarMap.clear();
    }

    private String getTeamBedStatus(@NotNull RunningTeam team) {
        return team.isDead() ?
                config.node("game-scoreboard", "team-status", "bed-destroyed").getString("§c\u2717") :
                config.node("game-scoreboard", "team-status", "bed-exists").getString("§a\u2713");
    }

    private String getTeamStatusFormat(@NotNull RunningTeam team) {
        String alive = config
                .node("game-scoreboard", "team-status", "alive")
                .getString("%color% %team% §a\u2713 §8%you%");

        String destroyed = config
                .node("game-scoreboard", "team-status", "destroyed")
                .getString("%color% %team% §a§f%players%§8 %you%");

        String status = team.isTargetBlockExists() ? alive : destroyed;

        String formattedTeam =  TeamColor.fromApiColor(team.getColor()).chatColor.name() + team.getName().charAt(0);

        return status
                .replace("%bed_status%", getTeamBedStatus(team))
                .replace("%color%", formattedTeam)
                .replace("%team%", ChatColor.WHITE + team.getName() + ":")
                .replace("%players%", ChatColor.GREEN.toString() + team.getConnectedPlayers().size());
    }

    private String getTeamStatusFormat(Game game, Team team) {
        return game
                .getRunningTeams()
                .stream()
                .filter(t -> t.getName().equalsIgnoreCase(team.getName()))
                .map(this::getTeamStatusFormat)
                .findAny()
                .orElseGet(() -> {
                    final var destroyed = config
                            .node("team-status", "eliminated")
                            .getString("%color% %team% §c\u2718 %you%");

                    String formattedTeam =  TeamColor.fromApiColor(team.getColor()).chatColor.name() + team.getName().charAt(0);

                    return destroyed
                            .replace("%color%", formattedTeam)
                            .replace("%team%", ChatColor.WHITE
                                    + team.getName() + ":");
                });
    }

    @RequiredArgsConstructor
    private static class SidebarTeamUpdateTask implements Runnable {
        private final Game game;
        private final Sidebar sidebar;
        private final TaskBase taskBase;

        @Override
        public void run() {
            if (game.getStatus() != GameStatus.RUNNING) {
                taskBase.cancel();
                return;
            }

            game.getRunningTeams().forEach(team -> {
                final var teamColor = TeamColor.fromApiColor(team.getColor()).chatColor;
                if (sidebar.getTeam(team.getName()).isEmpty()) {
                    sidebar.team(team.getName())
                            .friendlyFire(false)
                            .color(NamedTextColor.NAMES.value(teamColor.name().toLowerCase()));
                }

                var sidebarTeam = sidebar.getTeam(team.getName()).orElseThrow();

                List.copyOf(sidebarTeam.players())
                        .forEach(teamPlayer -> {
                            if (team.getConnectedPlayers().stream().noneMatch(bedWarsPlayer -> bedWarsPlayer.equals(teamPlayer.as(Player.class)))) {
                                sidebarTeam.removePlayer(teamPlayer);
                            }
                        });

                team.getConnectedPlayers()
                        .stream()
                        .map(PlayerMapper::wrapPlayer)
                        .filter(teamPlayer -> !sidebarTeam.players().contains(teamPlayer))
                        .forEach(sidebarTeam::player);
            });
        }
    }
}