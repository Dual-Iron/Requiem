/*
 * Requiem
 * Copyright (C) 2017-2020 Ladysnake
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses>.
 *
 * Linking this mod statically or dynamically with other
 * modules is making a combined work based on this mod.
 * Thus, the terms and conditions of the GNU General Public License cover the whole combination.
 *
 * In addition, as a special exception, the copyright holders of
 * this mod give you permission to combine this mod
 * with free software programs or libraries that are released under the GNU LGPL
 * and with code included in the standard release of Minecraft under All Rights Reserved (or
 * modified versions of such code, with unchanged license).
 * You may copy and distribute such a system following the terms of the GNU GPL for this mod
 * and the licenses of the other code concerned.
 *
 * Note that people who make modified versions of this mod are not obligated to grant
 * this special exception for their modified versions; it is their choice whether to do so.
 * The GNU General Public License gives permission to release a modified version without this exception;
 * this exception also makes it possible to release a modified version which carries forward this exception.
 */
package ladysnake.requiem.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import ladysnake.requiem.Requiem;
import ladysnake.requiem.api.v1.remnant.RemnantType;
import ladysnake.requiem.common.RequiemRegistries;
import ladysnake.requiem.common.item.OpusDemoniumItem;
import ladysnake.requiem.common.network.RequiemNetworking;
import ladysnake.requiem.mixin.client.gui.ingame.EditBookScreenAccessor;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class EditOpusScreen extends BookEditScreen {
    public static final Identifier BOOK_TEXTURE = Requiem.id("textures/gui/opus_daemonium.png");
    public static final Identifier XP_COST_TEXTURE = Requiem.id("textures/gui/required_xp_5.png");
    public static final int REQUIRED_XP = OpusDemoniumItem.REQUIRED_CONVERSION_XP;

    private Map<String, RemnantType> incantations;
    private boolean validSentence;

    public EditOpusScreen(PlayerEntity player, ItemStack book, Hand hand) {
        super(player, book, hand);
        this.incantations = new HashMap<>();
        for (RemnantType type : RequiemRegistries.REMNANT_STATES) {
            String incantation = type.getConversionBookSentence();
            if (incantation != null) {
                // User locale can theoretically cause incompatibilities, but english locale does not support accents
                this.incantations.put(I18n.translate(incantation).toLowerCase(Locale.getDefault()), type);
            }
        }
    }

    protected <T extends AbstractButtonWidget> void removeButton(T button) {
        this.buttons.remove(button);
        this.children.remove(button);
    }

    @Override
    protected void init() {
        super.init();
        EditBookScreenAccessor access = ((EditBookScreenAccessor)this);
        this.removeButton(access.getNextPageButton());
        this.removeButton(access.getPreviousPageButton());
        this.removeButton(access.getSignButton());
        this.removeButton(access.getFinalizeButton());
        this.removeButton(access.getCancelButton());
        access.setSignButton(this.addButton(new ButtonWidget(
                this.width / 2 - 100, 196, 98, 20,
                I18n.translate("book.signButton"),
                (widget) -> this.finalizeOpus(true)))
        );
        this.removeButton(access.getDoneButton());
        access.setDoneButton(this.addButton(new ButtonWidget(
                this.width / 2 + 2, 196, 98, 20,
                I18n.translate("gui.done"),
                (widget) -> this.finalizeOpus(false)))
        );
        checkMagicSentence();
    }

    private void finalizeOpus(boolean sign) {
        Objects.requireNonNull(this.minecraft).openScreen(null);
        if (((EditBookScreenAccessor)this).isDirty() || sign) {
            String firstPage = getFirstPage();   // it's the only one we accept
            Hand hand = ((EditBookScreenAccessor) this).getHand();
            RequiemNetworking.sendToServer(RequiemNetworking.createOpusUpdateBuffer(firstPage, sign, this.incantations.get(firstPage.toLowerCase(Locale.getDefault())), hand));
        }
    }

    public void render(int mouseX, int mouseY, float tickDelta) {
        assert this.minecraft != null;
        assert this.minecraft.player != null;
        this.renderBackground();
        this.setFocused(null);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.minecraft.getTextureManager().bindTexture(BOOK_TEXTURE);
        int bgX = (this.width - 192) / 2;
        this.blit(bgX, 2, 0, 0, 192, 192);
        String page = this.getFirstPage();
        this.font.drawTrimmed(page, bgX + 36, 32, 114, 0);
        ((EditBookScreenAccessor)this).invokeDrawHighlight(page);
        if (((EditBookScreenAccessor)this).getTickCounter() / 6 % 2 == 0) {
            Point2D cursorPosition = this.getCursorPositionForIndex(page, ((EditBookScreenAccessor)this).getCursorIndex());
            int x = cursorPosition.x;
            int y = cursorPosition.y;
            if (this.font.isRightToLeft()) {
                x = 110 - x;
            }

            x += (this.width - 192) / 2 + 36;
            y += 32;
            if (((EditBookScreenAccessor) this).getCursorIndex() < page.length()) {
                DrawableHelper.fill(x, y - 1, x + 1, y + 9, 0xFF000000);
            } else {
                this.font.draw("_", x, y, 0xFF000000);
            }
        }
        for (AbstractButtonWidget button : this.buttons) {
            button.render(mouseX, mouseY, tickDelta);
        }
        ButtonWidget signButton = ((EditBookScreenAccessor) this).getSignButton();
        int x = signButton.x + signButton.getWidth() - 22;
        int y = signButton.y - 13;
        this.minecraft.getTextureManager().bindTexture(XP_COST_TEXTURE);
        if (this.validSentence) {
            if (this.minecraft.player.experienceLevel < REQUIRED_XP && !this.minecraft.player.abilities.creativeMode) {
                blit(x + 1, y + 15, 0, 16, 16, 16, 32, 32);
            } else {
                blit(x + 1, y + 15, 0, 0, 16, 16, 32, 32);
            }
        }
    }

    private Point2D getCursorPositionForIndex(String content, int cursorIndex) {
        int x = 0;
        int y = 0;
        int lineLength = 0;
        int int_3 = 0;

        for(String str = content; !str.isEmpty(); int_3 = lineLength) {
            int charCount = this.font.getCharacterCountForWidth(str, 114);
            if (str.length() <= charCount) {
                String lastLine = str.substring(0, Math.min(Math.max(cursorIndex - int_3, 0), str.length()));
                x = x + this.font.getStringWidth(lastLine);
                break;
            }

            String line = str.substring(0, charCount);
            char lastChar = str.charAt(charCount);
            boolean empty = lastChar == ' ' || lastChar == '\n';
            str = Formatting.getFormatAtEnd(line) + str.substring(charCount + (empty ? 1 : 0));
            lineLength += line.length() + (empty ? 1 : 0);
            if (lineLength - 1 >= cursorIndex) {
                String substr = line.substring(0, Math.min(Math.max(cursorIndex - int_3, 0), line.length()));
                x += this.font.getStringWidth(substr);
                break;
            }

            y += 9;
        }

        return new Point2D(x, y);
    }

    private String getFirstPage() {
        return ((EditBookScreenAccessor) this).getPages().get(0);
    }

    @Override
    public boolean keyPressed(int int_1, int int_2, int int_3) {
        if (super.keyPressed(int_1, int_2, int_3)) {
            checkMagicSentence();
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char char_1, int int_1) {
        if (super.charTyped(char_1, int_1)) {
            checkMagicSentence();
            return true;
        }
        return false;
    }

    private void checkMagicSentence() {
        assert minecraft != null;
        assert minecraft.player != null;
        // Strings are lowercase in the map to make the check case insensitive
        this.validSentence = this.incantations.containsKey(this.getFirstPage().toLowerCase(Locale.getDefault()));
        ((EditBookScreenAccessor) this).getSignButton().active = this.validSentence && (this.minecraft.player.experienceLevel >= REQUIRED_XP || this.minecraft.player.isCreative());
    }
}
