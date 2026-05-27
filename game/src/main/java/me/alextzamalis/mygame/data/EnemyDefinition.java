package me.alextzamalis.mygame.data;

import org.joml.Vector4f;

public class EnemyDefinition {
    public String id;
    public String name;
    public int hp = 1;
    public float moveSpeed = 80f;
    public float attackRange = 60f;
    public float attackInterval = 1.5f;
    public int scoreValue = 10;
    public int bucksDrop = 5;
    public float bucksDropChance = 0.35f;
    public int bucksDropMin = 1;
    public int bucksDropMax = 3;
    public boolean bucksDropGuaranteed = false;
    public float width = 32f;
    public float height = 32f;
    public int spriteCol = 6;
    public float colorR = 1f;
    public float colorG = 0.3f;
    public float colorB = 0.3f;

    public Vector4f getColor() {
        return new Vector4f(colorR, colorG, colorB, 1f);
    }
}
