package me.alextzamalis.mygame.data;



import me.alextzamalis.engine.assets.AssetManager;

import me.alextzamalis.engine.graphics.NineSliceInsets;

import me.alextzamalis.engine.graphics.text.Font;

import me.alextzamalis.engine.scene.Sprite;

import me.alextzamalis.engine.ui.UICycleButton;

import me.alextzamalis.engine.ui.UIScaleMode;

import me.alextzamalis.engine.ui.UISpriteElement;

import me.alextzamalis.engine.ui.UITexturedButton;

import me.alextzamalis.engine.ui.UIToggle;



/**

 * Shared Sprout Lands UI assets for game screens.

 */

public final class GameUiAssets {

    /** Global UI scale for menus, inventory, and HUD. */
    public static final float UI_SCALE = 1.5f;

    public static final String UI_FONT = "/ui/fonts/pixelFont-7-8x14-sproutLands.ttf";

    private static final int MENU_BTN_W = Math.round(96 * UI_SCALE);
    private static final int MENU_BTN_H = Math.round(32 * UI_SCALE);

    public static float s(float value) {
        return value * UI_SCALE;
    }

    public static Font loadScaledUiFont(float baseSize) {
        return loadUiFont(s(baseSize));
    }



    private GameUiAssets() {

    }



    public static void load() {

        UiAtlasCatalog.load();

        WeaponIconCatalog.load();

    }



    public static Font loadUiFont(float size) {

        return AssetManager.loadFontResource(UI_FONT, size);

    }



    public static Sprite slotSprite() {

        load();

        return UiAtlasCatalog.getSprite("outline1x1");

    }



    public static Sprite gridOutline(int cellWidth, int cellHeight) {

        load();

        return UiAtlasCatalog.getGridOutline(cellWidth, cellHeight);

    }



    public static void applyPanelNineSlice(UISpriteElement panel, String regionId) {

        load();

        UiAtlasCatalog.AtlasRegion region = UiAtlasCatalog.getRegion(regionId);

        if (region == null) {

            return;

        }

        panel.setSprite(UiAtlasCatalog.getSprite(regionId));

        panel.setScaleMode(UIScaleMode.NINE_SLICE);

        panel.setNineSliceInsets(region.toNineSliceInsets());

        panel.setRegionPixelSize(region.w, region.h);

    }



    /** Wide menu button using play-button blank tiles and nine-slice. */

    public static void styleMenuButton(UITexturedButton button, boolean usePlayArt) {

        load();

        String normalId = usePlayArt ? "menuButtonPlayNormal" : "menuButtonBlankNormal";

        String hoverId = usePlayArt ? "menuButtonPlayHover" : "menuButtonBlankHover";

        NineSliceInsets insets = UiAtlasCatalog.getNineSliceInsets(normalId);

        button.setSprites(

                UiAtlasCatalog.getSprite(normalId),

                UiAtlasCatalog.getSprite(hoverId),

                UiAtlasCatalog.getSprite(hoverId));

        button.setScaleMode(UIScaleMode.NINE_SLICE);

        button.setNineSliceInsets(insets);

        button.setRegionPixelSize(MENU_BTN_W, MENU_BTN_H);

    }



    public static void styleCycleRow(UICycleButton row) {

        load();

        UiAtlasCatalog.AtlasRegion region = UiAtlasCatalog.getRegion("menuButtonBlankNormal");

        row.setSprites(

                UiAtlasCatalog.getSprite("menuButtonBlankNormal"),

                UiAtlasCatalog.getSprite("menuButtonBlankHover"),

                UiAtlasCatalog.getSprite("menuButtonBlankHover"));

        row.setCycleSprite(UiAtlasCatalog.getSprite("cycleButton"));

        if (region != null) {

            row.setNineSliceInsets(region.toNineSliceInsets());

            row.setRegionPixelSize(region.w, region.h);

        }

    }



    public static void styleInventoryActionButton(UITexturedButton button, boolean blue) {

        load();

        if (blue) {

            button.setSprites(

                    UiAtlasCatalog.getSprite("buttonBlueNormal"),

                    UiAtlasCatalog.getSprite("buttonBlueNormal"),

                    UiAtlasCatalog.getSprite("buttonBluePressed"));

        } else {

            button.setSprites(

                    UiAtlasCatalog.getSprite("buttonRedNormal"),

                    UiAtlasCatalog.getSprite("buttonRedNormal"),

                    UiAtlasCatalog.getSprite("buttonRedPressed"));

        }

        styleMenuButton(button, false);

    }



    public static void styleBrownButton(UITexturedButton button) {

        load();

        button.setSprites(

                UiAtlasCatalog.getSprite("buttonBrownNormal"),

                UiAtlasCatalog.getSprite("buttonBrownHover"),

                UiAtlasCatalog.getSprite("buttonBrownPressed"));

        button.setScaleMode(UIScaleMode.STRETCH);

    }



    public static void styleGreenButton(UITexturedButton button) {

        styleMenuButton(button, true);

    }



    public static void styleRedButton(UITexturedButton button) {

        styleMenuButton(button, false);

    }



    public static void styleBlueButton(UITexturedButton button) {

        styleMenuButton(button, false);

    }



    public static void styleSettingsToggle(UIToggle toggle) {

        load();

        toggle.setSprites(

                UiAtlasCatalog.getSprite("toggleOff"),

                UiAtlasCatalog.getSprite("toggleOn"));

        toggle.setRegionPixelSize(Math.round(52 * UI_SCALE), Math.round(22 * UI_SCALE));

    }



    public static Sprite dialogBigSprite() {

        load();

        return UiAtlasCatalog.getSprite("dialogBigPanel");

    }



    public static Sprite dialogMediumSprite() {

        load();

        return UiAtlasCatalog.getSprite("dialogMediumPanel");

    }

}

