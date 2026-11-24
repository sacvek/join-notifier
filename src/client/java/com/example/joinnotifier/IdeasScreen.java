package com.example.joinnotifier;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class IdeasScreen extends Screen {
    private static final int PANEL_WIDTH = 290;
    private static final int PANEL_HEIGHT = 152;

    private final Screen parent;
    private EditBox ideaInput;
    private Button sendButton;

    private boolean checking = true;
    private boolean sending = false;
    private boolean internetOk = false;
    private boolean isBanned = false;
    private String banReason = "";
    private Component statusMessage = Component.empty();
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
                top + 48,
                PANEL_WIDTH - 24,
                20,
                Component.translatable("gui.joinnotifier.ideas.label")
        );
        this.ideaInput.setMaxLength(300);
        this.ideaInput.setHint(Component.translatable("gui.joinnotifier.ideas.hint"));
        this.ideaInput.setEditable(false);
        this.addRenderableWidget(this.ideaInput);

        this.sendButton = Button.builder(Component.translatable("gui.joinnotifier.ideas.send"), button -> sendIdea())
                .bounds(left + 12, top + 76, PANEL_WIDTH - 24, 20)
                .build();
        this.sendButton.active = false;
        this.addRenderableWidget(this.sendButton);

        startConnectionAndBanCheck();
    }

    private String getPlayerUsername() {
        if (this.minecraft != null && this.minecraft.getUser() != null) {
            return this.minecraft.getUser().getName();
        }
        return "Unknown";
    }

    private void startConnectionAndBanCheck() {
        this.checking = true;
        this.statusMessage = Component.translatable("gui.joinnotifier.ideas.checking");
        this.statusColor = UiHelper.getSecondaryTextColor();

        String username = getPlayerUsername();
        NetworkManager.syncAsync(username).thenAccept(res -> {
            if (this.minecraft != null) {
                this.minecraft.execute(() -> {
                    this.checking = false;
                    if (!res.online) {
                        this.internetOk = false;
                        this.statusMessage = res.errorComponent;
                        this.statusColor = 0xFFCC3333;
                        this.ideaInput.setEditable(false);
                        this.sendButton.active = false;
                    } else if (res.banned) {
                        this.internetOk = true;
                        this.isBanned = true;
                        this.banReason = res.banReason != null ? res.banReason : "";
                        this.statusMessage = Component.translatable("gui.joinnotifier.ideas.banned");
                        this.statusColor = 0xFFCC2222;
                        this.ideaInput.setEditable(false);
                        this.sendButton.active = false;
                    } else {
                        this.internetOk = true;
                        this.isBanned = false;
                        this.statusMessage = Component.empty();
                        this.ideaInput.setEditable(true);
                        this.sendButton.active = true;
                    }
                });
            }
        });
    }

    private void sendIdea() {
        String text = this.ideaInput.getValue().trim();
        if (text.isEmpty() || this.sending || !this.internetOk || this.isBanned) return;

        this.sending = true;
        this.sendButton.active = false;
        this.ideaInput.setEditable(false);
        this.statusMessage = Component.translatable("gui.joinnotifier.ideas.sending");
        this.statusColor = UiHelper.getSecondaryTextColor();

        String username = getPlayerUsername();
        NetworkManager.submitIdeaAsync(username, text).thenAccept(res -> {
            if (this.minecraft != null) {
                this.minecraft.execute(() -> {
                    this.sending = false;
                    if ("OK".equals(res.status)) {
                        this.ideaInput.setValue("");
                        this.statusMessage = Component.translatable("gui.joinnotifier.ideas.sent");
                        this.statusColor = 0xFF2E9E2E;
                        this.ideaInput.setEditable(true);
                        this.sendButton.active = true;
                    } else if ("BANNED".equals(res.status)) {
                        this.isBanned = true;
                        this.banReason = res.detail != null ? res.detail.getString() : "";
                        this.statusMessage = Component.translatable("gui.joinnotifier.ideas.banned");
                        this.statusColor = 0xFFCC2222;
                    } else {
                        this.statusMessage = res.detail;
                        this.statusColor = 0xFFCC3333;
                        this.ideaInput.setEditable(true);
                        this.sendButton.active = true;
                    }
                });
            }
        });
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

        UiHelper.drawLightbulbIcon(graphics, left + 34, top + 9);
        graphics.drawString(this.font, this.title, left + 46, top + 10, UiHelper.getPrimaryTextColor(), false);

        graphics.drawString(
                this.font,
                Component.translatable("gui.joinnotifier.ideas.author", getPlayerUsername()),
                left + 12,
                top + 34,
                UiHelper.getSecondaryTextColor(),
                false
        );

        if (!this.statusMessage.getString().isEmpty()) {
            graphics.drawString(this.font, this.statusMessage, left + 12, top + 104, this.statusColor, false);
        }

        if (this.isBanned && !this.banReason.isEmpty()) {
            Component reasonLine = Component.translatable("gui.joinnotifier.ideas.ban_reason", this.banReason);
            String trimmed = this.font.plainSubstrByWidth(reasonLine.getString(), PANEL_WIDTH - 24);
            graphics.drawString(this.font, trimmed, left + 12, top + 118, 0xFFAA2222, false);
        }
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
