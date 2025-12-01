package com.yiorno.kabu.listeners;

import com.yiorno.kabu.database.DatabaseManager;
import com.yiorno.kabu.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.time.LocalDate;

/**
 * プレイヤー参加イベントリスナー
 */
public class PlayerJoinListener implements Listener {

    private final Plugin plugin;
    private final DatabaseManager database;
    private final String prefix;

    public PlayerJoinListener(Plugin plugin, DatabaseManager database) {
        this.plugin = plugin;
        this.database = database;
        this.prefix = plugin.getConfig().getString("prefix", "&f[Kabu] &f");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // カブを持っているかチェック
        int kabu = database.getPlayerKabu(player.getUniqueId());
        if (kabu <= 0) {
            return;
        }

        // 3秒後にメッセージを送信
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            int currentDay = LocalDate.now().getDayOfMonth();
            int currentPrice = database.getCurrentPrice();

            if (currentDay == 1 || currentDay == 16) {
                // 1日/16日
                String message = prefix + "おかえりなさい！ 現在のカブ価は" + currentPrice + " MOFUです。";
                MessageUtil.sendMessage(player, message);
            } else {
                // 通常日
                int priceChange = database.getPriceChange();
                String message = prefix + "おかえりなさい！ 現在のカブ価は" + currentPrice + " MOFUです。( 昨日との差: " + priceChange + " )";
                MessageUtil.sendMessage(player, message);
            }
        }, 60L); // 3秒 = 60 ticks
    }
}
