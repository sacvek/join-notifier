package com.example.joinnotifier;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class JoinNotifierScreen extends Screen {
    private static final int PANEL_WIDTH = 290;
    private static final int PANEL_HEIGHT = 250;
    private static final int ROW_HEIGHT = 23;
    private static final int VISIBLE_ROWS = 5;

    private EditBox nameInput;
    private Button modeButton;
    private int scrollOffset = 0;

    public JoinNotifierScreen() {
        super(Component.translatable("gui.joinnotifier.title"));
    }

    @Override
    protected void init() {
        super.init();

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        // Переключатель режима уведомлений: Чат / Плашка / Чат + Плашка
        this.modeButton = Button.builder(getModeLabel(), button -> {
            ConfigManager.cycleNotifyMode();
            button.setMessage(getModeLabel());
        }).bounds(left + 12, top + 26, 134, 18).build();
        this.addRenderableWidget(this.modeButton);

        // Поле ввода ника снизу
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

    private Component getModeLabel() {
        return switch (ConfigManager.getNotifyMode()) {
            case CHAT -> Component.translatable("gui.joinnotifier.mode.chat");
            case TOAST -> Component.translatable("gui.joinnotifier.mode.toast");
            case BOTH -> Component.translatable("gui.joinnotifier.mode.both");
        };
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

        // 1. Кнопка админ-панели (замок слева сверху)
        int adminX = left + 8;
        int adminY = top + 6;
        boolean adminHover = UiHelper.isInside(mouseX, mouseY, adminX, adminY, 16, 16);
        UiHelper.drawSmallButton(graphics, adminX, adminY, 16, 16, adminHover);
        UiHelper.drawLockIcon(graphics, adminX + 4, adminY + 3);

        // 2. Кнопка "Не беспокоить" (колокольчик рядом с замком)
        int dndX = left + 27;
        int dndY = top + 6;
        boolean dndHover = UiHelper.isInside(mouseX, mouseY, dndX, dndY, 16, 16);
        UiHelper.drawSmallButton(graphics, dndX, dndY, 16, 16, dndHover);
        UiHelper.drawBellIcon(graphics, dndX + 4, dndY + 3, ConfigManager.isGlobalMute());
        if (ConfigManager.isGlobalMute()) {
            UiHelper.drawRedSlash(graphics, dndX + 3, dndY + 3, 10);
        }

        // 3. Кнопка Ideas с лампочкой (справа сверху)
        int ideasW = 48;
        int ideasX = left + PANEL_WIDTH - ideasW - 8;
        int ideasY = top + 6;
        boolean ideasHover = UiHelper.isInside(mouseX, mouseY, ideasX, ideasY, ideasW, 16);
        UiHelper.drawSmallButton(graphics, ideasX, ideasY, ideasW, 16, ideasHover);
        UiHelper.drawLightbulbIcon(graphics, ideasX + 4, ideasY + 3);
        graphics.drawString(this.font, "Ideas", ideasX + 15, ideasY + 4, 0xFFFFFFFF, true);

        // Заголовок по центру
        int titleWidth = this.font.width(this.title);
        graphics.drawString(
                this.font,
                this.title,
                left + (PANEL_WIDTH - titleWidth) / 2 + 4,
                top + 10,
                0xFF404040,
                false
        );

        // Статус "Не беспокоить" справа от кнопки режима
        // Статус "Не беспокоить" справа от кнопки режима
        if (ConfigManager.isGlobalMute()) {
            graphics.drawString(
                    this.font,
                    Component.translatable("gui.joinnotifier.dnd_on"),
                    left + 152,
                    top + 31,
                    0xFFAA2222,
                    false
            );
        }

        // Внутренняя рамка списка игроков
        int listLeft = left + 12;
        int listTop = top + 48;
        int listWidth = PANEL_WIDTH - 24;
        int listHeight = VISIBLE_ROWS * ROW_HEIGHT + 4;
        UiHelper.drawInsetBox(graphics, listLeft, listTop, listWidth, listHeight);

        List<PlayerEntry> players = ConfigManager.getPlayers();
        clampScroll();

        if (players.isEmpty()) {
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
            int endIndex = Math.min(scrollOffset + VISIBLE_ROWS, players.size());
            for (int i = scrollOffset; i < endIndex; i++) {
                int rowIndex = i - scrollOffset;
                int rowX = listLeft + 2;
                int rowY = listTop + 2 + rowIndex * ROW_HEIGHT;
                int rowW = listWidth - 4;

                PlayerEntry entry = players.get(i);

                boolean rowHovered = UiHelper.isInside(mouseX, mouseY, rowX, rowY, rowW, ROW_HEIGHT - 1);
                graphics.fill(rowX, rowY, rowX + rowW, rowY + ROW_HEIGHT - 1,
                        rowHovered ? 0xFF7A7A7A : 0xFF6B6B6B);

                // Голова скина слева
                UiHelper.drawPlayerHead(graphics, entry.name, rowX + 4, rowY + 5, 12);

                int btnSize = 16;
                int btnY = rowY + 3;
                int removeBtnX = rowX + rowW - btnSize - 3;
                int toggleBtnX = removeBtnX - btnSize - 3;
                int editBtnX = toggleBtnX - btnSize - 3;

                // Формирование строки: [Тег] Ник
                String prefix = (entry.tag != null && !entry.tag.isEmpty()) ? "[" + entry.tag + "] " : "";
                String displayName = prefix + entry.name;
                int textColor = entry.enabled ? entry.getRgbColor() : 0xFFAAAAAA;

                int maxNameW = editBtnX - (rowX + 22);
                String trimmedName = this.font.plainSubstrByWidth(displayName, Math.max(20, maxNameW));
                graphics.drawString(this.font, trimmedName, rowX + 20, rowY + 7, textColor, true);

                // Если задан фильтр по серверу или выход — показываем маленькую метку перед кнопками
                int nameEndW = this.font.width(trimmedName);
                String badges = "";
                if (entry.notifyLeave) badges += "⇄";
                if (entry.serverIp != null && !entry.serverIp.isEmpty()) badges += " [IP]";
                if (!badges.isEmpty() && rowX + 22 + nameEndW + this.font.width(badges) < editBtnX) {
                    graphics.drawString(this.font, badges, rowX + 22 + nameEndW, rowY + 7, 0xFFCCCCCC, false);
                }

                // Кнопка [⚙] (Настройки игрока: выход, тег, цвет, IP сервера)
                boolean hoverEdit = UiHelper.isInside(mouseX, mouseY, editBtnX, btnY, btnSize, btnSize);
                UiHelper.drawSmallButton(graphics, editBtnX, btnY, btnSize, btnSize, hoverEdit);
                graphics.drawString(this.font, "⚙", editBtnX + 4, btnY + 4,
                        hoverEdit ? 0xFFFFD83D : 0xFFFFFFFF, true);

                // Кнопка [=] (Вкл/выкл уведомлений)
                boolean hoverToggle = UiHelper.isInside(mouseX, mouseY, toggleBtnX, btnY, btnSize, btnSize);
                UiHelper.drawSmallButton(graphics, toggleBtnX, btnY, btnSize, btnSize, hoverToggle);
                graphics.drawString(this.font, "=", toggleBtnX + 5, btnY + 4,
                        entry.enabled ? 0xFFFFFFFF : 0xFF999999, true);
                if (!entry.enabled) {
                    UiHelper.drawRedSlash(graphics, toggleBtnX + 3, btnY + 3, btnSize - 6);
                }

                // Кнопка [-] (Удалить игрока)
                boolean hoverRemove = UiHelper.isInside(mouseX, mouseY, removeBtnX, btnY, btnSize, btnSize);
                UiHelper.drawSmallButton(graphics, removeBtnX, btnY, btnSize, btnSize, hoverRemove);
                graphics.drawString(this.font, "-", removeBtnX + 5, btnY + 4,
                        hoverRemove ? 0xFFFF6666 : 0xFFFFFFFF, true);
            }

            if (players.size() > VISIBLE_ROWS) {
                Component scrollHint = Component.translatable(
                        "gui.joinnotifier.scroll",
                        scrollOffset + 1,
                        endIndex,
                        players.size()
                );
                int hintW = this.font.width(scrollHint);
                graphics.drawString(this.font, scrollHint, listLeft + listWidth - hintW,
                        listTop + listHeight + 3, 0xFF555555, false);
            }
        }

        // Выпадающий список автодополнения ников с текущего сервера
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

            // Клик по подсказке автодополнения (проверяем первым, так как она поверх списка)
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

            // Клик по замку (Админ-панель)
            if (UiHelper.isInside(mouseX, mouseY, left + 8, top + 6, 16, 16)) {
                if (this.minecraft != null) this.minecraft.setScreen(new AdminLoginScreen(this));
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

            // Клик по строкам списка игроков
            int listLeft = left + 12;
            int listTop = top + 48;
            int listWidth = PANEL_WIDTH - 24;

            List<PlayerEntry> players = ConfigManager.getPlayers();
            int endIndex = Math.min(scrollOffset + VISIBLE_ROWS, players.size());

            for (int i = scrollOffset; i < endIndex; i++) {
                int rowIndex = i - scrollOffset;
                int rowX = listLeft + 2;
                int rowY = listTop + 2 + rowIndex * ROW_HEIGHT;
                int rowW = listWidth - 4;

                int btnSize = 16;
                int btnY = rowY + 3;
                int removeBtnX = rowX + rowW - btnSize - 3;
                int toggleBtnX = removeBtnX - btnSize - 3;
                int editBtnX = toggleBtnX - btnSize - 3;

                PlayerEntry entry = players.get(i);

                // Нажатие на [⚙] (открывает настройки игрока)
                if (UiHelper.isInside(mouseX, mouseY, editBtnX, btnY, btnSize, btnSize)) {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new PlayerEditScreen(this, entry));
                    }
                    return true;
                }

                // Нажатие на [=]
                if (UiHelper.isInside(mouseX, mouseY, toggleBtnX, btnY, btnSize, btnSize)) {
                    ConfigManager.togglePlayer(entry);
                    return true;
                }

                // Нажатие на [-]
                if (UiHelper.isInside(mouseX, mouseY, removeBtnX, btnY, btnSize, btnSize)) {
                    ConfigManager.removePlayer(entry.name);
                    clampScroll();
                    return true;
                }

                // Нажатие на строку подставляет ник в поле снизу
                if (UiHelper.isInside(mouseX, mouseY, rowX, rowY, rowW, ROW_HEIGHT - 1)) {
                    this.nameInput.setValue(entry.name);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        List<PlayerEntry> players = ConfigManager.getPlayers();
        if (players.size() > VISIBLE_ROWS) {
            if (scrollY < 0) scrollOffset++;
            else if (scrollY > 0) scrollOffset--;
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void clampScroll() {
        int maxScroll = Math.max(0, ConfigManager.getPlayers().size() - VISIBLE_ROWS);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        if (scrollOffset < 0) scrollOffset = 0;
    }
}