package me.alextzamalis.mygame.data;

import java.util.LinkedHashMap;
import java.util.Map;

public class WaveStageDefinition {
    public String name;
    public int startWave;
    public int endWave;
    public Map<String, Integer> spawnWeights = new LinkedHashMap<>();
    public int baseEnemyCount = 5;
    public int enemyCountPerWave = 2;
    public float spawnInterval = 1.0f;
    public float spawnIntervalDecay = 0.05f;
    public int spawnBatchSize = 2;
    public int spawnBatchPerWave = 0;
    public int spawnBatchMax = 5;
}
