package me.fulcanelly.limitspect;

import me.fulcanelly.tgbridge.Bridge;
import me.fulcanelly.tgbridge.tools.twofactor.register.SignupLoginReception;
import org.bukkit.GameMode;
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
            sender.sendMessage("Only logined with telegram players can do it");
            return true;
        } 
        Player player = (Player)sender;
        if (player.getGameMode().equals(GameMode.SPECTATOR)) {
            player.setGameMode(GameMode.SURVIVAL);
        } else {
            player.setGameMode(GameMode.SPECTATOR);
        } 
        return true;
    }

    public void onEnable() {
        Bridge plugin = (Bridge)getServer().getPluginManager()
        .getPlugin("tg-bridge");
        this.reception = (SignupLoginReception)plugin.getInjector()
        .getInstance(SignupLoginReception.class);
        getCommand("spectator").setExecutor((CommandExecutor)this);
    }
}