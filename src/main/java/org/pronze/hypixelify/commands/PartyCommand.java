package org.pronze.hypixelify.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.pronze.hypixelify.Hypixelify;
import org.pronze.hypixelify.database.PlayerDatabase;
import org.pronze.hypixelify.party.Party;
import org.pronze.hypixelify.utils.ShopUtil;
import org.screamingsandals.bedwars.api.BedwarsAPI;
import org.screamingsandals.bedwars.api.game.Game;
import org.screamingsandals.bedwars.api.game.GameStatus;

import javax.xml.crypto.Data;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class PartyCommand implements TabExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can access this command");
            return true;
        }

        if (!Hypixelify.getConfigurator().config.getBoolean("party.enabled", true)) {
            sender.sendMessage("Cannot access command, party system is disabled.");
        }
        Player player = (Player) sender;

        if (args == null || args.length == 0 || args.length > 2) {
            for (String str : Hypixelify.getConfigurator().config.getStringList("party.message.invalid-command")) {
                player.sendMessage(ShopUtil.translateColors(str));
            }
            return true;
        }

        final HashMap<UUID, PlayerDatabase> Database = Hypixelify.getInstance().playerData;

        if (args[0].equalsIgnoreCase("invite")) {
            if (args.length == 2) {

                if (Database.get(player.getUniqueId()) != null) {
                    PlayerDatabase data = Database.get(player.getUniqueId());
                    int max_sz = Hypixelify.getConfigurator().config.getInt("party.size", 4);
                    if (data.isInParty() && Hypixelify.getInstance().partyManager.parties.get(data.getPartyLeader()) != null && Hypixelify.getInstance().partyManager.parties.get(data.getPartyLeader()).getCompleteSize() >= max_sz) {
                        player.sendMessage("§cParty has reached maximum Size.");
                        return true;
                    }
                }

                Player invited = Bukkit.getPlayerExact(args[1].toLowerCase());
                if (invited == null) {
                    for (String message : Hypixelify.getConfigurator().config.getStringList("party.message.player-not-found")) {
                        player.sendMessage(ShopUtil.translateColors(message));
                    }
                    return true;
                }

                if (invited.getUniqueId().equals(player.getUniqueId())) {
                    for (String message : Hypixelify.getConfigurator().config.getStringList("party.message.cannot-invite-yourself")) {
                        player.sendMessage(ShopUtil.translateColors(message));
                    }
                    return true;
                }

                PlayerDatabase data = Database.get(player.getUniqueId());
                if (data == null)
                    Hypixelify.getInstance().playerData.put(player.getUniqueId(), new PlayerDatabase(player));

                PlayerDatabase invitedData = Database.get(invited.getUniqueId());
                if (invitedData == null)
                    Hypixelify.getInstance().playerData.put(invited.getUniqueId(), new PlayerDatabase(invited));

                Party party = Hypixelify.getInstance().partyManager.parties.get(player);
                if (party == null) {
                    party = new Party(player);
                    Hypixelify.getInstance().partyManager.parties.put(player, party);
                    Hypixelify.getInstance().playerData.get(player.getUniqueId()).setIsInParty(true);
                    Hypixelify.getInstance().playerData.get(player.getUniqueId()).setPartyLeader(player);
                }

                if (invitedData.isInParty() || (!party.canAnyoneInvite() && !player.equals(party.getLeader()))) {
                    for (String message : Hypixelify.getConfigurator().config.getStringList("party.message.cannotinvite")) {
                        player.sendMessage(ShopUtil.translateColors(message));
                    }
                    return true;
                }

                if (invitedData.isInvited()) {
                    for (String message : Hypixelify.getConfigurator().config.getStringList("party.message.alreadyInvited")) {
                        player.sendMessage(ShopUtil.translateColors(message));
                    }
                    return true;
                }

                if (party.canAnyoneInvite() || player.equals(party.getLeader())) {
                    Hypixelify.getInstance().partyManager.parties.get(party.getLeader()).addInvitedMember(invited);
                    Database.get(invited.getUniqueId()).setInvited(true);
                    Database.get(invited.getUniqueId()).setInvitedParty(party);

                    for (String message : Hypixelify.getConfigurator().config.getStringList("party.message.invite")) {
                        invited.sendMessage(ShopUtil.translateColors(message).replace("{player}", player.getDisplayName()));
                    }

                    for (String message : Hypixelify.getConfigurator().config.getStringList("party.message.invited")) {
                        player.sendMessage(ShopUtil.translateColors(message).replace("{player}", invited.getDisplayName()));
                    }
                    return true;
                }

            } else {
                for (String str : Hypixelify.getConfigurator().config.getStringList("party.message.invalid-command")) {
                    sender.sendMessage(ShopUtil.translateColors(str));
                }
            }

        } else if (Database.get(player.getUniqueId()) != null && Database.get(player.getUniqueId()).isInParty()
                && Hypixelify.getInstance().partyManager.parties.get(player) != null && Hypixelify.getInstance().partyManager.getParty(player).getPlayers() == null) {
            for (String st : Hypixelify.getConfigurator().config.getStringList("party.message.no-other-commands")) {
                player.sendMessage(ShopUtil.translateColors(st));
            }
            return true;
        } else if (args[0].equalsIgnoreCase("accept") &&
                Database.get(player.getUniqueId()) != null && Database.get(player.getUniqueId()).isInvited()) {
            if (!Database.get(player.getUniqueId()).isInParty()
                    && Hypixelify.getInstance().playerData.get(player.getUniqueId()).getInvitedParty() != null) {

                Party pParty = Database.get(player.getUniqueId()).getInvitedParty();
                Player leader = Database.get(player.getUniqueId()).getInvitedParty().getLeader();
                if (leader != null && pParty != null && pParty.getLeader() != null) {
                    Hypixelify.getInstance().partyManager.parties.get(pParty.getLeader()).addMember(player);
                    Hypixelify.getInstance().partyManager.parties.get(pParty.getLeader()).removeInvitedMember(player);
                }

                Database.get(player.getUniqueId()).setPartyLeader(leader);
                Database.get(player.getUniqueId()).setInvited(false);
                Database.get(player.getUniqueId()).setIsInParty(true);
                Database.get(player.getUniqueId()).setInvitedParty(null);
                Database.get(player.getUniqueId()).setExpiredTimeTimeout(60);
                for (Player p : Hypixelify.getInstance().partyManager.parties.get(leader).getAllPlayers()) {
                    if (p == null) continue;
                    for (String message : Hypixelify.getConfigurator().config.getStringList("party.message.accepted")) {
                        p.sendMessage(ShopUtil.translateColors(message).replace("{player}", player.getDisplayName()));
                    }
                }
                return true;
            }

        } else if (args[0].equalsIgnoreCase("leave")) {
            PlayerDatabase db = Hypixelify.getInstance().playerData.get(player.getUniqueId());
            if (args.length != 1) {
                for (String st : Hypixelify.getConfigurator().config.getStringList("party.message.invalid-command")) {
                    player.sendMessage(ShopUtil.translateColors(st));
                }
                return true;
            }

            if (!db.isInParty()) {
                for (String st : Hypixelify.getConfigurator().config.getStringList("party.message.notinparty")) {
                    player.sendMessage(ShopUtil.translateColors(st));
                }

                return true;
            }
            if (db.isPartyLeader()) {
                player.sendMessage("§cYou have to disband the party first!");
                return true;
            }
            Hypixelify.getInstance().partyManager.parties.get(db.getPartyLeader()).removeMember(player);
            for (Player pl : Hypixelify.getInstance().partyManager.parties.get(db.getPartyLeader()).getAllPlayers()) {
                if (pl != null && pl.isOnline()) {
                    for (String st : Hypixelify.getConfigurator().config.getStringList("party.message.offline-quit")) {
                        pl.sendMessage(ShopUtil.translateColors(st).replace("{player}", player.getDisplayName()));
                    }
                }
            }

            Database.get(player.getUniqueId()).setIsInParty(false);
            Database.get(player.getUniqueId()).setPartyLeader(null);
        }else if (args[0].equalsIgnoreCase("decline") &&
                Database.get(player.getUniqueId()) != null && Database.get(player.getUniqueId()).isInvited()) {
            Party invitedParty = Database.get(player.getUniqueId()).getInvitedParty();
            if(invitedParty == null || invitedParty.getLeader() == null) return true;
            Hypixelify.getInstance().partyManager.parties.get(invitedParty.getLeader()).removeInvitedMember(player);
            for(Player pl : invitedParty.getAllPlayers()) {
                if(pl != null && pl.isOnline()) {
                    for (String st : Hypixelify.getConfigurator().config.getStringList("party.message.declined")) {
                        pl.sendMessage(ShopUtil.translateColors(st));
                    }
                }
            }
            Database.get(player.getUniqueId()).setInvited(false);
            Database.get(player.getUniqueId()).setExpiredTimeTimeout(60);
            Database.get(player.getUniqueId()).setPartyLeader(null);
            Database.get(player.getUniqueId()).setInvitedParty(null);
            return true;
        }


        else if (args[0].equalsIgnoreCase("list")) {
            if (Database.get(player.getUniqueId()) != null && Database.get(player.getUniqueId()).isInParty()) {
                Player leader = Database.get(player.getUniqueId()).getPartyLeader();
                player.sendMessage("Players: ");
                for (Player pl : Hypixelify.getInstance().partyManager.parties.get(leader).getAllPlayers()) {
                    player.sendMessage(pl.getDisplayName());
                }
            } else {
                for (String str : Hypixelify.getConfigurator().config.getStringList("party.message.notinparty")) {
                    player.sendMessage(ShopUtil.translateColors(str));
                }
                return true;
            }
        } else if (args[0].equalsIgnoreCase("disband")) {
            PlayerDatabase data = Database.get(player.getUniqueId());
            if (data == null || Hypixelify.getInstance().partyManager.parties.get(data.getPartyLeader()) == null) {
                for (String str : Hypixelify.getConfigurator().config.getStringList("party.message.notinparty")) {
                    player.sendMessage(ShopUtil.translateColors(str));
                }
                return true;
            } else if (!data.getPartyLeader().equals(player)) {
                for (String str : Hypixelify.getConfigurator().config.getStringList("party.message.access-denied")) {
                    player.sendMessage(ShopUtil.translateColors(str));
                }
                return true;
            } else {
                Party party = Hypixelify.getInstance().partyManager.parties.get(player);
                for (Player pl : party.getAllPlayers()) {
                    if (pl != null) {
                        if (pl.isOnline()) {
                            for (String str : Hypixelify.getConfigurator().config.getStringList("party.message.disband")) {
                                pl.sendMessage(ShopUtil.translateColors(str));
                            }
                        }
                        if (Hypixelify.getInstance().playerData.get(pl.getUniqueId()) != null) {
                            Hypixelify.getInstance().playerData.get(pl.getUniqueId()).setIsInParty(false);
                            Hypixelify.getInstance().playerData.get(pl.getUniqueId()).setPartyLeader(null);
                        }
                    }
                }
                Hypixelify.getInstance().playerData.get(player.getUniqueId()).setIsInParty(false);
                Hypixelify.getInstance().partyManager.parties.get(player).disband();
                Hypixelify.getInstance().partyManager.parties.remove(player);
                return true;
            }
        } else if (args[0].equalsIgnoreCase("kick")) {
            PlayerDatabase data = Database.get(player.getUniqueId());
            if (!data.isInParty() || data.getPartyLeader() == null) {
                for (String str : Hypixelify.getConfigurator().config.getStringList("party.message.notinparty")) {
                    player.sendMessage(ShopUtil.translateColors(str));
                }
                return true;
            } else if (!data.getPartyLeader().equals(player)) {
                for (String str : Hypixelify.getConfigurator().config.getStringList("party.message.access-denied")) {
                    player.sendMessage(ShopUtil.translateColors(str));
                }
                return true;
            } else {
                if (args.length != 2) {
                    for (String str : Hypixelify.getConfigurator().config.getStringList("party.message.invalid-command")) {
                        player.sendMessage(ShopUtil.translateColors(str));
                    }
                    return true;
                }

                Player invited = Bukkit.getPlayerExact(args[1].toLowerCase());
                if (invited == null) {
                    for (String str : Hypixelify.getConfigurator().config.getStringList("party.message.player-not-found")) {
                        player.sendMessage(ShopUtil.translateColors(str));
                    }
                    return true;
                }

                if (invited.equals(player)) {
                    for (String str : Hypixelify.getConfigurator().config.getStringList("party.message.cannot-blank-yourself")) {
                        player.sendMessage(ShopUtil.translateColors(str).replace("{blank}", "kick"));
                    }
                    return true;
                }

                if (!Hypixelify.getInstance().partyManager.parties.get(player).getAllPlayers().contains(invited)) {
                    for (String str : Hypixelify.getConfigurator().config.getStringList("party.message.player-not-found")) {
                        player.sendMessage(ShopUtil.translateColors(str));
                    }
                    return true;
                }
                Hypixelify.getInstance().partyManager.getParty(player).removeMember(invited);
                for (Player pl : Hypixelify.getInstance().partyManager.parties.get(player).getAllPlayers()) {
                    if (pl != null && pl.isOnline()) {
                        for (String st : Hypixelify.getConfigurator().config.getStringList("party.message.kicked")) {
                            pl.sendMessage(ShopUtil.translateColors(st).replace("{player}", invited.getDisplayName()));
                        }
                    }
                }
                Database.get(invited.getUniqueId()).setIsInParty(false);
                Database.get(invited.getUniqueId()).setPartyLeader(null);
                return true;
            }
        } else if (args[0].equalsIgnoreCase("warp")) {

            if (args.length != 1) {
                for (String str : Hypixelify.getConfigurator().config.getStringList("party.message.invalid-command")) {
                    player.sendMessage(ShopUtil.translateColors(str));
                }
                return true;
            }

            if (!player.isOnline()) {
                return true;
            }

            if (!Hypixelify.getInstance().partyManager.isInParty(player)) {
                for (String str : Hypixelify.getConfigurator().config.getStringList("party.message.notinparty")) {
                    player.sendMessage(ShopUtil.translateColors(str));
                }
                return true;
            }

            if (Hypixelify.getInstance().partyManager.getParty(player) != null) {
                PlayerDatabase database = Hypixelify.getInstance().playerData.get(player.getUniqueId());
                if (database != null && database.getPartyLeader() != null) {
                    if (!database.getPartyLeader().equals(player)) {
                        for (String str : Hypixelify.getConfigurator().config.getStringList("party.message.access-denied")) {
                            player.sendMessage(ShopUtil.translateColors(str));
                        }
                        return true;
                    }

                    if (BedwarsAPI.getInstance().isPlayerPlayingAnyGame(player)) {
                        Game game = BedwarsAPI.getInstance().getGameOfPlayer(player);
                        if (!game.getStatus().equals(GameStatus.WAITING)) {
                            player.sendMessage("§cYou cannot do this command while the game is running!");
                            return true;
                        } else {
                            for (String st : Hypixelify.getConfigurator().config.getStringList("party.message.warping")) {
                                player.sendMessage(ShopUtil.translateColors(st));
                            }
                            for (Player pl : Hypixelify.getInstance().partyManager.getParty(player).getPlayers()) {
                                if (pl != null && pl.isOnline()) {
                                    if (game.getConnectedPlayers().size() >= game.getMaxPlayers()) {
                                        pl.sendMessage("§cYou could not be warped to game");
                                        continue;
                                    }
                                    for (String st : Hypixelify.getConfigurator().config.getStringList("party.message.warp")) {
                                        pl.sendMessage(ShopUtil.translateColors(st));
                                    }
                                    if (BedwarsAPI.getInstance().isPlayerPlayingAnyGame(pl)) {
                                        Game g = BedwarsAPI.getInstance().getGameOfPlayer(pl);
                                        g.leaveFromGame(pl);
                                    }

                                    game.joinToGame(pl);
                                }
                            }
                        }
                    } else {
                        for (String st : Hypixelify.getConfigurator().config.getStringList("party.message.warping")) {
                            player.sendMessage(ShopUtil.translateColors(st));
                        }
                        for (Player pl : Hypixelify.getInstance().partyManager.getParty(player).getPlayers()) {
                            if (pl != null && pl.isOnline() && player.isOnline()) {
                                pl.teleport(player.getLocation());
                                for (String st : Hypixelify.getConfigurator().config.getStringList("party.message.warp")) {
                                    pl.sendMessage(ShopUtil.translateColors(st));
                                }
                            }
                        }
                    }

                } else {
                    player.sendMessage("§cSomething went wrong, reload Addon or BedWars to fix this issue");
                    return true;
                }
            }

        } else {
            for (String str : Hypixelify.getConfigurator().config.getStringList("party.message.invalid-command")) {
                player.sendMessage(ShopUtil.translateColors(str));
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!(commandSender instanceof Player))
            return null;
        Player player = (Player) commandSender;

        if (Hypixelify.getInstance().playerData.get(player.getUniqueId()) != null && Hypixelify.getInstance().playerData.get(player.getUniqueId()).isInvited()) {
            return Arrays.asList("accept", "decline");
        }
        if (strings.length == 1) {
            if (Hypixelify.getInstance().playerData.get(player.getUniqueId()) != null && Hypixelify.getInstance().playerData.get(player.getUniqueId()).isInParty()
                    && Hypixelify.getInstance().playerData.get(player.getUniqueId()).getPartyLeader().equals(player))
                return Arrays.asList("invite", "list", "disband", "kick", "warp");


            return Arrays.asList("invite", "list");
        }

        return null;
    }
}
