package com.example.joinnotifier;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class IdeasScreen extends Screen {
    private static final int PANEL_WIDTH = 304;
    private static final int PANEL_HEIGHT = 160;

    private final Screen parent;
    private EditBox ideaInput;
    private Button sendButton;

    private boolean isOnline = false;
    private boolean isBanned = false;
    private boolean isSending = false;
    private Component statusText = Component.translatable("gui.joinnotifier.ideas.checking");
    private Component subStatusText = Component.empty();
    private int statusColor = 0xFF555555;

    public IdeasScreen(Screen parent) {
        super(Component.translatable("gui.joinnotifier.ideas.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        this.ideaInput = new EditBox(
                this.font,
                left + 12,
                top + 56,
                PANEL_WIDTH - 24,
                22,
                Component.translatable("gui.joinnotifier.ideas.label")
        );
        this.ideaInput.setMaxLength(300);
        this.ideaInput.setHint(Component.translatable("gui.joinnotifier.ideas.hint"));
        this.ideaInput.setResponder(text -> updateButtonState());
        this.addRenderableWidget(this.ideaInput);

        this.sendButton = Button.builder(Component.translatable("gui.joinnotifier.ideas.send"), button -> {
            String text = this.ideaInput.getValue().trim();
            if (text.isEmpty() || !this.isOnline || this.isBanned || this.isSending) return;

            this.isSending = true;
            this.statusText = Component.translatable("gui.joinnotifier.ideas.sending");
            this.subStatusText = Component.empty();
            this.statusColor = 0xFF555555;
            updateButtonState();

            String author = this.minecraft != null && this.minecraft.getUser() != null
                    ? this.minecraft.getUser().getName()
                    : "Unknown";

            NetworkManager.submitIdeaAsync(author, text).thenAccept(res -> {
                if (this.minecraft != null) {
                    this.minecraft.execute(() -> {
                        this.isSending = false;
                        switch (res.status) {
                            case "OK" -> {
                                this.ideaInput.setValue("");
                                this.statusText = Component.translatable("gui.joinnotifier.ideas.sent");
                                this.subStatusText = Component.empty();
                                this.statusColor = 0xFF1F7A1F;
                            }
                            case "BANNED" -> {
                                this.isBanned = true;
                                this.statusText = Component.translatable("gui.joinnotifier.ideas.banned");
                                this.subStatusText = Component.translatable("gui.joinnotifier.ideas.ban_reason", res.detail);
                                this.statusColor = 0xFFAA2222;
                            }
                            default -> {
                                this.isOnline = false;
                                this.statusText = res.detail;
                                this.subStatusText = Component.empty();
                                this.statusColor = 0xFFAA2222;
                            }
                        }
                        updateButtonState();
                    });
                }
            });
        }).bounds(left + 12, top + PANEL_HEIGHT - 32, PANEL_WIDTH - 24, 20).build();

        this.sendButton.active = false;
        this.addRenderableWidget(this.sendButton);

        String username = this.minecraft != null && this.minecraft.getUser() != null
                ? this.minecraft.getUser().getName()
                : "";
        NetworkManager.syncAsync(username).thenAccept(res -> {
            if (this.minecraft != null) {
                this.minecraft.execute(() -> {
                    this.isOnline = res.online;
                    this.isBanned = res.banned;
                    if (!res.online) {
                        this.statusText = res.errorComponent;
                        this.subStatusText = Component.empty();
                        this.statusColor = 0xFFAA2222;
                    } else if (res.banned) {
                        this.statusText = Component.translatable("gui.joinnotifier.ideas.banned");
                        Component reasonComp = NetworkManager.isDefaultReason(res.banReason)
                                ? Component.translatable("gui.joinnotifier.net.default_reason")
                                : Component.literal(res.banReason);
                        this.subStatusText = Component.translatable("gui.joinnotifier.ideas.ban_reason", reasonComp);
                        this.statusColor = 0xFFAA2222;
                    } else {
                        this.statusText = Component.translatable("gui.joinnotifier.ideas.author", username);
                        this.subStatusText = Component.empty();
                        this.statusColor = 0xFF404040;
                    }
                    updateButtonState();
                });
            }
        });
    }

    private void updateButtonState() {
        if (this.sendButton == null || this.ideaInput == null) return;
        boolean hasText = !this.ideaInput.getValue().trim().isEmpty();
        this.sendButton.active = this.isOnline && !this.isBanned && !this.isSending && hasText;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        UiHelper.drawVanillaPanel(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);

        int backX = left + 8;
        int backY = top + 6;
        boolean backHover = UiHelper.isInside(mouseX, mouseY, backX, backY, 18, 16);
        UiHelper.drawSmallButton(graphics, backX, backY, 18, 16, backHover);
        graphics.drawString(this.font, "←", backX + 5, backY + 4, 0xFFFFFFFF, true);

        int titleW = this.font.width(this.title);
        graphics.drawString(this.font, this.title, left + (PANEL_WIDTH - titleW) / 2, top + 9, 0xFF404040, false);

        String shortStatus = this.font.plainSubstrByWidth(this.statusText.getString(), PANEL_WIDTH - 24);
        graphics.drawString(this.font, shortStatus, left + 12, top + 30, this.statusColor, false);

        String subStr = this.subStatusText.getString();
        if (!subStr.isEmpty()) {
            String shortSub = this.font.plainSubstrByWidth(subStr, PANEL_WIDTH - 24);
            graphics.drawString(this.font, shortSub, left + 12, top + 42, 0xFF662222, false);
        }

        String counter = this.ideaInput.getValue().length() + " / 300";
        int counterW = this.font.width(counter);
        graphics.drawString(this.font, counter, left + PANEL_WIDTH - 12 - counterW, top + 82, 0xFF555555, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int left = (this.width - PANEL_WIDTH) / 2;
            int top = (this.height - PANEL_HEIGHT) / 2;
            if (UiHelper.isInside(event.x(), event.y(), left + 8, top + 6, 18, 16)) {
                if (this.minecraft != null) this.minecraft.setScreen(this.parent);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}