package me.alextzamalis.mygame;

import me.alextzamalis.engine.Window;
import me.alextzamalis.engine.assets.AssetManager;
import me.alextzamalis.engine.collision.AABB;
import me.alextzamalis.engine.core.Input;
import me.alextzamalis.engine.core.Logger;
import me.alextzamalis.engine.core.Timer;
import me.alextzamalis.engine.graphics.BatchRenderer;
import me.alextzamalis.engine.graphics.Camera2D;
import me.alextzamalis.engine.graphics.OneShotAnimation;
import me.alextzamalis.engine.graphics.SpriteSheet;
import me.alextzamalis.engine.graphics.Texture;
import me.alextzamalis.engine.graphics.ViewportScaler;
import me.alextzamalis.engine.graphics.text.Font;
import me.alextzamalis.engine.graphics.text.TextRenderer;
import me.alextzamalis.engine.scene.GameObject;
import me.alextzamalis.engine.scene.Scene;
import me.alextzamalis.engine.scene.Sprite;
import me.alextzamalis.engine.scene.Transform;
import me.alextzamalis.engine.screen.GameScreen;
import me.alextzamalis.mygame.data.BackgroundCatalog;
import me.alextzamalis.mygame.data.EnemyCatalog;
import me.alextzamalis.mygame.data.EnemyDefinition;
import me.alextzamalis.mygame.data.GameAudio;
import me.alextzamalis.mygame.data.GameUiAssets;
import me.alextzamalis.mygame.data.WaveCatalog;
import me.alextzamalis.mygame.data.WaveStageDefinition;
import me.alextzamalis.mygame.data.ThemedMusicCatalog;
import me.alextzamalis.mygame.editor.OpdEditorDebugProvider;

import org.joml.Vector2f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class OPDPlayScreen extends GameScreen {

    private static final float PLAY_AREA_WIDTH = 800f;
    private static final float PLAY_AREA_HEIGHT = 600f;
    private static final float PLAYER_WIDTH = 40f;
    private static final float PLAYER_HEIGHT = 40f;
    private static final int BASE_HP = 100;
    private static final float WAVE_PAUSE_DURATION = 1.0f;

    private Window window;
    private BatchRenderer batch;
    private Camera2D camera;
    private Camera2D hudCamera;
    private Scene scene;
    private Font hudFont;
    private SpriteSheet spriteSheet;

    private final RunState runState = new RunState();
    private GameObject player;
    private final Vector2f playerAnchor = new Vector2f();
    private int currentHp;
    private int maxHp;
    private float currentShield;
    private int maxShield;
    private int score;
    private int waveNumber;

    private RunState.CombatLoadout combatLoadout = new RunState.CombatLoadout();
    private final List<RunState.EquippedWeapon> equippedProjectileWeapons = new ArrayList<>();
    private final List<RunState.EquippedBeam> equippedBeams = new ArrayList<>();
    private float bootsHealElapsed;
    private float bootsHealCooldownElapsed;
    private final List<ProjectileData> projectiles = new ArrayList<>();
    private final List<EnemyData> enemies = new ArrayList<>();
    private final List<OneShotAnimation> hitEffects = new ArrayList<>();

    private int enemiesToSpawn;
    private int spawnBatchSize;
    private float spawnTimer;
    private float spawnInterval;
    private boolean waveActive;
    private Timer wavePauseTimer;
    private WaveStageDefinition currentStage;
    private boolean inventoryOpen;
    private final ThemeTransitionController themeTransition = new ThemeTransitionController();
    private final ViewportScaler viewportScaler = new ViewportScaler(
            (int) PLAY_AREA_WIDTH, (int) PLAY_AREA_HEIGHT);
    private OpdEditorDebugProvider debugProvider;

    private final Random rng = new Random();

    @Override
    public void init(Window window) {
        this.window = window;

        EnemyCatalog.load();
        WaveCatalog.load();
        BackgroundCatalog.load();
        ThemedMusicCatalog.load();
        GameUiAssets.load();

        batch = new BatchRenderer(AssetManager.getOrLoadDefaultShader());
        camera = new Camera2D((int) PLAY_AREA_WIDTH, (int) PLAY_AREA_HEIGHT);
        hudCamera = new Camera2D(window.getWidth(), window.getHeight());
        viewportScaler.updateWindowSize(window.getWidth(), window.getHeight());
        scene = new Scene();

        hudFont = GameUiAssets.loadScaledUiFont(14f);

        Texture atlasTexture = AssetManager.tryLoadTextureResource("/atlas/img.png");
        if (atlasTexture != null) {
            spriteSheet = new SpriteSheet(atlasTexture, 32, 32);
        } else {
            Logger.warn("OPD", "Character atlas /atlas/img.png missing; using color sprites.");
        }

        score = 0;
        waveNumber = 0;

        player = new GameObject("player",
                new Transform(
                        new Vector2f(-PLAYER_WIDTH / 2f, -PLAY_AREA_HEIGHT / 2f + 20f),
                        new Vector2f(PLAYER_WIDTH, PLAYER_HEIGHT)));
        player.addSprite(getPlayerSprite());
        player.setZIndex(10);
        scene.addGameObject(player);
        playerAnchor.set(player.getTransform().position);

        wavePauseTimer = new Timer(WAVE_PAUSE_DURATION);

        themeTransition.initBlock(0);

        window.getEditorManager().setActiveScene(scene);
        window.getEditorManager().setActiveCamera(camera);
        debugProvider = new OpdEditorDebugProvider(this, window);
        window.getEditorManager().registerDebugProvider(debugProvider);

        refreshEquippedWeapons();
        openInventory(false);

        Logger.info("OPD", "Oil Protection Defense started. E=Inventory ESC=Pause");
    }

    @Override
    public void onEnter() {
        GameAudio.playThemedMusicForBlock(0);
    }

    @Override
    public void onExit() {
        window.getEditorManager().clearDebugProvider();
        GameAudio.stopMusic();
    }

    private void openInventory(boolean fromPlayerInput) {
        if (inventoryOpen) {
            return;
        }
        inventoryOpen = true;
        screenManager.pushScreen(new InventoryPopupScreen(runState, this::refreshEquippedWeapons));
    }

    private void refreshEquippedWeapons() {
        combatLoadout = runState.buildCombatLoadout();
        equippedProjectileWeapons.clear();
        equippedProjectileWeapons.addAll(combatLoadout.projectileWeapons);
        equippedBeams.clear();
        equippedBeams.addAll(combatLoadout.beams);

        maxHp = BASE_HP + combatLoadout.bootsMaxHpBonus;
        maxShield = maxHp + combatLoadout.beltMaxShieldBonus;
        if (currentHp <= 0) {
            currentHp = maxHp;
        } else {
            currentHp = Math.min(currentHp, maxHp);
        }
        currentShield = Math.min(currentShield, maxShield);

        bootsHealElapsed = 0f;
        bootsHealCooldownElapsed = 0f;

        inventoryOpen = false;

        if (!runState.isSetupPhase() && !waveActive && waveNumber == 0) {
            startNextWave();
        }
    }

    @Override
    public void update(float dt) {
        if (Input.isKeyJustPressed(Input.KEY_ESCAPE)) {
            screenManager.pushScreen(new PauseScreen());
            return;
        }

        if (Input.isKeyJustPressed(Input.KEY_E) && !inventoryOpen) {
            openInventory(true);
            return;
        }

        if (runState.isSetupPhase() || inventoryOpen) {
            lockPlayerPosition();
            OneShotAnimation.updateAll(hitEffects, dt);
            scene.update(dt);
            return;
        }

        lockPlayerPosition();

        for (RunState.EquippedWeapon w : equippedProjectileWeapons) {
            w.cooldownTimer.update(dt);
            if (w.cooldownTimer.isReady()) {
                spawnProjectile(w);
            }
        }

        updateBeams(dt);
        updatePassives(dt);

        if (!waveActive) {
            if (themeTransition.isRunning()) {
                themeTransition.update(dt);
                updateProjectiles(dt);
                OneShotAnimation.updateAll(hitEffects, dt);
                scene.update(dt);
                return;
            }
            wavePauseTimer.update(dt);
            if (wavePauseTimer.isReady()) {
                startNextWave();
            }
            updateProjectiles(dt);
            OneShotAnimation.updateAll(hitEffects, dt);
            scene.update(dt);
            return;
        }

        if (enemiesToSpawn > 0) {
            spawnTimer -= dt;
            if (spawnTimer <= 0f) {
                int batch = Math.min(spawnBatchSize, enemiesToSpawn);
                spawnEnemyBatch(batch);
                enemiesToSpawn -= batch;
                spawnTimer = spawnInterval;
            }
        }

        updateProjectiles(dt);
        updateEnemies(dt);
        checkCollisions();
        OneShotAnimation.updateAll(hitEffects, dt);
        scene.update(dt);

        if (enemiesToSpawn <= 0 && enemies.isEmpty()) {
            waveActive = false;
            Logger.info("OPD", "Wave " + waveNumber + " cleared! Score: " + score);
            if (waveNumber > 0 && waveNumber % 10 == 0) {
                int nextBlock = BackgroundCatalog.getBlockIndexForUpcomingWave(waveNumber);
                themeTransition.begin(nextBlock, this::startNextWave);
            } else {
                wavePauseTimer.reset();
            }
        }

        if (currentHp <= 0) {
            screenManager.swapScreen(new GameOverScreen(score, waveNumber));
        }
    }

    @Override
    public void render() {
        viewportScaler.updateWindowSize(window.getWidth(), window.getHeight());
        viewportScaler.applyPlayViewport();

        renderBackground();
        scene.render(batch, camera);
        renderHitEffects();
        renderBeams();

        viewportScaler.restoreFullViewport();
        renderHUD();
    }

    private void renderBackground() {
        Texture bg = themeTransition.getCurrentBackground();
        if (bg == null) {
            return;
        }
        float x = -PLAY_AREA_WIDTH / 2f;
        float y = -PLAY_AREA_HEIGHT / 2f;
        batch.setProjection(camera.getProjectionViewMatrix());
        batch.beginBatch();
        if (themeTransition.isRunning()) {
            themeTransition.renderWipe(batch, x, y, PLAY_AREA_WIDTH, PLAY_AREA_HEIGHT);
        } else {
            drawBackgroundQuad(bg, x, y, PLAY_AREA_WIDTH, PLAY_AREA_HEIGHT);
        }
        batch.endBatch();
        batch.flush();
    }

    private void drawBackgroundQuad(Texture bg, float x, float y, float w, float h) {
        float texAspect = (float) bg.getWidth() / bg.getHeight();
        float quadAspect = w / h;
        Vector2f uvMin = new Vector2f(0f, 0f);
        Vector2f uvMax = new Vector2f(1f, 1f);
        if (Math.abs(texAspect - quadAspect) > 0.01f) {
            if (texAspect > quadAspect) {
                float visible = quadAspect / texAspect;
                float pad = (1f - visible) / 2f;
                uvMin.x = pad;
                uvMax.x = 1f - pad;
            } else {
                float visible = texAspect / quadAspect;
                float pad = (1f - visible) / 2f;
                uvMin.y = pad;
                uvMax.y = 1f - pad;
            }
        }
        batch.drawQuad(x, y, w, h, bg, new Vector4f(1f, 1f, 1f, 1f), uvMin, uvMax);
    }

    private void renderHitEffects() {
        if (hitEffects.isEmpty()) {
            return;
        }
        batch.setProjection(camera.getProjectionViewMatrix());
        batch.beginBatch();
        OneShotAnimation.renderAll(batch, hitEffects);
        batch.endBatch();
        batch.flush();
    }

    @Override
    public void dispose() {
        if (scene != null) {
            scene.dispose();
        }
        if (batch != null) {
            batch.dispose();
        }
    }

    private void startNextWave() {
        waveNumber++;
        currentStage = WaveCatalog.getStageForWave(waveNumber);

        spawnInterval = WaveCatalog.getSpawnInterval(currentStage, waveNumber);
        spawnBatchSize = WaveCatalog.getSpawnBatchSize(currentStage, waveNumber);

        if (WaveCatalog.isBossWave(waveNumber)) {
            spawnBoss();
            enemiesToSpawn = WaveCatalog.getEnemyCount(currentStage, waveNumber)
                    + WaveCatalog.getBossMinionCount();
        } else {
            enemiesToSpawn = WaveCatalog.getEnemyCount(currentStage, waveNumber);
        }

        spawnTimer = 0f;
        waveActive = true;
        Logger.info("OPD", "Wave " + waveNumber + " started (" + currentStage.name + ")");
    }

    private void spawnBoss() {
        String bossId = WaveCatalog.getBossEnemyId();
        EnemyDefinition def = EnemyCatalog.get(bossId);
        if (def == null) {
            return;
        }
        spawnEnemyAt(def, 0f, PLAY_AREA_HEIGHT / 2f - def.height, true);
    }

    private void spawnEnemyBatch(int count) {
        for (int i = 0; i < count; i++) {
            spawnEnemy();
        }
    }

    private void spawnEnemy() {
        String enemyId = WaveCatalog.pickEnemyId(currentStage, rng);
        EnemyDefinition def = EnemyCatalog.get(enemyId);
        if (def == null) {
            return;
        }
        float halfW = PLAY_AREA_WIDTH / 2f;
        float topEdge = PLAY_AREA_HEIGHT / 2f;
        float x = -halfW + def.width + rng.nextFloat() * (PLAY_AREA_WIDTH - def.width * 2f);
        spawnEnemyAt(def, x, topEdge, false);
    }

    private void spawnEnemyAt(EnemyDefinition def, float x, float y, boolean isBossSpawn) {
        GameObject go = new GameObject("enemy_" + def.id + "_" + System.nanoTime(),
                new Transform(new Vector2f(x, y), new Vector2f(def.width, def.height)));
        go.addSprite(getEnemySprite(def));
        go.setZIndex(3);
        scene.addGameObject(go);

        float waveScale = 1f + waveNumber * 0.08f;
        if (isBossSpawn || "boss".equals(def.id)) {
            waveScale *= 1.5f + waveNumber * 0.02f;
        }
        enemies.add(new EnemyData(go, def, waveScale));
    }

    private void lockPlayerPosition() {
        player.getTransform().position.set(playerAnchor);
    }

    private void spawnProjectile(RunState.EquippedWeapon weapon) {
        Vector2f origin = getPlayerCenter();
        origin.y += PLAYER_HEIGHT * 0.4f;

        float speed = weapon.def.projectileSpeed;
        Vector2f target = findNearestEnemyCenter(origin);
        float vx;
        float vy;
        if (target != null) {
            float dx = target.x - origin.x;
            float dy = target.y - origin.y;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len > 0.001f) {
                vx = dx / len * speed;
                vy = dy / len * speed;
            } else {
                vx = 0f;
                vy = speed;
            }
        } else {
            vx = 0f;
            vy = speed;
        }

        float pw = weapon.def.projectileWidth;
        float ph = weapon.def.projectileHeight;
        GameObject proj = new GameObject("proj_" + weapon.def.id,
                new Transform(new Vector2f(origin.x - pw / 2f, origin.y),
                        new Vector2f(pw, ph)));
        proj.getTransform().rotation = velocityToRotation(vx, vy);
        Texture laserTex = loadLaserTexture(weapon.def.projectileLaserId);
        if (laserTex != null) {
            proj.addSprite(new Sprite(laserTex));
        } else {
            proj.addSprite(new Sprite(weapon.getColor()));
        }
        proj.setZIndex(5);
        scene.addGameObject(proj);
        projectiles.add(new ProjectileData(proj, weapon, vx, vy));
        GameAudio.playShoot();
    }

    private void updateProjectiles(float dt) {
        float halfW = PLAY_AREA_WIDTH / 2f + 50f;
        float halfH = PLAY_AREA_HEIGHT / 2f + 50f;

        for (int i = projectiles.size() - 1; i >= 0; i--) {
            ProjectileData pd = projectiles.get(i);
            Transform t = pd.go.getTransform();
            Vector2f center = getObjectCenter(t);

            if (pd.weapon.def.homingStrength > 0f) {
                Vector2f nearest = findNearestEnemyCenter(center);
                if (nearest != null) {
                    float dx = nearest.x - center.x;
                    float dy = nearest.y - center.y;
                    float len = (float) Math.sqrt(dx * dx + dy * dy);
                    if (len > 0.001f) {
                        float desiredVx = dx / len * pd.weapon.def.projectileSpeed;
                        float desiredVy = dy / len * pd.weapon.def.projectileSpeed;
                        float steer = pd.weapon.def.homingStrength * dt;
                        pd.velX += (desiredVx - pd.velX) * steer;
                        pd.velY += (desiredVy - pd.velY) * steer;
                        float curLen = (float) Math.sqrt(pd.velX * pd.velX + pd.velY * pd.velY);
                        if (curLen > 0.001f) {
                            float scale = pd.weapon.def.projectileSpeed / curLen;
                            pd.velX *= scale;
                            pd.velY *= scale;
                        }
                    }
                }
            }

            t.position.x += pd.velX * dt;
            t.position.y += pd.velY * dt;
            t.rotation = velocityToRotation(pd.velX, pd.velY);

            center = getObjectCenter(t);
            if (center.x < -halfW || center.x > halfW || center.y < -halfH || center.y > halfH) {
                scene.removeGameObject(pd.go);
                projectiles.remove(i);
            }
        }
    }

    private Vector2f findNearestEnemyCenter(Vector2f from) {
        Vector2f nearest = null;
        float bestDistSq = Float.MAX_VALUE;
        for (EnemyData ed : enemies) {
            Vector2f ec = getObjectCenter(ed.go.getTransform());
            float dx = ec.x - from.x;
            float dy = ec.y - from.y;
            float distSq = dx * dx + dy * dy;
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                nearest = ec;
            }
        }
        return nearest;
    }

    private Vector2f getObjectCenter(Transform t) {
        return new Vector2f(t.position.x + t.scale.x / 2f, t.position.y + t.scale.y / 2f);
    }

    private float velocityToRotation(float vx, float vy) {
        return (float) Math.toDegrees(Math.atan2(vy, vx)) - 90f;
    }

    private void updateBeams(float dt) {
        Vector2f origin = getPlayerCenter();
        origin.y += PLAYER_HEIGHT * 0.35f;

        for (RunState.EquippedBeam beam : equippedBeams) {
            EnemyData target = findNearestEnemyInRange(origin, beam.def.beamRange);
            beam.damageTickTimer.update(dt);
            if (target != null && beam.damageTickTimer.isReady()) {
                damageEnemy(target, beam.def.damage);
            }
        }
    }

    private void updatePassives(float dt) {
        if (combatLoadout.hasBelt && combatLoadout.shieldRegenPerSecond > 0f && currentShield < maxShield) {
            currentShield = Math.min(maxShield, currentShield + combatLoadout.shieldRegenPerSecond * dt);
        }

        if (!combatLoadout.hasBoots || currentHp >= maxHp) {
            return;
        }

        if (bootsHealCooldownElapsed > 0f) {
            bootsHealCooldownElapsed = Math.max(0f, bootsHealCooldownElapsed - dt);
            return;
        }

        bootsHealElapsed += dt;
        if (bootsHealElapsed >= combatLoadout.healInterval) {
            currentHp = Math.min(maxHp, currentHp + combatLoadout.healAmount);
            bootsHealElapsed = 0f;
            bootsHealCooldownElapsed = combatLoadout.healCooldownDuration;
        }
    }

    private void applyPlayerDamage(int amount) {
        int remaining = amount;
        if (currentShield > 0f) {
            int fromShield = (int) Math.min(currentShield, remaining);
            currentShield -= fromShield;
            remaining -= fromShield;
        }
        if (remaining > 0) {
            currentHp -= remaining;
        }
    }

    private void damageEnemy(EnemyData ed, int damage) {
        ed.hp -= damage;
        if (ed.hp <= 0) {
            onEnemyKilled(ed);
        }
    }

    private void onEnemyKilled(EnemyData ed) {
        int waveScoreBonus = (waveNumber / 10) * 2;
        score += ed.scoreValue + waveScoreBonus;
        grantEnemyBucks(ed);
        spawnDeathSmoke(getObjectCenter(ed.go.getTransform()));
        GameAudio.playHit();
        scene.removeGameObject(ed.go);
        enemies.remove(ed);
    }

    private void grantEnemyBucks(EnemyData ed) {
        if (ed.bucksDropGuaranteed) {
            runState.addBucks(ed.bucksDropMax);
            return;
        }
        if (rng.nextFloat() >= ed.bucksDropChance) {
            return;
        }
        int min = Math.min(ed.bucksDropMin, ed.bucksDropMax);
        int max = Math.max(ed.bucksDropMin, ed.bucksDropMax);
        int amount = min + rng.nextInt(max - min + 1);
        runState.addBucks(amount);
    }

    private EnemyData findNearestEnemyInRange(Vector2f from, float range) {
        EnemyData nearest = null;
        float bestDistSq = Float.MAX_VALUE;
        float rangeSq = range * range;
        for (EnemyData ed : enemies) {
            Vector2f ec = getObjectCenter(ed.go.getTransform());
            float dx = ec.x - from.x;
            float dy = ec.y - from.y;
            float distSq = dx * dx + dy * dy;
            if (distSq <= rangeSq && distSq < bestDistSq) {
                bestDistSq = distSq;
                nearest = ed;
            }
        }
        return nearest;
    }

    private void renderBeams() {
        if (equippedBeams.isEmpty()) {
            return;
        }
        Vector2f origin = getPlayerCenter();
        origin.y += PLAYER_HEIGHT * 0.35f;

        batch.setProjection(camera.getProjectionViewMatrix());
        batch.beginBatch();
        for (RunState.EquippedBeam beam : equippedBeams) {
            Vector2f targetCenter = null;
            EnemyData target = findNearestEnemyInRange(origin, beam.def.beamRange);
            if (target != null) {
                targetCenter = getObjectCenter(target.go.getTransform());
            }
            if (targetCenter != null) {
                drawBeamLine(origin, targetCenter, beam.def.beamWidth, beam.getColor());
            }
        }
        batch.endBatch();
        batch.flush();
    }

    private void drawBeamLine(Vector2f from, Vector2f to, float thickness, Vector4f color) {
        float dx = to.x - from.x;
        float dy = to.y - from.y;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 2f) {
            return;
        }
        float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
        float midX = (from.x + to.x) / 2f - len / 2f;
        float midY = (from.y + to.y) / 2f - thickness / 2f;
        Vector4f beamColor = new Vector4f(color);
        beamColor.w = 0.85f;
        batch.drawQuad(midX, midY, len, thickness, angle, beamColor);
    }

    private void updateEnemies(float dt) {
        Vector2f playerCenter = getPlayerCenter();

        for (EnemyData ed : enemies) {
            Transform t = ed.go.getTransform();
            float ex = t.position.x + t.scale.x / 2f;
            float ey = t.position.y + t.scale.y / 2f;

            float dx = playerCenter.x - ex;
            float dy = playerCenter.y - ey;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            if (dist > ed.attackRange) {
                float move = ed.moveSpeed * dt;
                if (dist > 0.001f) {
                    t.position.x += (dx / dist) * move;
                    t.position.y += (dy / dist) * move;
                }
            } else {
                ed.attackTimer -= dt;
                if (ed.attackTimer <= 0f) {
                    applyPlayerDamage(1);
                    ed.attackTimer = ed.attackInterval;
                }
            }
        }
    }

    private Vector2f getPlayerCenter() {
        Transform pt = player.getTransform();
        return new Vector2f(
                pt.position.x + PLAYER_WIDTH / 2f,
                pt.position.y + PLAYER_HEIGHT / 2f);
    }

    private void checkCollisions() {
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            ProjectileData pd = projectiles.get(i);
            AABB projBox = new AABB(pd.go.getTransform());

            for (int j = enemies.size() - 1; j >= 0; j--) {
                EnemyData ed = enemies.get(j);
                AABB enemyBox = new AABB(ed.go.getTransform());

                if (projBox.intersects(enemyBox)) {
                    damageEnemy(ed, pd.weapon.def.damage);
                    scene.removeGameObject(pd.go);
                    projectiles.remove(i);
                    break;
                }
            }
        }
    }

    private void renderHUD() {
        int w = window.getWidth();
        int h = window.getHeight();
        hudCamera.adjustProjection(w, h);

        float halfW = w / 2f;
        float halfH = h / 2f;

        batch.setProjection(hudCamera.getProjectionViewMatrix());
        batch.beginBatch();

        Vector4f white = new Vector4f(1f, 1f, 1f, 0.9f);
        Vector4f green = new Vector4f(0.3f, 1f, 0.3f, 0.9f);
        Vector4f red = new Vector4f(1f, 0.3f, 0.3f, 0.9f);

        float margin = GameUiAssets.s(10f);
        float lineStep = hudFont.getLineHeight() + GameUiAssets.s(4f);
        float labelLine = hudFont.getLineHeight();

        float hudTopY = halfH - margin;
        TextRenderer.drawText(batch, hudFont, "SCORE: " + score,
                -halfW + margin, hudTopY, white);
        TextRenderer.drawText(batch, hudFont, "BUCKS: " + runState.getBucks(),
                -halfW + margin, hudTopY - lineStep, new Vector4f(0.3f, 1f, 0.5f, 0.9f));

        String waveText = runState.isSetupPhase() ? "SETUP"
                : themeTransition.isRunning() ? "THEME CHANGE"
                : waveActive ? "WAVE " + waveNumber : "NEXT WAVE...";
        TextRenderer.drawTextCentered(batch, hudFont, waveText,
                0f, hudTopY - lineStep * 2.2f, white);

        float barW = GameUiAssets.s(140f);
        float barH = GameUiAssets.s(12f);
        float barGap = GameUiAssets.s(5f);
        float rowGap = GameUiAssets.s(10f);
        float barX = halfW - margin - barW;
        Vector4f blue = new Vector4f(0.3f, 0.5f, 1f, 0.9f);
        Vector4f barBg = new Vector4f(0.3f, 0.3f, 0.3f, 0.7f);

        float statsAnchorY = halfH - GameUiAssets.s(108f);

        float shieldLabelY = statsAnchorY;
        float shieldBarY = shieldLabelY - labelLine - barGap;
        TextRenderer.drawText(batch, hudFont,
                "SHIELD " + Math.round(currentShield) + "/" + maxShield,
                barX, shieldLabelY, white);
        batch.drawQuad(barX, shieldBarY, barW, barH, barBg);
        float shieldPct = maxShield > 0 ? currentShield / maxShield : 0f;
        batch.drawQuad(barX, shieldBarY, barW * shieldPct, barH, blue);

        float hpLabelY = shieldBarY - barH - rowGap;
        float hpBarY = hpLabelY - labelLine - barGap;
        TextRenderer.drawText(batch, hudFont,
                "HP " + currentHp + "/" + maxHp,
                barX, hpLabelY, white);
        batch.drawQuad(barX, hpBarY, barW, barH, barBg);
        float healthPct = maxHp > 0 ? (float) currentHp / maxHp : 0f;
        batch.drawQuad(barX, hpBarY, barW * healthPct, barH, healthPct > 0.3f ? green : red);

        int weaponLines = equippedProjectileWeapons.size() + equippedBeams.size();
        float weaponY = -halfH + margin + (weaponLines - 1) * lineStep;
        for (RunState.EquippedWeapon wpn : equippedProjectileWeapons) {
            TextRenderer.drawText(batch, hudFont,
                    wpn.def.name + " (DMG:" + wpn.def.damage + ")",
                    -halfW + margin, weaponY, wpn.getColor());
            weaponY -= lineStep;
        }
        for (RunState.EquippedBeam beam : equippedBeams) {
            TextRenderer.drawText(batch, hudFont,
                    beam.def.name + " (BEAM:" + beam.def.damage + ")",
                    -halfW + margin, weaponY, beam.getColor());
            weaponY -= lineStep;
        }

        TextRenderer.drawTextCentered(batch, hudFont, "E - Inventory  ESC - Pause  F1 - Editor",
                0f, -halfH + margin, new Vector4f(0.7f, 0.7f, 0.7f, 0.6f));

        batch.endBatch();
        batch.flush();
    }

    private Texture loadLaserTexture(String laserId) {
        if (laserId == null || laserId.isEmpty()) {
            return null;
        }
        String path = "/atlas/laser-sprites/" + laserId + ".png";
        return AssetManager.loadTextureResource(path);
    }

    private void spawnDeathSmoke(Vector2f center) {
        OneShotAnimation anim = new OneShotAnimation(0.06f);
        for (int i = 1; i <= 8; i++) {
            String path = String.format("/animated-fx/Smoke/FX002/FX002_%02d.png", i);
            anim.addFrame(AssetManager.loadTextureResource(path));
        }
        anim.setPosition(center.x - 16f, center.y - 16f, 32f, 32f);
        hitEffects.add(anim);
    }

    private Sprite getPlayerSprite() {
        if (spriteSheet != null) {
            int topRow = spriteSheet.getRows() - 1;
            if (topRow >= 0 && spriteSheet.getColumns() > 0) {
                return spriteSheet.getSprite(0, topRow);
            }
        }
        return new Sprite(new Vector4f(0.2f, 0.6f, 1.0f, 1.0f));
    }

    private Sprite getEnemySprite(EnemyDefinition def) {
        if (spriteSheet != null) {
            int topRow = spriteSheet.getRows() - 1;
            if (topRow >= 0 && def.spriteCol < spriteSheet.getColumns()) {
                return spriteSheet.getSprite(def.spriteCol, topRow);
            }
        }
        return new Sprite(def.getColor());
    }

    // Debug API for ImGui editor panels

    public int getDebugWaveNumber() {
        return waveNumber;
    }

    public boolean isDebugWaveActive() {
        return waveActive;
    }

    public int getDebugEnemiesToSpawn() {
        return enemiesToSpawn;
    }

    public float getDebugSpawnInterval() {
        return spawnInterval;
    }

    public String getDebugStageName() {
        return currentStage != null ? currentStage.name : "none";
    }

    public boolean isDebugThemeTransitionRunning() {
        return themeTransition.isRunning();
    }

    public int getDebugHealth() {
        return currentHp;
    }

    public int getDebugMaxHealth() {
        return maxHp;
    }

    public int getDebugShield() {
        return Math.round(currentShield);
    }

    public int getDebugMaxShield() {
        return maxShield;
    }

    public int getDebugScore() {
        return score;
    }

    public int getDebugEnemyCount() {
        return enemies.size();
    }

    public int getDebugProjectileCount() {
        return projectiles.size();
    }

    public int getDebugBucks() {
        return runState.getBucks();
    }

    public boolean isDebugSetupPhase() {
        return runState.isSetupPhase();
    }

    public boolean isDebugInventoryOpen() {
        return inventoryOpen;
    }

    public int getDebugEquippedWeaponCount() {
        return equippedProjectileWeapons.size() + equippedBeams.size();
    }

    public List<String> getDebugEnemyLines() {
        List<String> lines = new ArrayList<>();
        for (EnemyData ed : enemies) {
            Vector2f c = getObjectCenter(ed.go.getTransform());
            lines.add(String.format("%s HP:%d pos:(%.0f,%.0f)", ed.enemyId, ed.hp, c.x, c.y));
        }
        return lines;
    }

    public List<String> getDebugInventoryLines() {
        List<String> lines = new ArrayList<>();
        runState.getInventory().getPlacedItems().forEach(placed ->
                lines.add(placed.item.getDisplayName() + " tier " + placed.item.getTier()
                        + " at (" + placed.col + "," + placed.row + ")"));
        if (lines.isEmpty()) {
            lines.add("(empty)");
        }
        return lines;
    }

    public void debugSkipWave() {
        if (waveActive) {
            for (int i = enemies.size() - 1; i >= 0; i--) {
                scene.removeGameObject(enemies.get(i).go);
                enemies.remove(i);
            }
            enemiesToSpawn = 0;
            waveActive = false;
            wavePauseTimer.reset();
        } else if (!themeTransition.isRunning()) {
            startNextWave();
        }
    }

    public void debugForceThemeTransition() {
        if (themeTransition.isRunning()) {
            return;
        }
        int nextBlock = BackgroundCatalog.getBlockIndexForUpcomingWave(Math.max(waveNumber, 1));
        themeTransition.begin(nextBlock, () -> {
            if (!waveActive) {
                wavePauseTimer.reset();
            }
        });
    }

    public void debugKillAllEnemies() {
        for (int i = enemies.size() - 1; i >= 0; i--) {
            scene.removeGameObject(enemies.get(i).go);
            enemies.remove(i);
        }
    }

    public void debugSpawnRandomEnemy() {
        if (currentStage == null) {
            currentStage = WaveCatalog.getStageForWave(Math.max(waveNumber, 1));
        }
        spawnEnemy();
    }

    public void debugAddBucks(int amount) {
        runState.addBucks(amount);
    }

    private static class ProjectileData {
        final GameObject go;
        final RunState.EquippedWeapon weapon;
        float velX;
        float velY;

        ProjectileData(GameObject go, RunState.EquippedWeapon weapon, float velX, float velY) {
            this.go = go;
            this.weapon = weapon;
            this.velX = velX;
            this.velY = velY;
        }
    }

    private static class EnemyData {
        final GameObject go;
        final String enemyId;
        final float moveSpeed;
        final float attackRange;
        final float attackInterval;
        final int scoreValue;
        final float bucksDropChance;
        final int bucksDropMin;
        final int bucksDropMax;
        final boolean bucksDropGuaranteed;
        int hp;
        float attackTimer;

        EnemyData(GameObject go, EnemyDefinition def, float waveScale) {
            this.go = go;
            this.enemyId = def.id;
            this.hp = Math.round(def.hp * waveScale);
            this.moveSpeed = def.moveSpeed + waveScale * 5f;
            this.attackRange = def.attackRange;
            this.attackInterval = def.attackInterval;
            this.scoreValue = def.scoreValue;
            this.bucksDropChance = def.bucksDropChance > 0f ? def.bucksDropChance : 0.35f;
            this.bucksDropMin = def.bucksDropMin > 0 ? def.bucksDropMin : 1;
            this.bucksDropMax = def.bucksDropMax > 0 ? def.bucksDropMax : Math.max(1, def.bucksDrop);
            this.bucksDropGuaranteed = def.bucksDropGuaranteed;
            this.attackTimer = def.attackInterval;
        }
    }
}
