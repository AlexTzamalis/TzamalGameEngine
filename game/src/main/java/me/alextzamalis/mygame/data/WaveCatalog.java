package me.alextzamalis.mygame.data;

import com.google.gson.Gson;
import me.alextzamalis.engine.core.Logger;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class WaveCatalog {

    private static final Gson GSON = new Gson();

    private static WavesFile data;

    private WaveCatalog() {
    }

    public static void load() {
        if (data != null) {
            return;
        }
        try (InputStream is = WaveCatalog.class.getResourceAsStream("/config/waves.json")) {
            if (is == null) {
                throw new RuntimeException("waves.json not found");
            }
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            data = GSON.fromJson(json, WavesFile.class);
            Logger.info("Data", "Loaded " + (data.stages != null ? data.stages.size() : 0) + " wave stages.");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load waves.json", e);
        }
    }

    public static WaveStageDefinition getStageForWave(int waveNumber) {
        load();
        for (WaveStageDefinition stage : data.stages) {
            if (waveNumber >= stage.startWave && waveNumber <= stage.endWave) {
                return stage;
            }
        }
        return data.stages.get(data.stages.size() - 1);
    }

    public static boolean isBossWave(int waveNumber) {
        load();
        return waveNumber > 0 && waveNumber % data.bossWaveInterval == 0;
    }

    public static String getBossEnemyId() {
        load();
        return data.bossEnemyId;
    }

    public static int getEnemyCount(WaveStageDefinition stage, int waveNumber) {
        return stage.baseEnemyCount + waveNumber * stage.enemyCountPerWave;
    }

    public static float getSpawnInterval(WaveStageDefinition stage, int waveNumber) {
        int wavesIntoStage = Math.max(0, waveNumber - stage.startWave);
        return Math.max(0.12f, stage.spawnInterval - wavesIntoStage * stage.spawnIntervalDecay);
    }

    public static int getSpawnBatchSize(WaveStageDefinition stage, int waveNumber) {
        int wavesIntoStage = Math.max(0, waveNumber - stage.startWave);
        int size = stage.spawnBatchSize + wavesIntoStage * stage.spawnBatchPerWave;
        return Math.min(stage.spawnBatchMax, Math.max(1, size));
    }

    public static int getBossMinionCount() {
        load();
        return Math.max(0, data.bossMinionCount);
    }

    public static String pickEnemyId(WaveStageDefinition stage, Random rng) {
        int total = 0;
        for (Map.Entry<String, Integer> entry : stage.spawnWeights.entrySet()) {
            total += entry.getValue();
        }
        if (total <= 0) {
            return "grunt";
        }
        int roll = rng.nextInt(total);
        int cumulative = 0;
        for (Map.Entry<String, Integer> entry : stage.spawnWeights.entrySet()) {
            cumulative += entry.getValue();
            if (roll < cumulative) {
                return entry.getKey();
            }
        }
        return stage.spawnWeights.keySet().iterator().next();
    }

    private static class WavesFile {
        int bossWaveInterval = 10;
        String bossEnemyId = "boss";
        int bossMinionCount = 20;
        List<WaveStageDefinition> stages = new ArrayList<>();
    }
}
