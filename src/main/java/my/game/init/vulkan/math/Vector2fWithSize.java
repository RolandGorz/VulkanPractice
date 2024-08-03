package my.game.init.vulkan.math;

import org.joml.Vector2f;

public class Vector2fWithSize extends Vector2f implements VectorWithSize {
    public static final int SIZE = Float.BYTES * 2;

    public Vector2fWithSize(float x, float y) {
        super(x, y);
    }

    public Vector2fWithSize() {
        super();
    }

    @Override
    public int getSize() {
        return SIZE;
    }
}
