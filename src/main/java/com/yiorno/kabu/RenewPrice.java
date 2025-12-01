package com.yiorno.kabu;

import com.yiorno.kabu.database.DatabaseManager;
import com.yiorno.kabu.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.time.LocalDate;
import java.util.Random;

/**
 * カブ価格の更新ロジック
 * Skriptのロジックを完全移植
 */
public class RenewPrice {

    private final Plugin plugin;
    private final DatabaseManager database;
    private final Random random;
    private final String prefix;

    public RenewPrice(Plugin plugin, DatabaseManager database) {
        this.plugin = plugin;
        this.database = database;
        this.random = new Random();
        this.prefix = plugin.getConfig().getString("prefix", "&f[Kabu] &f");
    }

    /**
     * 日付をチェックして価格を更新
     */
    public void checkAndUpdatePrice() {
        int currentDay = LocalDate.now().getDayOfMonth();
        int lastUpdateDay = database.getLastUpdateDay();

        // 既に今日更新済みなら何もしない
        if (lastUpdateDay == currentDay) {
            return;
        }

        if (currentDay == 1 || currentDay == 16) {
            // 1日または16日: カブをリセットして新価格を設定
            resetKabuOnNewPeriod(currentDay);
        } else {
            // 通常日: 価格を変動させる
            updateDailyPrice(currentDay);
        }
    }

    /**
     * 1日/16日のリセット処理
     */
    private void resetKabuOnNewPeriod(int currentDay) {
        // 全プレイヤーのカブをクリア
        database.clearAllKabu();

        // 新しい価格を計算
        int kabuPrice1 = random.nextInt(10) + 1; // 1-10
        int kabuPrice2;

        if (kabuPrice1 == 1 || kabuPrice1 == 2) {
            if (kabuPrice1 == 1) {
                kabuPrice2 = random.nextInt(21) + 80; // 80-100
            } else {
                kabuPrice2 = random.nextInt(41) + 140; // 140-180
            }
        } else {
            kabuPrice2 = random.nextInt(41) + 100; // 100-140
        }

        int kabuPrice3 = random.nextInt(9) + 1; // 1-9
        int newPrice = kabuPrice2 - kabuPrice3;

        // データベースを更新
        database.updatePrice(newPrice, 0, currentDay);

        // ブロードキャスト
        String message = prefix + "新カブ価 " + newPrice + " MOFU";
        Bukkit.getServer().broadcast(net.kyori.adventure.text.Component.text(MessageUtil.colorize(message)));

        plugin.getLogger().info("カブ価をリセットしました: " + newPrice + " MOFU");
    }

    /**
     * 通常日の価格変動
     */
    private void updateDailyPrice(int currentDay) {
        int currentPrice = database.getCurrentPrice();

        // 変動率を決定
        int nextKabuChance = random.nextInt(51); // 0-50
        int nextKabuPrice;

        if (nextKabuChance <= 20) {
            nextKabuPrice = random.nextInt(11); // 0-10
        } else if (nextKabuChance <= 45) {
            nextKabuPrice = random.nextInt(16) + 10; // 10-25
        } else {
            nextKabuPrice = random.nextInt(16) + 25; // 25-40
        }

        // プラスかマイナスか
        int plusOrMinus = random.nextInt(2); // 0 or 1
        if (plusOrMinus == 0) {
            nextKabuPrice = -nextKabuPrice;
        }

        // 価格変動を計算
        int priceChange = (int) ((double) nextKabuPrice * currentPrice / 100.0);
        int newPrice = currentPrice + priceChange;

        // 価格が0以下の場合は334に設定
        if (newPrice <= 0) {
            newPrice = 334;
            priceChange = 334;
        }

        // データベースを更新
        database.updatePrice(newPrice, priceChange, currentDay);

        // ブロードキャスト
        String message = prefix + "新カブ価 " + newPrice + " MOFU ( 増減:" + priceChange + " )";
        Bukkit.getServer().broadcast(net.kyori.adventure.text.Component.text(MessageUtil.colorize(message)));

        plugin.getLogger().info("カブ価を更新しました: " + newPrice + " MOFU (増減: " + priceChange + ")");
    }

    /**
     * スケジューラーを開始
     */
    public void startScheduler() {
        // 起動時に1回チェック（10秒後）
        Bukkit.getScheduler().runTaskLater(plugin, this::checkAndUpdatePrice, 200L);

        // 定期的にチェック
        long interval = plugin.getConfig().getLong("date-check-interval", 30) * 60 * 20; // 分をTickに変換
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkAndUpdatePrice, 400L, interval);
    }
}
