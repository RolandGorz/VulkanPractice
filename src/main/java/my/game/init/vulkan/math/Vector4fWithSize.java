package my.game.init.vulkan.math;

import org.joml.Vector4f;

public class Vector4fWithSize extends Vector4f implements LinearAlgebraWithSize {
    public static final int SIZE = Float.BYTES * 4;

    public Vector4fWithSize(float x, float y, float z, float w) {
        super(x, y, z, w);
    }

    public Vector4fWithSize() {
        super();
    }

    @Override
    public int getSize() {
        return SIZE;
    }
}
