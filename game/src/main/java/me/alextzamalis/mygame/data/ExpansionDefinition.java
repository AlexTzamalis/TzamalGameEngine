package me.alextzamalis.mygame.data;

import org.joml.Vector4f;

public class ExpansionDefinition {
    public String id;
    public String name;
    public int cellWidth = 1;
    public int cellHeight = 1;
    public int weight = 1;
    public float colorR = 0.5f;
    public float colorG = 0.7f;
    public float colorB = 0.5f;
    public float colorA = 1f;

    public Vector4f getColor() {
        return new Vector4f(colorR, colorG, colorB, colorA);
    }
}
