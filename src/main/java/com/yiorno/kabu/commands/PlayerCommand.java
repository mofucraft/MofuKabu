package com.yiorno.kabu.commands;

import com.yiorno.kabu.database.DatabaseManager;
import com.yiorno.kabu.util.MessageUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * プレイヤーコマンドハンドラー
 */
public class PlayerCommand implements CommandExecutor {

    private final Plugin plugin;
    private final DatabaseManager database;
    private final Economy economy;
    private final String prefix;

    public PlayerCommand(Plugin plugin, DatabaseManager database, Economy economy) {
        this.plugin = plugin;
        this.database = database;
        this.economy = economy;
        this.prefix = plugin.getConfig().getString("prefix", "&f[Kabu] &f");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("このコマンドはプレイヤーのみ実行できます");
            return true;
        }

        Player player = (Player) sender;

        // 引数がない場合はヘルプを表示
        if (args.length == 0) {
            showHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "price":
                showPrice(player);
                break;
            case "own":
                showOwn(player);
                break;
            case "buy":
                if (args.length < 2) {
                    MessageUtil.sendMessage(player, prefix, "使い方: /kabu buy <数量>");
                    return true;
                }
                try {
                    int amount = Integer.parseInt(args[1]);
                    buyKabu(player, amount);
                } catch (NumberFormatException e) {
                    MessageUtil.sendMessage(player, prefix, "数値を入力してください");
                }
                break;
            case "sell":
                if (args.length < 2) {
                    MessageUtil.sendMessage(player, prefix, "使い方: /kabu sell <数量>");
                    return true;
                }
                try {
                    int amount = Integer.parseInt(args[1]);
                    sellKabu(player, amount);
                } catch (NumberFormatException e) {
                    MessageUtil.sendMessage(player, prefix, "数値を入力してください");
                }
                break;
            case "check":
                if (args.length >= 2) {
                    checkPlayer(player, args[1]);
                } else {
                    checkDaysRemaining(player);
                }
                break;
            case "top":
                showTop(player);
                break;
            case "set":
                if (!player.hasPermission("mofucraft.staff")) {
                    MessageUtil.sendMessage(player, prefix, "ああん？");
                    return true;
                }
                if (args.length < 2) {
                    MessageUtil.sendMessage(player, prefix, "おおん？");
                    return true;
                }
                try {
                    int price = Integer.parseInt(args[1]);
                    setPrice(player, price);
                } catch (NumberFormatException e) {
                    MessageUtil.sendMessage(player, prefix, "数値を入力してください");
                }
                break;
            case "set-zougen":
                if (!player.hasPermission("mofucraft.staff")) {
                    MessageUtil.sendMessage(player, prefix, "ああん？");
                    return true;
                }
                if (args.length < 2) {
                    MessageUtil.sendMessage(player, prefix, "おおん？");
                    return true;
                }
                try {
                    int change = Integer.parseInt(args[1]);
                    setPriceChange(player, change);
                } catch (NumberFormatException e) {
                    MessageUtil.sendMessage(player, prefix, "数値を入力してください");
                }
                break;
            case "reload":
                if (!player.hasPermission("mofucraft.staff")) {
                    MessageUtil.sendMessage(player, prefix, "ああん？");
                    return true;
                }
                reload(player);
                break;
            default:
                showHelp(player);
                break;
        }

        return true;
    }

    private void showHelp(Player player) {
        MessageUtil.sendMessage(player, "------- " + prefix + "-------");
        MessageUtil.sendMessage(player, "/kabu price : &a本日のカブ価");
        MessageUtil.sendMessage(player, "/kabu own : &a所持しているカブの確認");
        MessageUtil.sendMessage(player, "/kabu buy 数字 : &aカブを購入");
        MessageUtil.sendMessage(player, "/kabu sell 数字 : &aカブを売却");
        MessageUtil.sendMessage(player, "/kabu check : &a残り時間を確認");
        MessageUtil.sendMessage(player, "もっと詳しく : mofucraft.net/kabu");
    }

    private void showPrice(Player player) {
        int price = database.getCurrentPrice();
        MessageUtil.sendMessage(player, prefix, "本日のカブ価は" + price + " MOFUです");
    }

    private void showOwn(Player player) {
        int kabu = database.getPlayerKabu(player.getUniqueId());
        MessageUtil.sendMessage(player, prefix, "現在" + kabu + "カブ持っています！");
    }

    private void buyKabu(Player player, int amount) {
        // 100単位チェック
        if (amount % 100 != 0) {
            MessageUtil.sendMessage(player, prefix, "100カブ単位で購入できます");
            return;
        }

        if (amount <= 0) {
            MessageUtil.sendMessage(player, prefix, "その数値は使えません！");
            return;
        }

        int price = database.getCurrentPrice();
        double totalCost = (double) amount * price;

        // 残高チェック
        if (!economy.has(player, totalCost)) {
            MessageUtil.sendMessage(player, prefix, "お金が足りません！");
            return;
        }

        // カブを追加
        database.addPlayerKabu(player.getUniqueId(), amount);
        economy.withdrawPlayer(player, totalCost);

        MessageUtil.sendMessage(player, prefix, amount + "カブ購入しました！");
        MessageUtil.sendMessage(player, prefix, "合計" + (int) totalCost + " MOFU使いました");
    }

    private void sellKabu(Player player, int amount) {
        // 100単位チェック
        if (amount % 100 != 0) {
            MessageUtil.sendMessage(player, prefix, "100カブ単位で売却できます");
            return;
        }

        if (amount <= 0) {
            MessageUtil.sendMessage(player, prefix, "その数値は使えません！");
            return;
        }

        int currentKabu = database.getPlayerKabu(player.getUniqueId());
        if (currentKabu < amount) {
            MessageUtil.sendMessage(player, prefix, "カブが足りません！");
            return;
        }

        int price = database.getCurrentPrice();
        double totalIncome = (double) amount * price;

        // カブを減らす
        database.addPlayerKabu(player.getUniqueId(), -amount);
        economy.depositPlayer(player, totalIncome);

        MessageUtil.sendMessage(player, prefix, amount + "カブ売却しました");
        MessageUtil.sendMessage(player, prefix, "合計" + (int) totalIncome + " MOFU入手しました！");
    }

    private void checkDaysRemaining(Player player) {
        int currentDay = LocalDate.now().getDayOfMonth();
        int daysInMonth = LocalDate.now().lengthOfMonth();
        int daysRemaining;

        if (currentDay <= 16) {
            daysRemaining = 16 - currentDay;
        } else {
            daysRemaining = daysInMonth - currentDay + 1;
        }

        MessageUtil.sendMessage(player, prefix, "あと" + daysRemaining + "日でカブがダメになります！");
    }

    private void checkPlayer(Player player, String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        int kabu = database.getPlayerKabu(target.getUniqueId());
        MessageUtil.sendMessage(player, prefix, targetName + "は" + kabu + "カブ持っています");
    }

    private void showTop(Player player) {
        Map<UUID, Integer> topPlayers = database.getTopPlayers(5);

        if (topPlayers.isEmpty()) {
            MessageUtil.sendMessage(player, prefix, "まだランキングデータがありません");
            return;
        }

        int rank = 1;
        for (Map.Entry<UUID, Integer> entry : topPlayers.entrySet()) {
            OfflinePlayer p = Bukkit.getOfflinePlayer(entry.getKey());
            MessageUtil.sendMessage(player, prefix, rank + "位 " + p.getName() + " : " + entry.getValue() + "カブ");
            rank++;
        }
    }

    private void setPrice(Player player, int price) {
        int currentDay = database.getLastUpdateDay();
        database.updatePrice(price, 0, currentDay);
        MessageUtil.sendMessage(player, prefix, "カブ価を" + price + "に設定しました");
    }

    private void setPriceChange(Player player, int change) {
        int currentPrice = database.getCurrentPrice();
        int currentDay = database.getLastUpdateDay();
        database.updatePrice(currentPrice, change, currentDay);
        MessageUtil.sendMessage(player, prefix, "増減値を" + change + "に設定しました");
    }

    private void reload(Player player) {
        plugin.reloadConfig();
        MessageUtil.sendMessage(player, prefix, "設定ファイルをリロードしました");
    }
}
