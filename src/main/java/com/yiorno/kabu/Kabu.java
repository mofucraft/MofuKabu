package com.yiorno.kabu;

import com.yiorno.kabu.commands.AdminCommand;
import com.yiorno.kabu.commands.PlayerCommand;
import com.yiorno.kabu.database.DatabaseManager;
import com.yiorno.kabu.listeners.PlayerJoinListener;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Kabuプラグインのメインクラス
 */
public final class Kabu extends JavaPlugin {

    private DatabaseManager databaseManager;
    private Economy economy;
    private RenewPrice renewPrice;
    private String prefix;

    @Override
    public void onEnable() {
        // 設定ファイルを保存
        saveDefaultConfig();
        prefix = getConfig().getString("prefix", "&f[Kabu] &f");

        getLogger().info("カブプラグインを起動しています...");

        // Vaultの初期化
        if (!setupEconomy()) {
            getLogger().severe("Vaultが見つかりません！プラグインを無効化します。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // データベースの初期化
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();

        // 価格更新システムの初期化
        renewPrice = new RenewPrice(this, databaseManager);
        renewPrice.startScheduler();

        // コマンドの登録
        PlayerCommand playerCommand = new PlayerCommand(this, databaseManager, economy);
        AdminCommand adminCommand = new AdminCommand(this, databaseManager, renewPrice);

        getCommand("kabu").setExecutor(playerCommand);
        getCommand("editkabu").setExecutor(adminCommand);
        getCommand("reset-kabu").setExecutor(adminCommand);

        // イベントリスナーの登録
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, databaseManager), this);

        getLogger().info("カブプラグインが起動しました！");
    }

    @Override
    public void onDisable() {
        // データベース接続を閉じる
        if (databaseManager != null) {
            databaseManager.close();
        }

        getLogger().info("カブプラグインが停止しました");
    }

    /**
     * Vaultのセットアップ
     */
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public Economy getEconomy() {
        return economy;
    }

    public String getPrefix() {
        return prefix;
    }
}
