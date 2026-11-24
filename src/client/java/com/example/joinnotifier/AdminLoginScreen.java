package com.example.joinnotifier;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public class AdminLoginScreen extends Screen {
    private static final int PANEL_WIDTH = 250;
    private static final int PANEL_HEIGHT = 136;

    private final Screen parent;
    private EditBox passwordInput;
    private Button loginButton;
    private Component statusText = Component.empty();
    private int statusColor = 0xFFCC3333;
    private boolean checking = false;

    public AdminLoginScreen(Screen parent) {
        super(Component.translatable("gui.joinnotifier.admin.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        if (NetworkManager.hasAdminSession()) {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new AdminPanelScreen(this.parent));
            }
            return;
        }

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        this.passwordInput = new EditBox(
                this.font,
                left + 12,
                top + 52,
                PANEL_WIDTH - 24,
                18,
                Component.translatable("gui.joinnotifier.admin.password_label")
        );
        this.passwordInput.setMaxLength(64);
        this.passwordInput.setHint(Component.translatable("gui.joinnotifier.admin.password_hint"));
        this.addRenderableWidget(this.passwordInput);

        this.loginButton = Button.builder(Component.translatable("gui.joinnotifier.admin.login"), button -> tryPasswordLogin())
                .bounds(left + 12, top + 82, PANEL_WIDTH - 24, 20)
                .build();
        this.addRenderableWidget(this.loginButton);

        tryAutoLicensedLogin();
    }

    private boolean isLicensedSession() {
        if (this.minecraft == null || this.minecraft.getUser() == null) return false;
        String name = this.minecraft.getUser().getName();
        UUID uuid = this.minecraft.getUser().getProfileId();
        String token = this.minecraft.getUser().getAccessToken();
        if (name == null || uuid == null || token == null || token.isEmpty() || "0".equals(token)) {
            return false;
        }
        UUID offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes());
        return !uuid.equals(offlineUuid);
    }

    private void tryAutoLicensedLogin() {
        if (!isLicensedSession()) {
            return;
        }

        this.checking = true;
        this.loginButton.active = false;
        this.statusText = Component.translatable("gui.joinnotifier.admin.checking_license");
        this.statusColor = UiHelper.getSecondaryTextColor();

        NetworkManager.tryLicenseAdminLoginAsync().thenAccept(ok -> {
            if (this.minecraft != null) {
                this.minecraft.execute(() -> {
                    this.checking = false;
                    this.loginButton.active = true;
                    if (ok) {
                        this.minecraft.setScreen(new AdminPanelScreen(this.parent));
                    } else {
                        this.statusText = Component.empty();
                    }
                });
            }
        });
    }

    private void tryPasswordLogin() {
        if (this.checking) return;
        String pass = this.passwordInput.getValue().trim();
        if (pass.isEmpty()) return;

        this.checking = true;
        this.loginButton.active = false;
        this.statusText = Component.translatable("gui.joinnotifier.admin.checking_password");
        this.statusColor = UiHelper.getSecondaryTextColor();

        NetworkManager.loginWithPasswordAsync(pass).thenAccept(res -> {
            if (this.minecraft != null) {
                this.minecraft.execute(() -> {
                    this.checking = false;
                    this.loginButton.active = true;
                    if (res.ok) {
                        this.minecraft.setScreen(new AdminPanelScreen(this.parent));
                    } else {
                        this.statusText = res.errorComponent;
                        this.statusColor = 0xFFCC2222;
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

        int titleW = this.font.width(this.title);
        graphics.drawString(this.font, this.title, left + (PANEL_WIDTH - titleW) / 2 + 6, top + 10, UiHelper.getPrimaryTextColor(), false);

        graphics.drawString(
                this.font,
                Component.translatable("gui.joinnotifier.admin.login_prompt"),
                left + 12,
                top + 38,
                UiHelper.getPrimaryTextColor(),
                false
        );

        if (!this.statusText.getString().isEmpty()) {
            graphics.drawString(this.font, this.statusText, left + 12, top + 110, this.statusColor, false);
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
