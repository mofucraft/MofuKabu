package com.yiorno.kabu.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * MySQLデータベース管理クラス
 * HikariCPを使用した効率的な接続プール管理
 */
public class DatabaseManager {

    private final Plugin plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * データベース接続を初期化
     */
    public void initialize() {
        HikariConfig config = new HikariConfig();

        String host = plugin.getConfig().getString("mysql.host", "localhost");
        int port = plugin.getConfig().getInt("mysql.port", 3306);
        String database = plugin.getConfig().getString("mysql.database", "minecraft");
        String username = plugin.getConfig().getString("mysql.username", "root");
        String password = plugin.getConfig().getString("mysql.password", "password");

        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&serverTimezone=UTC");
        config.setUsername(username);
        config.setPassword(password);

        config.setMaximumPoolSize(plugin.getConfig().getInt("mysql.pool.maximum-pool-size", 10));
        config.setMinimumIdle(plugin.getConfig().getInt("mysql.pool.minimum-idle", 2));
        config.setConnectionTimeout(plugin.getConfig().getLong("mysql.pool.connection-timeout", 30000));

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        try {
            dataSource = new HikariDataSource(config);
            plugin.getLogger().info("データベース接続に成功しました");
            createTables();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "データベース接続に失敗しました", e);
        }
    }

    /**
     * テーブルを作成
     */
    private void createTables() {
        String createPlayersTable = "CREATE TABLE IF NOT EXISTS kabu_players (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "kabu_amount INT NOT NULL DEFAULT 0" +
                ")";

        String createPricesTable = "CREATE TABLE IF NOT EXISTS kabu_prices (" +
                "id INT PRIMARY KEY DEFAULT 1," +
                "current_price INT NOT NULL DEFAULT 100," +
                "price_change INT NOT NULL DEFAULT 0," +
                "last_update_day INT NOT NULL DEFAULT 1" +
                ")";

        try (Connection conn = getConnection();
                PreparedStatement stmt1 = conn.prepareStatement(createPlayersTable);
                PreparedStatement stmt2 = conn.prepareStatement(createPricesTable)) {

            stmt1.executeUpdate();
            stmt2.executeUpdate();

            // 初期価格データを挿入（存在しない場合のみ）
            String insertInitialPrice = "INSERT INTO kabu_prices (id, current_price, price_change, last_update_day) " +
                    "VALUES (1, 100, 0, 1) " +
                    "ON DUPLICATE KEY UPDATE id=id";
            try (PreparedStatement stmt3 = conn.prepareStatement(insertInitialPrice)) {
                stmt3.executeUpdate();
            }

            plugin.getLogger().info("データベーステーブルを作成しました");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "テーブル作成に失敗しました", e);
        }
    }

    /**
     * データベース接続を取得
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("データソースが初期化されていません");
        }
        return dataSource.getConnection();
    }

    /**
     * プレイヤーのカブ保有数を取得
     */
    public int getPlayerKabu(UUID uuid) {
        String query = "SELECT kabu_amount FROM kabu_players WHERE uuid = ?";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("kabu_amount");
            }
            return 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "カブ保有数の取得に失敗しました: " + uuid, e);
            return 0;
        }
    }

    /**
     * プレイヤーのカブ保有数を設定
     */
    public void setPlayerKabu(UUID uuid, int amount) {
        String query = "INSERT INTO kabu_players (uuid, kabu_amount) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE kabu_amount = ?";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, uuid.toString());
            stmt.setInt(2, amount);
            stmt.setInt(3, amount);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "カブ保有数の設定に失敗しました: " + uuid, e);
        }
    }

    /**
     * プレイヤーのカブ保有数を増減
     */
    public void addPlayerKabu(UUID uuid, int amount) {
        int current = getPlayerKabu(uuid);
        setPlayerKabu(uuid, current + amount);
    }

    /**
     * 全プレイヤーのカブをクリア
     */
    public void clearAllKabu() {
        String query = "DELETE FROM kabu_players";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.executeUpdate();
            plugin.getLogger().info("全プレイヤーのカブをクリアしました");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "カブクリアに失敗しました", e);
        }
    }

    /**
     * カブ保有数ランキングを取得
     */
    public Map<UUID, Integer> getTopPlayers(int limit) {
        String query = "SELECT uuid, kabu_amount FROM kabu_players ORDER BY kabu_amount DESC LIMIT ?";
        Map<UUID, Integer> topPlayers = new HashMap<>();

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                int amount = rs.getInt("kabu_amount");
                topPlayers.put(uuid, amount);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "ランキング取得に失敗しました", e);
        }

        return topPlayers;
    }

    /**
     * 現在のカブ価格を取得
     */
    public int getCurrentPrice() {
        String query = "SELECT current_price FROM kabu_prices WHERE id = 1";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("current_price");
            }
            return 100; // デフォルト値
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "価格取得に失敗しました", e);
            return 100;
        }
    }

    /**
     * 価格変動を取得
     */
    public int getPriceChange() {
        String query = "SELECT price_change FROM kabu_prices WHERE id = 1";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("price_change");
            }
            return 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "価格変動取得に失敗しました", e);
            return 0;
        }
    }

    /**
     * 最終更新日を取得
     */
    public int getLastUpdateDay() {
        String query = "SELECT last_update_day FROM kabu_prices WHERE id = 1";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("last_update_day");
            }
            return 1;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "最終更新日取得に失敗しました", e);
            return 1;
        }
    }

    /**
     * 価格データを更新
     */
    public void updatePrice(int currentPrice, int priceChange, int lastUpdateDay) {
        String query = "UPDATE kabu_prices SET current_price = ?, price_change = ?, last_update_day = ? WHERE id = 1";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, currentPrice);
            stmt.setInt(2, priceChange);
            stmt.setInt(3, lastUpdateDay);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "価格更新に失敗しました", e);
        }
    }

    /**
     * データベース接続を閉じる
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("データベース接続を閉じました");
        }
    }

    /**
     * 非同期でプレイヤーのカブを設定
     */
    public CompletableFuture<Void> setPlayerKabuAsync(UUID uuid, int amount) {
        return CompletableFuture.runAsync(() -> setPlayerKabu(uuid, amount));
    }

    /**
     * 非同期でプレイヤーのカブを取得
     */
    public CompletableFuture<Integer> getPlayerKabuAsync(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> getPlayerKabu(uuid));
    }
}
