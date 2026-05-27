package me.alextzamalis.mygame.data;

import org.joml.Vector4f;

public class WeaponDefinition {
    public String id;
    public String baseId;
    public int tier = 1;
    public String name;
    public int cellWidth = 1;
    public int cellHeight = 1;
    public int damage = 1;
    public float cooldown = 0.5f;
    public float projectileSpeed = 500f;
    public float projectileWidth = 6f;
    public float projectileHeight = 14f;
    public float homingStrength = 0f;
    public String projectileLaserId = "01";
    public float colorR = 1f;
    public float colorG = 1f;
    public float colorB = 1f;
    public float colorA = 1f;
    public String fireMode = "projectile";
    public float beamTickInterval = 0.5f;
    public float beamRange = 400f;
    public float beamWidth = 5f;

    public boolean isBeam() {
        return "beam".equalsIgnoreCase(fireMode);
    }

    public Vector4f getColor() {
        return new Vector4f(colorR, colorG, colorB, colorA);
    }
}
