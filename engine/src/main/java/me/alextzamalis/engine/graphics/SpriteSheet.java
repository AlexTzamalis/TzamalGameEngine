package me.alextzamalis.engine.graphics;

import me.alextzamalis.engine.scene.Sprite;

import org.joml.Vector2f;
import org.joml.Vector4f;

/**
 * Divides a {@link Texture} into a uniform grid of tiles and provides
 * easy sprite lookup by column/row or linear index.
 *
 * <h2>Usage example</h2>
 * <pre>{@code
 * Texture atlas = new Texture("assets/tileset.png");
 * SpriteSheet sheet = new SpriteSheet(atlas, 16, 16);
 *
 * // Get tile at column 3, row 1
 * Sprite tile = sheet.getSprite(3, 1);
 *
 * // Get tile by linear index (row-major, bottom-to-top)
 * Sprite tile2 = sheet.getSprite(5);
 *
 * // Get a 2x2 region for a large sprite
 * Sprite boss = sheet.getSpriteRegion(4, 2, 2, 2);
 *
 * GameObject go = new GameObject("tile", someTransform);
 * go.addSprite(tile);
 * }</pre>
 *
 * <p>UV coordinates are computed assuming the texture was loaded with
 * STB's vertical flip (Y=0 at the bottom). Row 0 is the bottom row
 * of the atlas.</p>
 *
 * @author Alexandros Tzamalis
 * @see Sprite
 * @see Texture
 */
public class SpriteSheet {

    private final Texture texture;
    private final int tileWidth;
    private final int tileHeight;
    private final int columns;
    private final int rows;

    /**
     * Creates a sprite sheet from an existing texture.
     *
     * @param texture    the atlas texture.
     * @param tileWidth  width of a single tile in pixels.
     * @param tileHeight height of a single tile in pixels.
     * @throws IllegalArgumentException if tile dimensions are non-positive
     *                                  or larger than the texture.
     */
    public SpriteSheet(Texture texture, int tileWidth, int tileHeight) {
        if (tileWidth <= 0 || tileHeight <= 0) {
            throw new IllegalArgumentException(
                    "Tile dimensions must be positive: " + tileWidth + "x" + tileHeight);
        }
        if (tileWidth > texture.getWidth() || tileHeight > texture.getHeight()) {
            throw new IllegalArgumentException(
                    "Tile dimensions (" + tileWidth + "x" + tileHeight
                    + ") exceed texture size (" + texture.getWidth() + "x"
                    + texture.getHeight() + ")");
        }
        this.texture = texture;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.columns = texture.getWidth() / tileWidth;
        this.rows = texture.getHeight() / tileHeight;
    }

    /**
     * Returns a sprite for the tile at the given grid position.
     *
     * @param col column index (0-based, left to right).
     * @param row row index (0-based, bottom to top).
     * @return a new Sprite with UV coordinates for that tile.
     * @throws IndexOutOfBoundsException if col or row is out of range.
     */
    public Sprite getSprite(int col, int row) {
        validateBounds(col, row);

        float texW = texture.getWidth();
        float texH = texture.getHeight();

        float uvMinX = (col * tileWidth) / texW;
        float uvMinY = (row * tileHeight) / texH;
        float uvMaxX = ((col + 1) * tileWidth) / texW;
        float uvMaxY = ((row + 1) * tileHeight) / texH;

        Sprite sprite = new Sprite(texture);
        sprite.uvMin = new Vector2f(uvMinX, uvMinY);
        sprite.uvMax = new Vector2f(uvMaxX, uvMaxY);
        return sprite;
    }

    /**
     * Returns a sprite for the tile at the given linear index.
     *
     * <p>Indices are row-major, bottom-to-top: index 0 is the bottom-left
     * tile, index {@code columns - 1} is the bottom-right, index
     * {@code columns} is the first tile of the second row, and so on.</p>
     *
     * @param index linear tile index.
     * @return a new Sprite with UV coordinates for that tile.
     * @throws IndexOutOfBoundsException if the index is out of range.
     */
    public Sprite getSprite(int index) {
        int total = columns * rows;
        if (index < 0 || index >= total) {
            throw new IndexOutOfBoundsException(
                    "Sprite index " + index + " out of range [0, " + total + ")");
        }
        int col = index % columns;
        int row = index / columns;
        return getSprite(col, row);
    }

    /**
     * Returns a sprite covering multiple tiles for large sprites
     * (for example a 2x2 boss on a 16x16 grid).
     *
     * @param col      column of the bottom-left tile of the region.
     * @param row      row of the bottom-left tile of the region.
     * @param spanCols number of tile columns the region spans.
     * @param spanRows number of tile rows the region spans.
     * @return a new Sprite with UV coordinates covering the entire region.
     * @throws IllegalArgumentException  if span values are non-positive.
     * @throws IndexOutOfBoundsException if the region exceeds the grid.
     */
    public Sprite getSpriteRegion(int col, int row, int spanCols, int spanRows) {
        if (spanCols <= 0 || spanRows <= 0) {
            throw new IllegalArgumentException(
                    "Span must be positive: " + spanCols + "x" + spanRows);
        }
        validateBounds(col, row);
        validateBounds(col + spanCols - 1, row + spanRows - 1);

        float texW = texture.getWidth();
        float texH = texture.getHeight();

        float uvMinX = (col * tileWidth) / texW;
        float uvMinY = (row * tileHeight) / texH;
        float uvMaxX = ((col + spanCols) * tileWidth) / texW;
        float uvMaxY = ((row + spanRows) * tileHeight) / texH;

        Sprite sprite = new Sprite(texture);
        sprite.uvMin = new Vector2f(uvMinX, uvMinY);
        sprite.uvMax = new Vector2f(uvMaxX, uvMaxY);
        return sprite;
    }

    /** @return the number of tile columns in this sheet. */
    public int getColumns() {
        return columns;
    }

    /** @return the number of tile rows in this sheet. */
    public int getRows() {
        return rows;
    }

    /** @return the width of a single tile in pixels. */
    public int getTileWidth() {
        return tileWidth;
    }

    /** @return the height of a single tile in pixels. */
    public int getTileHeight() {
        return tileHeight;
    }

    /** @return the underlying atlas texture. */
    public Texture getTexture() {
        return texture;
    }

    private void validateBounds(int col, int row) {
        if (col < 0 || col >= columns) {
            throw new IndexOutOfBoundsException(
                    "Column " + col + " out of range [0, " + columns + ")");
        }
        if (row < 0 || row >= rows) {
            throw new IndexOutOfBoundsException(
                    "Row " + row + " out of range [0, " + rows + ")");
        }
    }
}
