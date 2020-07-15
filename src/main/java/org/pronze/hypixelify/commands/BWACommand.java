package org.pronze.hypixelify.commands;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.pronze.hypixelify.Hypixelify;
import org.screamingsandals.bedwars.Main;
import org.screamingsandals.bedwars.api.game.Game;
import org.screamingsandals.bedwars.game.GameCreator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


public class BWACommand implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {

        if (args.length >= 1) {
            if(sender instanceof  Player) {
                if (!sender.isOp()) {
                    sender.sendMessage(ChatColor.RED + "You Don't have permissions to do this command");
                    return true;
                }
            }
            if (args[0].equalsIgnoreCase("reload")) {
                Bukkit.getServer().getPluginManager().disablePlugin(Hypixelify.getInstance());
                Bukkit.getServer().getPluginManager().enablePlugin(Hypixelify.getInstance());
                sender.sendMessage("Plugin reloaded!");
            }
            else if (args[0].equalsIgnoreCase("help"))
            {
                sender.sendMessage(ChatColor.RED + "SBAHypixelify");
                sender.sendMessage("Available commands:");
                sender.sendMessage("/bwaddon reload - Reload the addon");
                sender.sendMessage("/bwaddon help - Show available list of commands");
                sender.sendMessage("/bwaddon reset - resets all configs related to addon");
            }
            else if(args[0].equalsIgnoreCase("reset")){
                sender.sendMessage("Resetting...");
                try{
                    Hypixelify.getConfigurator().upgradeCustomFiles();
                    ((Player)sender).performCommand("bwaddon clearnpc");
                    sender.sendMessage("Sucessfully resetted");
                } catch(Exception e){
                    e.printStackTrace();
                }
            } else if(args[0].equalsIgnoreCase("clearnpc")){
                for(Game game: Main.getInstance().getGames()){
                    World world = game.getGameWorld();
                    List<NPC> npcs = new ArrayList<>();
                    CitizensAPI.getNPCRegistry().forEach(npc -> {
                        if(npc.getStoredLocation().getWorld().equals(world) &&
                                GameCreator.isInArea(npc.getStoredLocation(), game.getPos1(), game.getPos2())){
                            npcs.add(npc);
                        }
                    });

                    if(!npcs.isEmpty() && npcs != null){
                        for(NPC npc : npcs){
                            npc.destroy();
                        }
                    }
                }
                sender.sendMessage("Cleared all npcs from bedwars worlds");
            }
            else if(!Objects.requireNonNull(Hypixelify.getConfigurator().config.getString("version")).contains(Hypixelify.getVersion())) {
                if (args[0].equalsIgnoreCase("upgrade")) {
                    try {
                        Hypixelify.getConfigurator().upgradeCustomFiles();
                        ((Player)sender).performCommand("bwaddon clearnpc");
                        sender.sendMessage("[SBAHypixelify]: " + ChatColor.GOLD + "Sucessfully upgraded files!");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (args[0].equalsIgnoreCase("cancel")) {
                    Hypixelify.getConfigurator().config.set("version", Hypixelify.getVersion());
                    sender.sendMessage("[SBAHypixelify]: Cancelled shop and upgradeShop changes");
                }
            }
            else {
                sender.sendMessage("[SBAHypixelify]" + ChatColor.RED + "Unknown command, do /bwaddon help for more.");
            }
        }
        else {
            sender.sendMessage("[SBAHypixelify]" + ChatColor.RED + "Unknown command, do /bwaddon help for more.");
        }

        return true;
    }


    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if (strings.length == 1) {
            if(!Objects.requireNonNull(Hypixelify.getConfigurator().config.getString("version")).contains(Hypixelify.getVersion())){
                return Arrays.asList("cancel","upgrade");
            }
            return Arrays.asList("reload", "help", "reset", "clearnpc");
        }

        return null;
    }
}

