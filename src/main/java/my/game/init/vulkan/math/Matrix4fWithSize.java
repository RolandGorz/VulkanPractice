package my.game.init.vulkan.math;

import org.joml.Matrix4f;
import org.joml.Vector4f;

public class Matrix4fWithSize extends Matrix4f implements LinearAlgebraWithSize {
    public static final int SIZE = Vector4fWithSize.SIZE * 4;

    public Matrix4fWithSize() {
        super();
    }

    public Matrix4fWithSize(Vector4f col0,Vector4f col1, Vector4f col2,Vector4f col3) {
        super(col0, col1, col2, col3);
    }

    @Override
    public int getSize() {
        return SIZE;
    }
}
