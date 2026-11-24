package com.example.joinnotifier;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvent;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

public class JoinNotifierClient implements ClientModInitializer {
	private static KeyMapping openGuiKey;

	private Set<String> onlinePlayers = new HashSet<>();
	private boolean initialized = false;
	private int warmupTicks = 0;

	@Override
	public void onInitializeClient() {
		ConfigManager.load();
		NetworkManager.loadCachedBlockedServers();
		NetworkManager.refreshSyncIfNeeded("", true);

		openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.joinnotifier.open",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_K,
				KeyMapping.Category.MISC
		));

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				dispatcher.register(ClientCommandManager.literal("joinnotify")
						.executes(context -> {
							Minecraft client = context.getSource().getClient();
							client.execute(() -> client.setScreen(new JoinNotifierScreen()));
							return 1;
						}))
		);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openGuiKey.consumeClick()) {
				client.setScreen(new JoinNotifierScreen());
			}

			if (client.level == null || client.getConnection() == null) {
				if (initialized) {
					ConfigManager.onDisconnectAll();
				}
				onlinePlayers.clear();
				initialized = false;
				warmupTicks = 0;
				return;
			}

			String selfName = client.getUser() != null ? client.getUser().getName() : "";
			NetworkManager.refreshSyncIfNeeded(selfName, !initialized);

			Set<String> currentPlayers = new HashSet<>();
			for (PlayerInfo entry : client.getConnection().getOnlinePlayers()) {
				if (entry.getProfile() != null && entry.getProfile().name() != null) {
					currentPlayers.add(entry.getProfile().name());
				}
			}

			if (!initialized) {
				warmupTicks++;
				if (warmupTicks >= 40) {
					onlinePlayers = currentPlayers;
					initialized = true;
				}
				return;
			}

			if (ConfigManager.isGlobalMute() || NetworkManager.isCurrentServerBlocked(client)) {
				onlinePlayers = currentPlayers;
				return;
			}

			String currentServerIp = getCurrentNormalizedIp(client);

			// 1. Проверка вошедших игроков
			for (String playerName : currentPlayers) {
				if (!onlinePlayers.contains(playerName)) {
					PlayerEntry entry = ConfigManager.getEntry(playerName);
					if (entry != null && entry.isEffectivelyEnabled() && entry.matchesServer(currentServerIp)) {
						dispatchNotification(client, entry, playerName, true);
					}
				}
			}

			// 2. Проверка вышедших игроков
			for (String oldPlayer : onlinePlayers) {
				if (!currentPlayers.contains(oldPlayer)) {
					PlayerEntry entry = ConfigManager.getEntry(oldPlayer);
					if (entry != null && entry.isEffectivelyEnabled() && entry.notifyLeave && entry.matchesServer(currentServerIp)) {
						dispatchNotification(client, entry, oldPlayer, false);
					}
				}
			}

			onlinePlayers = currentPlayers;
		});
	}

	private String getCurrentNormalizedIp(Minecraft client) {
		ServerData data = client.getCurrentServer();
		if (data == null || data.ip == null) return "";
		return PlayerEntry.normalizeIp(data.ip);
	}

	private void dispatchNotification(Minecraft client, PlayerEntry entry, String actualName, boolean isJoin) {
		ChatFormatting nameColor = entry.getChatFormatting();

		MutableComponent styledPlayer = Component.empty();
		if (entry.tag != null && !entry.tag.isEmpty()) {
			styledPlayer.append(Component.literal("[" + entry.tag + "] ").withStyle(nameColor));
		}
		styledPlayer.append(Component.literal(actualName).withStyle(nameColor, ChatFormatting.BOLD));

		String translationKey = isJoin ? "chat.joinnotifier.joined" : "chat.joinnotifier.left";
		Component body = Component.translatable(translationKey, styledPlayer).withStyle(ChatFormatting.GRAY);

		ConfigManager.NotifyMode mode = ConfigManager.getNotifyMode();

		if (mode == ConfigManager.NotifyMode.CHAT || mode == ConfigManager.NotifyMode.BOTH) {
			Component chatMessage = Component.literal("[JoinNotify] ").withStyle(ChatFormatting.GOLD)
					.append(body);
			client.gui.getChat().addMessage(chatMessage);
		}

		if (mode == ConfigManager.NotifyMode.TOAST || mode == ConfigManager.NotifyMode.BOTH) {
			SystemToast.add(
					client.getToastManager(),
					SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
					Component.literal("Join Notifier").withStyle(ChatFormatting.GOLD),
					Component.translatable(translationKey, styledPlayer)
			);
		}

		// Проигрывание выбранного звука
		SoundEvent sound = entry.getSoundEvent();
		if (sound != null && client.player != null) {
			client.player.playSound(sound, 1.0F, 1.0F);
		}
	}
}
