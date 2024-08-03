package my.game.init.vulkan.drawing;

import my.game.init.vulkan.math.Vector2fWithSize;
import my.game.init.vulkan.math.Vector3fWithSize;

public class Vertex {
    public static final int SIZE = Vector2fWithSize.SIZE + Vector3fWithSize.SIZE;
    public static final int POSITION_OFFSET = 0;
    public static final int COLOR_OFFSET = Vector2fWithSize.SIZE;
    private final Vector2fWithSize pos;
    private final Vector3fWithSize color;
    public Vertex(final Vector2fWithSize pos, final Vector3fWithSize color) {
        this.pos = pos;
        this.color = color;
    }

    public Vector2fWithSize getPos() {
        return pos;
    }

    public Vector3fWithSize getColor() {
        return color;
    }

    public int getSize() {
        return SIZE;
    }
}
