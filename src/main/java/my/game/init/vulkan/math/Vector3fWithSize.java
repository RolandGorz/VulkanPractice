package my.game.init.vulkan.math;

import org.joml.Vector3f;

public class Vector3fWithSize extends Vector3f implements LinearAlgebraWithSize {
    public static final int SIZE = Float.BYTES * 3;

    public Vector3fWithSize(float x, float y, float z) {
        super(x, y, z);
    }

    public Vector3fWithSize() {
        super();
    }

    @Override
    public int getSize() {
        return SIZE;
    }
}
