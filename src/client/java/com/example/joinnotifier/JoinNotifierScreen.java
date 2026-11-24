package com.example.joinnotifier;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.*;

public class JoinNotifierScreen extends Screen {
    private static final int PANEL_WIDTH = 290;
    private static final int PANEL_HEIGHT = 250;
    private static final int ROW_HEIGHT = 23;
    private static final int VISIBLE_ROWS = 5;

    private static class ListRow {
        final boolean isGroupHeader;
        final String groupName;
        final PlayerEntry player;

        ListRow(String groupName) {
            this.isGroupHeader = true;
            this.groupName = groupName;
            this.player = null;
        }

        ListRow(PlayerEntry player) {
            this.isGroupHeader = false;
            this.groupName = player.group;
            this.player = player;
        }
    }

    private EditBox nameInput;
    private Button filterButton;
    private boolean onlyOnline = false;
    private int scrollOffset = 0;

    public JoinNotifierScreen() {
        super(Component.translatable("gui.joinnotifier.title"));
    }

    @Override
    protected void init() {
        super.init();

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        // Фильтр списка: Все / Только онлайн
        this.filterButton = Button.builder(getFilterLabel(), button -> {
            this.onlyOnline = !this.onlyOnline;
            button.setMessage(getFilterLabel());
            clampScroll();
        }).bounds(left + 12, top + 26, 126, 18).build();
        this.addRenderableWidget(this.filterButton);

        this.nameInput = new EditBox(
                this.font,
                left + 12,
                top + PANEL_HEIGHT - 56,
                PANEL_WIDTH - 24,
                18,
                Component.translatable("gui.joinnotifier.input_label")
        );
        this.nameInput.setMaxLength(16);
        this.nameInput.setHint(Component.translatable("gui.joinnotifier.input_hint"));
        this.addRenderableWidget(this.nameInput);

        int btnWidth = (PANEL_WIDTH - 28) / 2;
        int btnY = top + PANEL_HEIGHT - 32;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.joinnotifier.add"), button -> {
            if (ConfigManager.addPlayer(this.nameInput.getValue())) {
                this.nameInput.setValue("");
                clampScroll();
            }
        }).bounds(left + 12, btnY, btnWidth, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.joinnotifier.remove"), button -> {
            if (ConfigManager.removePlayer(this.nameInput.getValue())) {
                this.nameInput.setValue("");
                clampScroll();
            }
        }).bounds(left + 16 + btnWidth, btnY, btnWidth, 20).build());
    }

    private Component getFilterLabel() {
        return Component.translatable(this.onlyOnline
                ? "gui.joinnotifier.filter.online"
                : "gui.joinnotifier.filter.all");
    }

    private List<ListRow> buildDisplayRows() {
        List<ListRow> rows = new ArrayList<>();
        List<PlayerEntry> all = ConfigManager.getPlayers();

        Map<String, List<PlayerEntry>> grouped = new LinkedHashMap<>();
        List<PlayerEntry> ungrouped = new ArrayList<>();

        for (PlayerEntry p : all) {
            if (this.onlyOnline && !UiHelper.isPlayerOnline(p.name)) {
                continue;
            }
            if (p.group != null && !p.group.trim().isEmpty()) {
                grouped.computeIfAbsent(p.group.trim(), k -> new ArrayList<>()).add(p);
            } else {
                ungrouped.add(p);
            }
        }

        for (Map.Entry<String, List<PlayerEntry>> entry : grouped.entrySet()) {
            String grp = entry.getKey();
            rows.add(new ListRow(grp));
            if (!ConfigManager.isGroupCollapsed(grp)) {
                for (PlayerEntry p : entry.getValue()) {
                    rows.add(new ListRow(p));
                }
            }
        }

        for (PlayerEntry p : ungrouped) {
            rows.add(new ListRow(p));
        }

        return rows;
    }

    private List<String> getAutocompleteSuggestions() {
        List<String> result = new ArrayList<>();
        if (this.nameInput == null || this.minecraft == null || this.minecraft.getConnection() == null) {
            return result;
        }
        String typed = this.nameInput.getValue().trim().toLowerCase(Locale.ROOT);
        if (typed.isEmpty()) return result;

        for (PlayerInfo info : this.minecraft.getConnection().getOnlinePlayers()) {
            if (info.getProfile() != null && info.getProfile().name() != null) {
                String onlineName = info.getProfile().name();
                if (onlineName.toLowerCase(Locale.ROOT).startsWith(typed)
                        && !onlineName.equalsIgnoreCase(typed)) {
                    result.add(onlineName);
                    if (result.size() >= 4) break;
                }
            }
        }
        return result;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        UiHelper.drawVanillaPanel(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);

        // 1. Кнопка общих настроек (шестерёнка слева сверху)
        int settingsX = left + 8;
        int settingsY = top + 6;
        boolean settingsHover = UiHelper.isInside(mouseX, mouseY, settingsX, settingsY, 16, 16);
        UiHelper.drawSmallButton(graphics, settingsX, settingsY, 16, 16, settingsHover);
        UiHelper.drawGearIcon(graphics, settingsX + 4, settingsY + 4);

        // 2. Кнопка "Не беспокоить" (колокольчик)
        int dndX = left + 27;
        int dndY = top + 6;
        boolean dndHover = UiHelper.isInside(mouseX, mouseY, dndX, dndY, 16, 16);
        UiHelper.drawSmallButton(graphics, dndX, dndY, 16, 16, dndHover);
        UiHelper.drawBellIcon(graphics, dndX + 4, dndY + 3, ConfigManager.isGlobalMute());
        if (ConfigManager.isGlobalMute()) {
            UiHelper.drawRedSlash(graphics, dndX + 3, dndY + 3, 10);
        }

        // 3. Кнопка Ideas (справа сверху)
        int ideasW = 48;
        int ideasX = left + PANEL_WIDTH - ideasW - 8;
        int ideasY = top + 6;
        boolean ideasHover = UiHelper.isInside(mouseX, mouseY, ideasX, ideasY, ideasW, 16);
        UiHelper.drawSmallButton(graphics, ideasX, ideasY, ideasW, 16, ideasHover);
        UiHelper.drawLightbulbIcon(graphics, ideasX + 4, ideasY + 3);
        graphics.drawString(this.font, "Ideas", ideasX + 15, ideasY + 4, 0xFFFFFFFF, true);

        // Заголовок
        int titleWidth = this.font.width(this.title);
        graphics.drawString(
                this.font,
                this.title,
                left + (PANEL_WIDTH - titleWidth) / 2 + 4,
                top + 10,
                UiHelper.getPrimaryTextColor(),
                false
        );

        // Статус "Не беспокоить" справа от фильтра
        if (ConfigManager.isGlobalMute()) {
            graphics.drawString(
                    this.font,
                    Component.translatable("gui.joinnotifier.dnd_on"),
                    left + 144,
                    top + 31,
                    0xFFFF4444,
                    false
            );
        }

        // Список игроков и групп
        int listLeft = left + 12;
        int listTop = top + 48;
        int listWidth = PANEL_WIDTH - 24;
        int listHeight = VISIBLE_ROWS * ROW_HEIGHT + 4;
        UiHelper.drawInsetBox(graphics, listLeft, listTop, listWidth, listHeight);

        List<ListRow> rows = buildDisplayRows();
        clampScroll(rows.size());

        if (rows.isEmpty()) {
            Component emptyText = Component.translatable("gui.joinnotifier.empty");
            int emptyWidth = this.font.width(emptyText);
            graphics.drawString(
                    this.font,
                    emptyText,
                    listLeft + (listWidth - emptyWidth) / 2,
                    listTop + (listHeight - 8) / 2,
                    0xFFDDDDDD,
                    true
            );
        } else {
            int endIndex = Math.min(scrollOffset + VISIBLE_ROWS, rows.size());
            for (int i = scrollOffset; i < endIndex; i++) {
                int rowIndex = i - scrollOffset;
                int rowX = listLeft + 2;
                int rowY = listTop + 2 + rowIndex * ROW_HEIGHT;
                int rowW = listWidth - 4;

                ListRow row = rows.get(i);
                boolean rowHovered = UiHelper.isInside(mouseX, mouseY, rowX, rowY, rowW, ROW_HEIGHT - 1);

                if (row.isGroupHeader) {
                    // Отрисовка заголовка группы
                    graphics.fill(rowX, rowY, rowX + rowW, rowY + ROW_HEIGHT - 1,
                            rowHovered ? 0xFF4F5D6B : 0xFF3F4B57);
                    boolean collapsed = ConfigManager.isGroupCollapsed(row.groupName);
                    String arrow = collapsed ? "▶ " : "▼ ";
                    graphics.drawString(this.font, arrow + row.groupName, rowX + 6, rowY + 7, 0xFFFFD83D, true);

                    int btnSize = 16;
                    int btnY = rowY + 3;
                    int groupToggleX = rowX + rowW - btnSize - 3;
                    boolean hoverGrpToggle = UiHelper.isInside(mouseX, mouseY, groupToggleX, btnY, btnSize, btnSize);
                    UiHelper.drawSmallButton(graphics, groupToggleX, btnY, btnSize, btnSize, hoverGrpToggle);
                    graphics.drawString(this.font, "=", groupToggleX + 5, btnY + 4, 0xFFFFFFFF, true);
                } else {
                    PlayerEntry entry = row.player;
                    int bgNormal = UiHelper.isDarkTheme() ? 0xFF2D2D2D : 0xFF6B6B6B;
                    int bgHover = UiHelper.isDarkTheme() ? 0xFF3E3E3E : 0xFF7A7A7A;
                    graphics.fill(rowX, rowY, rowX + rowW, rowY + ROW_HEIGHT - 1,
                            rowHovered ? bgHover : bgNormal);

                    int indent = (entry.group != null && !entry.group.isEmpty()) ? 8 : 0;

                    // Голова скина и индикатор онлайн/оффлайн
                    UiHelper.drawPlayerHead(graphics, entry.name, rowX + 4 + indent, rowY + 5, 12);
                    UiHelper.drawOnlineDot(graphics, rowX + 13 + indent, rowY + 14, UiHelper.isPlayerOnline(entry.name));

                    int btnSize = 16;
                    int btnY = rowY + 3;
                    int removeBtnX = rowX + rowW - btnSize - 3;
                    int toggleBtnX = removeBtnX - btnSize - 3;
                    int editBtnX = toggleBtnX - btnSize - 3;

                    boolean active = entry.isEffectivelyEnabled();
                    String prefix = (entry.tag != null && !entry.tag.isEmpty()) ? "[" + entry.tag + "] " : "";
                    String displayName = prefix + entry.name;
                    int textColor = active ? entry.getRgbColor() : 0xFFAAAAAA;

                    int maxNameW = editBtnX - (rowX + 22 + indent);
                    String trimmedName = this.font.plainSubstrByWidth(displayName, Math.max(20, maxNameW));
                    graphics.drawString(this.font, trimmedName, rowX + 20 + indent, rowY + 7, textColor, true);

                    int nameEndW = this.font.width(trimmedName);
                    String badges = "";
                    if (entry.notifyLeave) badges += "⇄";
                    if (!"NONE".equalsIgnoreCase(entry.sound)) badges += "♪";
                    if (entry.serverIp != null && !entry.serverIp.isEmpty()) badges += " [IP]";
                    if (!badges.isEmpty() && rowX + 22 + indent + nameEndW + this.font.width(badges) < editBtnX) {
                        graphics.drawString(this.font, badges, rowX + 22 + indent + nameEndW, rowY + 7, 0xFFCCCCCC, false);
                    }

                    // Кнопка [⚙] (Настройки игрока)
                    boolean hoverEdit = UiHelper.isInside(mouseX, mouseY, editBtnX, btnY, btnSize, btnSize);
                    UiHelper.drawSmallButton(graphics, editBtnX, btnY, btnSize, btnSize, hoverEdit);
                    graphics.drawString(this.font, "⚙", editBtnX + 4, btnY + 4,
                            hoverEdit ? 0xFFFFD83D : 0xFFFFFFFF, true);

                    // Кнопка [=] (Пауза / Таймер мута)
                    boolean hoverToggle = UiHelper.isInside(mouseX, mouseY, toggleBtnX, btnY, btnSize, btnSize);
                    UiHelper.drawSmallButton(graphics, toggleBtnX, btnY, btnSize, btnSize, hoverToggle);
                    graphics.drawString(this.font, "=", toggleBtnX + 5, btnY + 4,
                            active ? 0xFFFFFFFF : 0xFF999999, true);
                    if (!active) {
                        UiHelper.drawRedSlash(graphics, toggleBtnX + 3, btnY + 3, btnSize - 6);
                    }

                    // Кнопка [-] (Удалить игрока)
                    boolean hoverRemove = UiHelper.isInside(mouseX, mouseY, removeBtnX, btnY, btnSize, btnSize);
                    UiHelper.drawSmallButton(graphics, removeBtnX, btnY, btnSize, btnSize, hoverRemove);
                    graphics.drawString(this.font, "-", removeBtnX + 5, btnY + 4,
                            hoverRemove ? 0xFFFF6666 : 0xFFFFFFFF, true);
                }
            }

            if (rows.size() > VISIBLE_ROWS) {
                Component scrollHint = Component.translatable(
                        "gui.joinnotifier.scroll",
                        scrollOffset + 1,
                        endIndex,
                        rows.size()
                );
                int hintW = this.font.width(scrollHint);
                graphics.drawString(this.font, scrollHint, listLeft + listWidth - hintW,
                        listTop + listHeight + 3, UiHelper.getSecondaryTextColor(), false);
            }
        }

        // Выпадающий список автодополнения ников
        List<String> suggestions = getAutocompleteSuggestions();
        if (!suggestions.isEmpty()) {
            int itemH = 16;
            int dropH = suggestions.size() * itemH + 2;
            int dropX = left + 12;
            int dropY = top + PANEL_HEIGHT - 58 - dropH;
            int dropW = PANEL_WIDTH - 24;

            graphics.fill(dropX, dropY, dropX + dropW, dropY + dropH, 0xFF1E1E1E);
            graphics.fill(dropX + 1, dropY + 1, dropX + dropW - 1, dropY + dropH - 1, 0xFF2D2D2D);

            for (int i = 0; i < suggestions.size(); i++) {
                String nick = suggestions.get(i);
                int sy = dropY + 1 + i * itemH;
                boolean hoverSug = UiHelper.isInside(mouseX, mouseY, dropX + 1, sy, dropW - 2, itemH);
                if (hoverSug) {
                    graphics.fill(dropX + 1, sy, dropX + dropW - 1, sy + itemH, 0xFF4A6B8A);
                }
                UiHelper.drawPlayerHead(graphics, nick, dropX + 4, sy + 2, 12);
                graphics.drawString(this.font, nick, dropX + 20, sy + 4, 0xFFFFFFFF, true);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double mouseX = event.x();
            double mouseY = event.y();

            int left = (this.width - PANEL_WIDTH) / 2;
            int top = (this.height - PANEL_HEIGHT) / 2;

            List<String> suggestions = getAutocompleteSuggestions();
            if (!suggestions.isEmpty()) {
                int itemH = 16;
                int dropH = suggestions.size() * itemH + 2;
                int dropX = left + 12;
                int dropY = top + PANEL_HEIGHT - 58 - dropH;
                int dropW = PANEL_WIDTH - 24;

                for (int i = 0; i < suggestions.size(); i++) {
                    int sy = dropY + 1 + i * itemH;
                    if (UiHelper.isInside(mouseX, mouseY, dropX + 1, sy, dropW - 2, itemH)) {
                        this.nameInput.setValue(suggestions.get(i));
                        return true;
                    }
                }
            }

            // Клик по шестерёнке (Общие настройки)
            if (UiHelper.isInside(mouseX, mouseY, left + 8, top + 6, 16, 16)) {
                if (this.minecraft != null) this.minecraft.setScreen(new SettingsScreen(this));
                return true;
            }

            // Клик по колокольчику ("Не беспокоить")
            if (UiHelper.isInside(mouseX, mouseY, left + 27, top + 6, 16, 16)) {
                ConfigManager.toggleGlobalMute();
                return true;
            }

            // Клик по лампочке "Ideas"
            int ideasW = 48;
            int ideasX = left + PANEL_WIDTH - ideasW - 8;
            if (UiHelper.isInside(mouseX, mouseY, ideasX, top + 6, ideasW, 16)) {
                if (this.minecraft != null) this.minecraft.setScreen(new IdeasScreen(this));
                return true;
            }

            int listLeft = left + 12;
            int listTop = top + 48;
            int listWidth = PANEL_WIDTH - 24;

            List<ListRow> rows = buildDisplayRows();
            int endIndex = Math.min(scrollOffset + VISIBLE_ROWS, rows.size());

            for (int i = scrollOffset; i < endIndex; i++) {
                int rowIndex = i - scrollOffset;
                int rowX = listLeft + 2;
                int rowY = listTop + 2 + rowIndex * ROW_HEIGHT;
                int rowW = listWidth - 4;

                ListRow row = rows.get(i);
                int btnSize = 16;
                int btnY = rowY + 3;

                if (row.isGroupHeader) {
                    int groupToggleX = rowX + rowW - btnSize - 3;
                    if (UiHelper.isInside(mouseX, mouseY, groupToggleX, btnY, btnSize, btnSize)) {
                        ConfigManager.toggleGroupEnabled(row.groupName);
                        return true;
                    }
                    if (UiHelper.isInside(mouseX, mouseY, rowX, rowY, rowW, ROW_HEIGHT - 1)) {
                        ConfigManager.toggleGroupCollapsed(row.groupName);
                        clampScroll();
                        return true;
                    }
                } else {
                    PlayerEntry entry = row.player;
                    int removeBtnX = rowX + rowW - btnSize - 3;
                    int toggleBtnX = removeBtnX - btnSize - 3;
                    int editBtnX = toggleBtnX - btnSize - 3;

                    if (UiHelper.isInside(mouseX, mouseY, editBtnX, btnY, btnSize, btnSize)) {
                        if (this.minecraft != null) {
                            this.minecraft.setScreen(new PlayerEditScreen(this, entry));
                        }
                        return true;
                    }

                    // Клик по [=]: если на паузе — сразу включаем, если активен — открываем выбор таймера мута
                    if (UiHelper.isInside(mouseX, mouseY, toggleBtnX, btnY, btnSize, btnSize)) {
                        if (!entry.isEffectivelyEnabled()) {
                            entry.clearMute();
                            ConfigManager.save();
                        } else if (this.minecraft != null) {
                            this.minecraft.setScreen(new MuteTimerScreen(this, entry));
                        }
                        return true;
                    }

                    if (UiHelper.isInside(mouseX, mouseY, removeBtnX, btnY, btnSize, btnSize)) {
                        ConfigManager.removePlayer(entry.name);
                        clampScroll();
                        return true;
                    }

                    if (UiHelper.isInside(mouseX, mouseY, rowX, rowY, rowW, ROW_HEIGHT - 1)) {
                        this.nameInput.setValue(entry.name);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        List<ListRow> rows = buildDisplayRows();
        if (rows.size() > VISIBLE_ROWS) {
            if (scrollY < 0) scrollOffset++;
            else if (scrollY > 0) scrollOffset--;
            clampScroll(rows.size());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void clampScroll() {
        clampScroll(buildDisplayRows().size());
    }

    private void clampScroll(int totalRows) {
        int maxScroll = Math.max(0, totalRows - VISIBLE_ROWS);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        if (scrollOffset < 0) scrollOffset = 0;
    }
}
