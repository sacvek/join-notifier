package com.example.joinnotifier;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class AdminPanelScreen extends Screen {
    private static final int PANEL_WIDTH = 310;
    private static final int PANEL_HEIGHT = 256;
    private static final int ROW_HEIGHT = 26;

    private enum Tab {
        IDEAS, SERVERS, BANS, ADMINS
    }

    private final Screen parent;
    private Tab currentTab = Tab.IDEAS;
    private int scrollOffset = 0;
    private boolean loading = true;

    private boolean banDialogOpen = false;
    private String banTargetUser = "";

    private Button tabIdeasBtn;
    private Button tabServersBtn;
    private Button tabBansBtn;
    private Button tabAdminsBtn;

    private EditBox inputBox;
    private EditBox reasonBox;
    private Button addActionButton;

    private Button confirmBanButton;
    private Button cancelBanButton;

    private List<NetworkManager.IdeaItem> ideas = new ArrayList<>();
    private List<String> blockedServers = new ArrayList<>();
    private List<NetworkManager.BanItem> bannedUsers = new ArrayList<>();
    private List<String> adminWhitelist = new ArrayList<>();

    public AdminPanelScreen(Screen parent) {
        super(Component.translatable("gui.joinnotifier.admin.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        // 4 вкладки сверху
        int tabY = top + 25;
        int tabW = (PANEL_WIDTH - 36) / 4;
        this.tabIdeasBtn = this.addRenderableWidget(Button.builder(Component.translatable("gui.joinnotifier.admin.tab.ideas"), b -> switchTab(Tab.IDEAS))
                .bounds(left + 12, tabY, tabW, 18).build());
        this.tabServersBtn = this.addRenderableWidget(Button.builder(Component.translatable("gui.joinnotifier.admin.tab.servers"), b -> switchTab(Tab.SERVERS))
                .bounds(left + 16 + tabW, tabY, tabW, 18).build());
        this.tabBansBtn = this.addRenderableWidget(Button.builder(Component.translatable("gui.joinnotifier.admin.tab.bans"), b -> switchTab(Tab.BANS))
                .bounds(left + 20 + tabW * 2, tabY, tabW, 18).build());
        this.tabAdminsBtn = this.addRenderableWidget(Button.builder(Component.translatable("gui.joinnotifier.admin.tab.admins"), b -> switchTab(Tab.ADMINS))
                .bounds(left + 24 + tabW * 3, tabY, tabW, 18).build());

        this.inputBox = new EditBox(
                this.font,
                left + 12,
                top + PANEL_HEIGHT - 54,
                PANEL_WIDTH - 24,
                18,
                Component.translatable("gui.joinnotifier.admin.input_label")
        );
        this.inputBox.setMaxLength(64);
        this.addRenderableWidget(this.inputBox);

        this.reasonBox = new EditBox(
                this.font,
                left + 12,
                top + PANEL_HEIGHT - 54,
                PANEL_WIDTH - 24,
                18,
                Component.translatable("gui.joinnotifier.admin.reason_label")
        );
        this.reasonBox.setMaxLength(120);
        this.addRenderableWidget(this.reasonBox);

        this.addActionButton = Button.builder(Component.translatable("gui.joinnotifier.admin.add"), button -> {
            String value = this.inputBox.getValue().trim();
            if (value.isEmpty()) return;

            if (this.currentTab == Tab.BANS) {
                String reason = this.reasonBox.getValue().trim();
                this.inputBox.setValue("");
                this.reasonBox.setValue("");
                performAction("ban_user", null, value, reason);
                return;
            }

            String action = switch (this.currentTab) {
                case SERVERS -> "block_server";
                case ADMINS -> "add_admin";
                default -> null;
            };

            if (action != null) {
                this.inputBox.setValue("");
                performAction(action, null, value, null);
            }
        }).bounds(left + 12, top + PANEL_HEIGHT - 30, PANEL_WIDTH - 24, 20).build();
        this.addRenderableWidget(this.addActionButton);

        int modalBtnW = (PANEL_WIDTH - 68) / 2;
        this.confirmBanButton = Button.builder(Component.translatable("gui.joinnotifier.admin.confirm_ban"), button -> {
            String reason = this.reasonBox.getValue().trim();
            String target = this.banTargetUser;
            this.banDialogOpen = false;
            this.reasonBox.setValue("");
            updateControlsVisibility();
            performAction("ban_user", null, target, reason);
        }).bounds(left + 30, top + 132, modalBtnW, 20).build();
        this.addRenderableWidget(this.confirmBanButton);

        this.cancelBanButton = Button.builder(Component.translatable("gui.joinnotifier.admin.cancel"), button -> {
            this.banDialogOpen = false;
            this.reasonBox.setValue("");
            updateControlsVisibility();
        }).bounds(left + 38 + modalBtnW, top + 132, modalBtnW, 20).build();
        this.addRenderableWidget(this.cancelBanButton);

        updateControlsVisibility();
        reloadData();
    }

    private void switchTab(Tab tab) {
        this.currentTab = tab;
        this.scrollOffset = 0;
        this.banDialogOpen = false;
        if (this.inputBox != null) this.inputBox.setValue("");
        if (this.reasonBox != null) this.reasonBox.setValue("");
        updateControlsVisibility();
    }

    private void openBanDialog(String username) {
        this.banTargetUser = username;
        this.banDialogOpen = true;
        this.reasonBox.setValue("");
        updateControlsVisibility();
    }

    private void updateControlsVisibility() {
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        boolean showTabs = !this.banDialogOpen;
        this.tabIdeasBtn.visible = showTabs;
        this.tabServersBtn.visible = showTabs;
        this.tabBansBtn.visible = showTabs;
        this.tabAdminsBtn.visible = showTabs;

        this.confirmBanButton.visible = this.banDialogOpen;
        this.cancelBanButton.visible = this.banDialogOpen;

        if (this.banDialogOpen) {
            this.inputBox.visible = false;
            this.addActionButton.visible = false;

            this.reasonBox.visible = true;
            this.reasonBox.setX(left + 30);
            this.reasonBox.setY(top + 104);
            this.reasonBox.setWidth(PANEL_WIDTH - 60);
            this.reasonBox.setHint(Component.translatable("gui.joinnotifier.admin.ban_dialog_hint"));
            return;
        }

        if (this.currentTab == Tab.IDEAS) {
            this.inputBox.visible = false;
            this.reasonBox.visible = false;
            this.addActionButton.visible = false;
        } else if (this.currentTab == Tab.BANS) {
            this.inputBox.visible = true;
            this.inputBox.setX(left + 12);
            this.inputBox.setY(top + PANEL_HEIGHT - 76);
            this.inputBox.setWidth(PANEL_WIDTH - 24);
            this.inputBox.setHint(Component.translatable("gui.joinnotifier.admin.ban_user_hint"));

            this.reasonBox.visible = true;
            this.reasonBox.setX(left + 12);
            this.reasonBox.setY(top + PANEL_HEIGHT - 54);
            this.reasonBox.setWidth(PANEL_WIDTH - 24);
            this.reasonBox.setHint(Component.translatable("gui.joinnotifier.admin.ban_reason_hint"));

            this.addActionButton.visible = true;
            this.addActionButton.setMessage(Component.translatable("gui.joinnotifier.admin.ban_user_btn"));
        } else {
            this.inputBox.visible = true;
            this.inputBox.setX(left + 12);
            this.inputBox.setY(top + PANEL_HEIGHT - 54);
            this.inputBox.setWidth(PANEL_WIDTH - 24);

            this.reasonBox.visible = false;
            this.addActionButton.visible = true;

            if (this.currentTab == Tab.SERVERS) {
                this.inputBox.setHint(Component.translatable("gui.joinnotifier.admin.server_hint"));
                this.addActionButton.setMessage(Component.translatable("gui.joinnotifier.admin.block_server"));
            } else {
                this.inputBox.setHint(Component.translatable("gui.joinnotifier.admin.add_admin_hint"));
                this.addActionButton.setMessage(Component.translatable("gui.joinnotifier.admin.add_admin_btn"));
            }
        }
    }

    private int getVisibleRows() {
        return this.currentTab == Tab.BANS ? 4 : 5;
    }

    private void reloadData() {
        this.loading = true;
        NetworkManager.fetchAdminDataAsync().thenAccept(data -> {
            if (this.minecraft != null) {
                this.minecraft.execute(() -> {
                    this.loading = false;
                    if (data.ok) {
                        this.ideas = data.ideas;
                        this.blockedServers = data.blockedServers;
                        this.bannedUsers = data.bannedUsers;
                        this.adminWhitelist = data.adminWhitelist;
                        clampScroll();
                    }
                });
            }
        });
    }

    private void performAction(String action, String id, String target, String reason) {
        this.loading = true;
        NetworkManager.sendAdminActionAsync(action, id, target, reason).thenAccept(ok -> reloadData());
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

        if (this.banDialogOpen) {
            UiHelper.drawInsetBox(graphics, left + 20, top + 64, PANEL_WIDTH - 40, 100);
            Component dialogTitle = Component.translatable("gui.joinnotifier.admin.ban_dialog_title", this.banTargetUser);
            Component dialogDesc = Component.translatable("gui.joinnotifier.admin.ban_dialog_desc");
            graphics.drawString(this.font, dialogTitle, left + 30, top + 74, 0xFFFFD83D, true);
            graphics.drawString(this.font, dialogDesc, left + 30, top + 89, 0xFFDDDDDD, false);
            return;
        }

        int visibleRows = getVisibleRows();
        int listLeft = left + 12;
        int listTop = top + 48;
        int listWidth = PANEL_WIDTH - 24;
        int listHeight = visibleRows * ROW_HEIGHT + 4;
        UiHelper.drawInsetBox(graphics, listLeft, listTop, listWidth, listHeight);

        if (this.loading) {
            Component msg = Component.translatable("gui.joinnotifier.admin.loading");
            graphics.drawString(this.font, msg, listLeft + (listWidth - this.font.width(msg)) / 2,
                    listTop + (listHeight - 8) / 2, 0xFFDDDDDD, true);
            return;
        }

        int totalSize = getCurrentListSize();
        clampScroll();

        if (totalSize == 0) {
            Component msg = Component.translatable("gui.joinnotifier.empty");
            graphics.drawString(this.font, msg, listLeft + (listWidth - this.font.width(msg)) / 2,
                    listTop + (listHeight - 8) / 2, 0xFFDDDDDD, true);
            return;
        }

        Component banLabel = Component.translatable("gui.joinnotifier.admin.ban_short");
        int banBtnW = Math.max(28, this.font.width(banLabel) + 8);

        Component unbanLabel = Component.translatable("gui.joinnotifier.admin.unban_short");
        int unbanBtnW = Math.max(44, this.font.width(unbanLabel) + 10);

        int endIndex = Math.min(scrollOffset + visibleRows, totalSize);
        for (int i = scrollOffset; i < endIndex; i++) {
            int rowIndex = i - scrollOffset;
            int rowX = listLeft + 2;
            int rowY = listTop + 2 + rowIndex * ROW_HEIGHT;
            int rowW = listWidth - 4;

            boolean hovered = UiHelper.isInside(mouseX, mouseY, rowX, rowY, rowW, ROW_HEIGHT - 1);
            graphics.fill(rowX, rowY, rowX + rowW, rowY + ROW_HEIGHT - 1, hovered ? 0xFF7A7A7A : 0xFF6B6B6B);

            int btnH = 18;
            int btnY = rowY + 3;

            if (this.currentTab == Tab.IDEAS) {
                NetworkManager.IdeaItem idea = this.ideas.get(i);
                int rejectBtnX = rowX + rowW - 18 - 3;
                int banBtnX = rejectBtnX - banBtnW - 3;

                graphics.drawString(this.font, idea.author + ":", rowX + 5, rowY + 3, 0xFFFFD83D, true);
                int maxTextW = banBtnX - (rowX + 8);
                String shortText = this.font.plainSubstrByWidth(idea.text, Math.max(20, maxTextW));
                graphics.drawString(this.font, shortText, rowX + 5, rowY + 14, 0xFFFFFFFF, false);

                boolean hoverBan = UiHelper.isInside(mouseX, mouseY, banBtnX, btnY, banBtnW, btnH);
                UiHelper.drawSmallButton(graphics, banBtnX, btnY, banBtnW, btnH, hoverBan);
                graphics.drawString(this.font, banLabel, banBtnX + 4, btnY + 5, hoverBan ? 0xFFFF5555 : 0xFFFFAAAA, true);

                boolean hoverReject = UiHelper.isInside(mouseX, mouseY, rejectBtnX, btnY, 18, btnH);
                UiHelper.drawSmallButton(graphics, rejectBtnX, btnY, 18, btnH, hoverReject);
                graphics.drawString(this.font, "✕", rejectBtnX + 5, btnY + 5, hoverReject ? 0xFFFF6666 : 0xFFFFFFFF, true);

            } else if (this.currentTab == Tab.BANS) {
                NetworkManager.BanItem ban = this.bannedUsers.get(i);
                int unbanBtnX = rowX + rowW - unbanBtnW - 3;

                graphics.drawString(this.font, ban.name, rowX + 5, rowY + 3, 0xFFFF8888, true);

                Component reasonVal = NetworkManager.isDefaultReason(ban.reason)
                        ? Component.translatable("gui.joinnotifier.net.default_reason")
                        : Component.literal(ban.reason);
                String fullReasonStr = Component.translatable("gui.joinnotifier.admin.reason_prefix", reasonVal).getString();
                int maxReasonW = unbanBtnX - (rowX + 8);
                String shortReason = this.font.plainSubstrByWidth(fullReasonStr, Math.max(20, maxReasonW));
                graphics.drawString(this.font, shortReason, rowX + 5, rowY + 14, 0xFFDDDDDD, false);

                boolean hoverUnban = UiHelper.isInside(mouseX, mouseY, unbanBtnX, btnY, unbanBtnW, btnH);
                UiHelper.drawSmallButton(graphics, unbanBtnX, btnY, unbanBtnW, btnH, hoverUnban);
                graphics.drawString(this.font, unbanLabel, unbanBtnX + 5, btnY + 5, hoverUnban ? 0xFF66FF66 : 0xFFFFFFFF, true);

            } else {
                String value = this.currentTab == Tab.SERVERS ? this.blockedServers.get(i) : this.adminWhitelist.get(i);
                int delBtnX = rowX + rowW - 18 - 3;

                graphics.drawString(this.font, value, rowX + 6, rowY + 8, 0xFFFFFFFF, true);

                boolean hoverDel = UiHelper.isInside(mouseX, mouseY, delBtnX, btnY, 18, btnH);
                UiHelper.drawSmallButton(graphics, delBtnX, btnY, 18, btnH, hoverDel);
                graphics.drawString(this.font, "-", delBtnX + 6, btnY + 5, hoverDel ? 0xFFFF6666 : 0xFFFFFFFF, true);
            }
        }

        if (this.currentTab == Tab.ADMINS) {
            Component note = Component.translatable("gui.joinnotifier.admin.license_note");
            graphics.drawString(this.font, note, listLeft, listTop + listHeight + 3, 0xFF555555, false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double mx = event.x();
            double my = event.y();

            int left = (this.width - PANEL_WIDTH) / 2;
            int top = (this.height - PANEL_HEIGHT) / 2;

            if (UiHelper.isInside(mx, my, left + 8, top + 6, 18, 16)) {
                if (this.banDialogOpen) {
                    this.banDialogOpen = false;
                    this.reasonBox.setValue("");
                    updateControlsVisibility();
                    return true;
                }
                if (this.minecraft != null) this.minecraft.setScreen(this.parent);
                return true;
            }

            if (!this.loading && !this.banDialogOpen) {
                int visibleRows = getVisibleRows();
                int listLeft = left + 12;
                int listTop = top + 48;
                int listWidth = PANEL_WIDTH - 24;
                int totalSize = getCurrentListSize();
                int endIndex = Math.min(scrollOffset + visibleRows, totalSize);

                Component banLabel = Component.translatable("gui.joinnotifier.admin.ban_short");
                int banBtnW = Math.max(28, this.font.width(banLabel) + 8);

                Component unbanLabel = Component.translatable("gui.joinnotifier.admin.unban_short");
                int unbanBtnW = Math.max(44, this.font.width(unbanLabel) + 10);

                for (int i = scrollOffset; i < endIndex; i++) {
                    int rowIndex = i - scrollOffset;
                    int rowX = listLeft + 2;
                    int rowY = listTop + 2 + rowIndex * ROW_HEIGHT;
                    int rowW = listWidth - 4;

                    int btnH = 18;
                    int btnY = rowY + 3;

                    if (this.currentTab == Tab.IDEAS) {
                        NetworkManager.IdeaItem idea = this.ideas.get(i);
                        int rejectBtnX = rowX + rowW - 18 - 3;
                        int banBtnX = rejectBtnX - banBtnW - 3;

                        if (UiHelper.isInside(mx, my, banBtnX, btnY, banBtnW, btnH)) {
                            openBanDialog(idea.author);
                            return true;
                        }
                        if (UiHelper.isInside(mx, my, rejectBtnX, btnY, 18, btnH)) {
                            performAction("reject_idea", idea.id, null, null);
                            return true;
                        }
                    } else if (this.currentTab == Tab.BANS) {
                        NetworkManager.BanItem ban = this.bannedUsers.get(i);
                        int unbanBtnX = rowX + rowW - unbanBtnW - 3;

                        if (UiHelper.isInside(mx, my, unbanBtnX, btnY, unbanBtnW, btnH)) {
                            performAction("unban_user", null, ban.name, null);
                            return true;
                        }
                    } else {
                        int delBtnX = rowX + rowW - 18 - 3;
                        if (UiHelper.isInside(mx, my, delBtnX, btnY, 18, btnH)) {
                            if (this.currentTab == Tab.SERVERS) {
                                performAction("unblock_server", null, this.blockedServers.get(i), null);
                            } else {
                                performAction("remove_admin", null, this.adminWhitelist.get(i), null);
                            }
                            return true;
                        }
                    }
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.banDialogOpen) return true;
        int totalSize = getCurrentListSize();
        int visibleRows = getVisibleRows();
        if (totalSize > visibleRows) {
            if (scrollY < 0) scrollOffset++;
            else if (scrollY > 0) scrollOffset--;
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private int getCurrentListSize() {
        return switch (this.currentTab) {
            case IDEAS -> this.ideas.size();
            case SERVERS -> this.blockedServers.size();
            case BANS -> this.bannedUsers.size();
            case ADMINS -> this.adminWhitelist.size();
        };
    }

    private void clampScroll() {
        int maxScroll = Math.max(0, getCurrentListSize() - getVisibleRows());
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        if (scrollOffset < 0) scrollOffset = 0;
    }
}