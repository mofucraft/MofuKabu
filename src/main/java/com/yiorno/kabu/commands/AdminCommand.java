package com.yiorno.kabu.commands;

import com.yiorno.kabu.RenewPrice;
import com.yiorno.kabu.database.DatabaseManager;
import com.yiorno.kabu.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * 管理コマンドハンドラー
 */
public class AdminCommand implements CommandExecutor {

    private final Plugin plugin;
    private final DatabaseManager database;
    private final RenewPrice renewPrice;
    private final String prefix;

    public AdminCommand(Plugin plugin, DatabaseManager database, RenewPrice renewPrice) {
        this.plugin = plugin;
        this.database = database;
        this.renewPrice = renewPrice;
        this.prefix = plugin.getConfig().getString("prefix", "&f[Kabu] &f");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("mofucraft.staff")) {
            MessageUtil.sendMessage(sender, prefix, "ああん？");
            return true;
        }

        // /editkabu コマンド
        if (command.getName().equalsIgnoreCase("editkabu")) {
            if (args.length < 3) {
                MessageUtil.sendMessage(sender, "使い方: /editkabu <add|remove> <player> <amount>");
                return true;
            }

            String action = args[0].toLowerCase();
            String playerName = args[1];
            int amount;

            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                MessageUtil.sendMessage(sender, prefix, "数値を入力してください");
                return true;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);

            if (action.equals("add")) {
                database.addPlayerKabu(target.getUniqueId(), amount);
                MessageUtil.sendMessage(sender, "done");
            } else if (action.equals("remove")) {
                database.addPlayerKabu(target.getUniqueId(), -amount);
                MessageUtil.sendMessage(sender, "done");
            } else {
                MessageUtil.sendMessage(sender, "使い方: /editkabu <add|remove> <player> <amount>");
            }

            return true;
        }

        // /reset-kabu コマンド
        if (command.getName().equalsIgnoreCase("reset-kabu")) {
            renewPrice.checkAndUpdatePrice();
            MessageUtil.sendMessage(sender, prefix, "カブ価をリセットしました");
            return true;
        }

        return false;
    }
}
