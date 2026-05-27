package me.alextzamalis.mygame;

import me.alextzamalis.engine.Window;
import me.alextzamalis.engine.assets.AssetManager;
import me.alextzamalis.engine.core.Input;
import me.alextzamalis.engine.core.Logger;
import me.alextzamalis.engine.graphics.BatchRenderer;
import me.alextzamalis.engine.graphics.Camera2D;
import me.alextzamalis.engine.graphics.text.Font;
import me.alextzamalis.engine.graphics.text.TextRenderer;
import me.alextzamalis.engine.screen.GameScreen;
import me.alextzamalis.engine.ui.UIDragDropController;
import me.alextzamalis.engine.ui.UIGridInventoryPanel;
import me.alextzamalis.engine.ui.UIManager;
import me.alextzamalis.engine.ui.UITexturedButton;
import me.alextzamalis.mygame.data.GameAudio;
import me.alextzamalis.mygame.data.GameUiAssets;
import me.alextzamalis.mygame.data.WeaponIconCatalog;

import org.joml.Vector4f;

public class InventoryPopupScreen extends GameScreen {

    private static final Vector4f WHITE = new Vector4f(1f, 1f, 1f, 1f);

    /**
     * Layout stacks top to bottom: header hints, grid, offer row, action buttons.
     */
    private static final class Layout {
        static final float GRID_PX = RunState.GRID_SIZE * RunState.CELL_SIZE;
        static final float PANEL_PAD = GameUiAssets.s(28f);
        static final float PANEL_W = GRID_PX + PANEL_PAD * 2f;
        static final float PANEL_H = GameUiAssets.s(620f);
        static final float PANEL_X = -PANEL_W / 2f;
        static final float PANEL_Y = -PANEL_H / 2f;

        static final float GRID_X = -GRID_PX / 2f;

        static final float CLOSE_Y = PANEL_Y + 14f;
        static final float BUY_Y = PANEL_Y + 62f;

        static final float OFFER_Y = PANEL_Y + 118f;
        static final float OFFER_GAP = GameUiAssets.s(14f);
        static final float OFFER_CELL = RunState.CELL_SIZE;
        static final float OFFER_LABEL_Y = OFFER_Y + OFFER_CELL + 16f;

        static final float GRID_Y = OFFER_LABEL_Y + 22f;
        static final float GRID_TOP = GRID_Y + GRID_PX;

        static final float HINT_LINE2_Y = GRID_TOP + 16f;
        static final float HINT_LINE1_Y = HINT_LINE2_Y + 14f;
        static final float BUCKS_Y = HINT_LINE1_Y + 22f;
        static final float TITLE_Y = BUCKS_Y + 28f;
    }

    private final RunState runState;
    private final Runnable onClose;

    private Window window;
    private BatchRenderer batch;
    private Camera2D camera;
    private Font titleFont;
    private Font labelFont;
    private Font bodyFont;
    private UIManager uiManager;
    private UIGridInventoryPanel gridPanel;
    private UIDragDropController dragController;

    public InventoryPopupScreen(RunState runState, Runnable onClose) {
        this.runState = runState;
        this.onClose = onClose;
    }

    @Override
    public void init(Window window) {
        this.window = window;
        GameUiAssets.load();

        batch = new BatchRenderer(AssetManager.getOrLoadDefaultShader());
        camera = new Camera2D(window.getWidth(), window.getHeight());
        titleFont = GameUiAssets.loadScaledUiFont(22f);
        labelFont = GameUiAssets.loadScaledUiFont(11f);
        bodyFont = GameUiAssets.loadScaledUiFont(14f);
        uiManager = new UIManager();

        gridPanel = new UIGridInventoryPanel(
                Layout.GRID_X, Layout.GRID_Y, runState.getInventory(), RunState.CELL_SIZE);
        gridPanel.setIconProvider(item -> {
            if (!item.isWeapon()) {
                return null;
            }
            return WeaponIconCatalog.getIcon(item.getBaseId(), item.getTier());
        });

        float offerRowW = UIDragDropController.OFFER_SLOT_COUNT * Layout.OFFER_CELL
                + (UIDragDropController.OFFER_SLOT_COUNT - 1) * Layout.OFFER_GAP;
        float offerStartX = -offerRowW / 2f;

        dragController = new UIDragDropController(
                runState.getInventory(), gridPanel,
                offerStartX, Layout.OFFER_Y, Layout.OFFER_GAP, Layout.OFFER_CELL,
                runState::tryMergeItems);
        dragController.setIconProvider(item -> {
            if (!item.isWeapon()) {
                return null;
            }
            return WeaponIconCatalog.getIcon(item.getBaseId(), item.getTier());
        });

        UITexturedButton buyBtn = new UITexturedButton(
                -GameUiAssets.s(110f), Layout.BUY_Y, GameUiAssets.s(220f), GameUiAssets.s(44f), "BUY (30)");
        GameUiAssets.styleInventoryActionButton(buyBtn, true);
        buyBtn.setOnClick(() -> {
            GameAudio.playRandomUiClick();
            buyRandomOffer();
        });
        uiManager.addElement(buyBtn);

        UITexturedButton closeBtn = new UITexturedButton(
                -GameUiAssets.s(80f), Layout.CLOSE_Y, GameUiAssets.s(160f), GameUiAssets.s(40f), "CLOSE");
        GameUiAssets.styleInventoryActionButton(closeBtn, false);
        closeBtn.setOnClick(() -> {
            GameAudio.playRandomUiClick();
            closePopup();
        });
        uiManager.addElement(closeBtn);

        Logger.info("Inventory", "Inventory popup opened (7x7 grid).");
    }

    private void buyRandomOffer() {
        if (!runState.spendBucks(RunState.BUY_COST)) {
            Logger.info("Inventory", "Not enough bucks for purchase.");
            return;
        }
        dragController.clearOfferSlots();
        runState.fillOfferSlots(dragController.getOfferSlots());
    }

    private void closePopup() {
        runState.markFirstInventoryShown();
        if (runState.isSetupPhase()) {
            runState.endSetupPhase();
        }
        if (onClose != null) {
            onClose.run();
        }
        screenManager.popScreen();
    }

    @Override
    public void update(float dt) {
        if (Input.isKeyJustPressed(Input.KEY_E) || Input.isKeyJustPressed(Input.KEY_ESCAPE)) {
            closePopup();
            return;
        }

        float uiMouseX = (float) Input.getMouseX() - window.getWidth() / 2f;
        float uiMouseY = (window.getHeight() / 2f) - (float) Input.getMouseY();
        dragController.setMousePosition(uiMouseX, uiMouseY);

        dragController.update();
        uiManager.update(dt, window.getWidth(), window.getHeight());
    }

    @Override
    public void render() {
        int w = window.getWidth();
        int h = window.getHeight();
        camera.adjustProjection(w, h);

        float halfW = w / 2f;
        float halfH = h / 2f;

        batch.setProjection(camera.getProjectionViewMatrix());
        batch.beginBatch();

        batch.drawQuad(-halfW, -halfH, w, h, new Vector4f(0f, 0f, 0f, 0.65f));

        drawPanelBackground(batch);

        TextRenderer.drawTextFullyCentered(batch, titleFont, "INVENTORY",
                0f, Layout.TITLE_Y, new Vector4f(1f, 0.85f, 0.3f, 1f));

        TextRenderer.drawTextCentered(batch, bodyFont, "Bucks: " + runState.getBucks(),
                0f, Layout.BUCKS_Y, new Vector4f(0.3f, 1f, 0.5f, 0.9f));

        TextRenderer.drawTextCentered(batch, labelFont,
                "BUY for random loot. Place weapons on unlocked tiles.",
                0f, Layout.HINT_LINE1_Y, WHITE);
        TextRenderer.drawTextCentered(batch, labelFont,
                "Drop expansion tiles on locked cells. Merge in grid or offers.",
                0f, Layout.HINT_LINE2_Y, WHITE);

        gridPanel.render(batch, labelFont);

        TextRenderer.drawTextCentered(batch, bodyFont, "Offer slots",
                0f, Layout.OFFER_LABEL_Y, new Vector4f(0.85f, 0.85f, 0.85f, 0.9f));

        dragController.renderOfferSlots(batch, labelFont);
        dragController.renderDragging(batch, labelFont);

        uiManager.render(batch, bodyFont);

        batch.endBatch();
        batch.flush();
    }

    private void drawPanelBackground(BatchRenderer batch) {
        Vector4f outer = new Vector4f(0.1f, 0.09f, 0.12f, 0.96f);
        Vector4f inner = new Vector4f(0.16f, 0.14f, 0.18f, 0.9f);
        batch.drawQuad(Layout.PANEL_X, Layout.PANEL_Y, Layout.PANEL_W, Layout.PANEL_H, outer);
        batch.drawQuad(Layout.PANEL_X + 6f, Layout.PANEL_Y + 6f,
                Layout.PANEL_W - 12f, Layout.PANEL_H - 12f, inner);
    }

    @Override
    public void dispose() {
        if (batch != null) {
            batch.dispose();
        }
    }

    @Override
    public boolean isTransparent() {
        return true;
    }
}
