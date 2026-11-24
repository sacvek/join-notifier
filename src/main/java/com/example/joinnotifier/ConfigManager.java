package com.example.joinnotifier;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ConfigManager {
    public enum NotifyMode {
        CHAT, TOAST, BOTH
    }

    public enum UiStyle {
        CLASSIC, CLASSIC_DARK
    }

    private static class ConfigData {
        boolean globalMute = false;
        NotifyMode notifyMode = NotifyMode.BOTH;
        UiStyle uiStyle = UiStyle.CLASSIC;
        List<String> collapsedGroups = new ArrayList<>();
        List<PlayerEntry> players = new ArrayList<>();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("joinnotifier.json");

    private static final List<PlayerEntry> players = new ArrayList<>();
    private static final Set<String> collapsedGroups = new HashSet<>();
    private static boolean globalMute = false;
    private static NotifyMode notifyMode = NotifyMode.BOTH;
    private static UiStyle uiStyle = UiStyle.CLASSIC;

    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                JsonElement root = JsonParser.parseString(json);

                players.clear();
                collapsedGroups.clear();
                if (root.isJsonArray()) {
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
                        uiStyle = data.uiStyle != null ? data.uiStyle : UiStyle.CLASSIC;
                        if (data.collapsedGroups != null) {
                            collapsedGroups.addAll(data.collapsedGroups);
                        }
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
            data.uiStyle = uiStyle;
            data.collapsedGroups = new ArrayList<>(collapsedGroups);
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

    public static UiStyle getUiStyle() {
        return uiStyle;
    }

    public static void cycleUiStyle() {
        uiStyle = switch (uiStyle) {
            case CLASSIC -> UiStyle.CLASSIC_DARK;
            case CLASSIC_DARK -> UiStyle.CLASSIC;
        };
        save();
    }

    public static boolean isGroupCollapsed(String groupName) {
        return groupName != null && collapsedGroups.contains(groupName);
    }

    public static void toggleGroupCollapsed(String groupName) {
        if (groupName == null || groupName.isEmpty()) return;
        if (!collapsedGroups.remove(groupName)) {
            collapsedGroups.add(groupName);
        }
        save();
    }

    public static void toggleGroupEnabled(String groupName) {
        if (groupName == null || groupName.isEmpty()) return;
        boolean anyActive = false;
        for (PlayerEntry p : players) {
            if (groupName.equalsIgnoreCase(p.group) && p.isEffectivelyEnabled()) {
                anyActive = true;
                break;
            }
        }
        boolean targetState = !anyActive;
        for (PlayerEntry p : players) {
            if (groupName.equalsIgnoreCase(p.group)) {
                if (targetState) p.clearMute();
                else p.enabled = false;
            }
        }
        save();
    }

    public static void onDisconnectAll() {
        boolean changed = false;
        for (PlayerEntry p : players) {
            if (p.muteUntilDisconnect) {
                p.onDisconnect();
                changed = true;
            }
        }
        if (changed) save();
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
