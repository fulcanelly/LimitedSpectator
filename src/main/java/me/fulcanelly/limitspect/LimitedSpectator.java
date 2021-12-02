package me.fulcanelly.limitspect;

import me.fulcanelly.tgbridge.Bridge;
import me.fulcanelly.tgbridge.tools.twofactor.register.SignupLoginReception;

import java.util.WeakHashMap;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class LimitedSpectator extends JavaPlugin {
    SignupLoginReception reception;

    public boolean onCommand(CommandSender sender, Command cmd, String cname, String[] args) {
        if (sender instanceof org.bukkit.command.ConsoleCommandSender) {
            sender.sendMessage("Only players can use this command");
            return true;
        } 
        if (this.reception.getTgByUser(sender.getName()).isEmpty()) {
            sender.sendMessage("Only logged-in with telegram players can do it");
            return true;
        } 
        Player player = (Player)sender;
        if (player.getGameMode().equals(GameMode.SPECTATOR)) {
            onSpecToSurv(player);
            player.setGameMode(GameMode.SURVIVAL);
        } else {
            onSurvToSpec(player);
            player.setGameMode(GameMode.SPECTATOR);
        } 
        return true;
    }

    WeakHashMap<Player, Location> changePosByPlayer = new WeakHashMap<>();
   
    void onSurvToSpec(Player player) {
        changePosByPlayer.put(player, player.getLocation());
    }   

    void onSpecToSurv(Player player) {
        player.teleport(changePosByPlayer.get(player));
        changePosByPlayer.remove(player);
    }


    public void onEnable() {
        Bridge plugin = (Bridge)getServer().getPluginManager()
        .getPlugin("tg-bridge");
        this.reception = (SignupLoginReception)plugin.getInjector()
        .getInstance(SignupLoginReception.class);
        getCommand("spectator").setExecutor((CommandExecutor)this);
    }
}
