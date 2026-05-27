package me.alextzamalis.mygame.data;

import org.joml.Vector4f;

public class GearDefinition {
    public String id;
    public String baseId;
    public int tier = 1;
    public String name;
    public String gearType = "boots";
    public int cellWidth = 1;
    public int cellHeight = 2;
    public int maxHpBonus = 0;
    public int maxShieldBonus = 0;
    public int healAmount = 1;
    public float healInterval = 8f;
    public float healCooldown = 5f;
    public float shieldRegenPerSecond = 1.5f;
    public float colorR = 0.5f;
    public float colorG = 0.5f;
    public float colorB = 0.5f;
    public float colorA = 1f;

    public boolean isBoots() {
        return "boots".equalsIgnoreCase(gearType);
    }

    public boolean isBelt() {
        return "belt".equalsIgnoreCase(gearType);
    }

    public Vector4f getColor() {
        return new Vector4f(colorR, colorG, colorB, colorA);
    }
}
