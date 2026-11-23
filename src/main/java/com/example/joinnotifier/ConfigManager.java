package com.example.joinnotifier;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {
    public enum NotifyMode {
        CHAT, TOAST, BOTH
    }

    private static class ConfigData {
        boolean globalMute = false;
        NotifyMode notifyMode = NotifyMode.BOTH;
        List<PlayerEntry> players = new ArrayList<>();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("joinnotifier.json");

    private static final List<PlayerEntry> players = new ArrayList<>();
    private static boolean globalMute = false;
    private static NotifyMode notifyMode = NotifyMode.BOTH;

    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                JsonElement root = JsonParser.parseString(json);

                players.clear();
                if (root.isJsonArray()) {
                    // Совместимость со старым форматом списка
                    Type listType = new TypeToken<List<PlayerEntry>>() {}.getType();
                    List<PlayerEntry> loaded = GSON.fromJson(root, listType);
                    if (loaded != null) {
                        for (PlayerEntry e : loaded) {
                            e.normalize();
                            players.add(e);
                        }
                    }
                    save();
                } else if (root.isJsonObject()) {
                    ConfigData data = GSON.fromJson(root, ConfigData.class);
                    if (data != null) {
                        globalMute = data.globalMute;
                        notifyMode = data.notifyMode != null ? data.notifyMode : NotifyMode.BOTH;
                        if (data.players != null) {
                            for (PlayerEntry e : data.players) {
                                e.normalize();
                                players.add(e);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            ConfigData data = new ConfigData();
            data.globalMute = globalMute;
            data.notifyMode = notifyMode;
            data.players = players;
            Files.writeString(CONFIG_PATH, GSON.toJson(data));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<PlayerEntry> getPlayers() {
        return players;
    }

    public static boolean isGlobalMute() {
        return globalMute;
    }

    public static void toggleGlobalMute() {
        globalMute = !globalMute;
        save();
    }

    public static NotifyMode getNotifyMode() {
        return notifyMode;
    }

    public static void cycleNotifyMode() {
        notifyMode = switch (notifyMode) {
            case CHAT -> NotifyMode.TOAST;
            case TOAST -> NotifyMode.BOTH;
            case BOTH -> NotifyMode.CHAT;
        };
        save();
    }

    public static boolean addPlayer(String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.isEmpty()) return false;

        for (PlayerEntry entry : players) {
            if (entry.name.equalsIgnoreCase(name)) {
                return false;
            }
        }
        players.add(new PlayerEntry(name, true));
        save();
        return true;
    }

    public static boolean removePlayer(String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.isEmpty()) return false;

        boolean removed = players.removeIf(entry -> entry.name.equalsIgnoreCase(name));
        if (removed) {
            save();
        }
        return removed;
    }

    public static void togglePlayer(PlayerEntry entry) {
        entry.enabled = !entry.enabled;
        save();
    }

    public static PlayerEntry getEntry(String playerName) {
        if (playerName == null) return null;
        for (PlayerEntry entry : players) {
            if (entry.name.equalsIgnoreCase(playerName)) {
                return entry;
            }
        }
        return null;
    }
}