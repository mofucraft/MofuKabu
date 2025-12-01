package com.yiorno.kabu.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

/**
 * メッセージユーティリティクラス
 * HEXカラーコードやMiniMessage形式のサポート
 */
public class MessageUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();

    /**
     * カラーコードを変換（&コード + HEXサポート）
     * 例: "&f[<##00ff7f>Kabu&cβ&f] &f"
     */
    public static String colorize(String message) {
        if (message == null)
            return "";

        // <##RRGGBB>形式をMiniMessage形式に変換
        message = message.replaceAll("<##([0-9a-fA-F]{6})>", "<#$1>");

        // MiniMessageで処理
        Component component = MINI_MESSAGE.deserialize(message);

        // レガシー形式にシリアライズ（&コードのサポート）
        return LEGACY_SERIALIZER.serialize(component);
    }

    /**
     * プレフィックス付きメッセージを送信
     */
    public static void sendMessage(CommandSender sender, String prefix, String message) {
        String fullMessage = prefix + message;
        sender.sendMessage(colorize(fullMessage));
    }

    /**
     * メッセージをそのまま送信
     */
    public static void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(colorize(message));
    }
}
