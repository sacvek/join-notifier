package com.example.joinnotifier;

import net.minecraft.ChatFormatting;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.Locale;

public class PlayerEntry {
    public String name;
    public boolean enabled = true;
    public boolean notifyLeave = false;
    public String tag = "";
    public String color = "GREEN";
    public String serverIp = "";

    // Новые поля v1.3.0
    public String group = "";              // Название группы/категории (пусто = без группы)
    public String sound = "NONE";          // Звук уведомления: NONE, BELL, XP, LEVEL, CAT, ANVIL, NOTE
    public long muteUntil = 0L;            // Timestamp (ms), до которого игрок на паузе (0 = не задано)
    public boolean muteUntilDisconnect = false; // Пауза до выхода с текущего сервера

    public PlayerEntry() {
    }

    public PlayerEntry(String name, boolean enabled) {
        this.name = name;
        this.enabled = enabled;
        normalize();
    }

    public void normalize() {
        if (this.name == null) this.name = "";
        if (this.tag == null) this.tag = "";
        if (this.color == null || this.color.isEmpty()) this.color = "GREEN";
        if (this.serverIp == null) this.serverIp = "";
        if (this.group == null) this.group = "";
        if (this.sound == null || this.sound.isEmpty()) this.sound = "NONE";
    }

    /**
     * Проверяет, активны ли сейчас уведомления для игрока с учётом ручной паузы и таймера.
     */
    public boolean isEffectivelyEnabled() {
        if (!this.enabled) {
            return false;
        }
        if (this.muteUntilDisconnect) {
            return false;
        }
        if (this.muteUntil > 0L) {
            if (System.currentTimeMillis() < this.muteUntil) {
                return false;
            } else {
                this.muteUntil = 0L; // Таймер истёк — автоматически снимаем паузу
            }
        }
        return true;
    }

    /**
     * Снимает все виды паузы (ручную, по таймеру и до перезахода).
     */
    public void clearMute() {
        this.enabled = true;
        this.muteUntil = 0L;
        this.muteUntilDisconnect = false;
    }

    /**
     * Сбрасывает временный мут при выходе с сервера.
     */
    public void onDisconnect() {
        if (this.muteUntilDisconnect) {
            this.muteUntilDisconnect = false;
            this.enabled = true;
        }
    }

    public SoundEvent getSoundEvent() {
        return getSoundEventByKey(this.sound);
    }

    public static SoundEvent getSoundEventByKey(String key) {
        if (key == null) return null;
        return switch (key.toUpperCase(Locale.ROOT)) {
            case "BELL" -> SoundEvents.NOTE_BLOCK_BELL.value();
            case "XP" -> SoundEvents.EXPERIENCE_ORB_PICKUP;
            case "LEVEL" -> SoundEvents.PLAYER_LEVELUP;
            case "CAT" -> SoundEvents.CAT_AMBIENT;
            case "ANVIL" -> SoundEvents.ANVIL_LAND;
            case "NOTE" -> SoundEvents.NOTE_BLOCK_PLING.value();
            default -> null;
        };
    }

    public ChatFormatting getChatFormatting() {
        return switch (this.color.toUpperCase(Locale.ROOT)) {
            case "AQUA" -> ChatFormatting.AQUA;
            case "GOLD" -> ChatFormatting.GOLD;
            case "LIGHT_PURPLE" -> ChatFormatting.LIGHT_PURPLE;
            case "YELLOW" -> ChatFormatting.YELLOW;
            case "RED" -> ChatFormatting.RED;
            case "WHITE" -> ChatFormatting.WHITE;
            default -> ChatFormatting.GREEN;
        };
    }

    public int getRgbColor() {
        return switch (this.color.toUpperCase(Locale.ROOT)) {
            case "AQUA" -> 0xFF55FFFF;
            case "GOLD" -> 0xFFFFAA00;
            case "LIGHT_PURPLE" -> 0xFFFF55FF;
            case "YELLOW" -> 0xFFFFFF55;
            case "RED" -> 0xFFFF5555;
            case "WHITE" -> 0xFFFFFFFF;
            default -> 0xFF55FF55;
        };
    }

    public boolean matchesServer(String currentNormalizedIp) {
        if (this.serverIp == null || this.serverIp.trim().isEmpty()) {
            return true;
        }
        if (currentNormalizedIp == null || currentNormalizedIp.isEmpty()) {
            return false;
        }
        String target = normalizeIp(this.serverIp);
        String currentHost = currentNormalizedIp.contains(":")
                ? currentNormalizedIp.substring(0, currentNormalizedIp.indexOf(':'))
                : currentNormalizedIp;
        return currentNormalizedIp.equals(target) || currentHost.equals(target);
    }

    public static String normalizeIp(String ip) {
        if (ip == null) return "";
        String s = ip.trim().toLowerCase(Locale.ROOT);
        if (s.endsWith(":25565")) {
            s = s.substring(0, s.length() - 6);
        }
        return s;
    }
}
