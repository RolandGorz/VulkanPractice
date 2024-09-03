package my.game.init.vulkan.math;

import org.joml.Matrix2f;
import org.joml.Vector2f;

public class Matrix2fWithSize extends Matrix2f implements LinearAlgebraWithSize {
    public static final int SIZE = Vector2fWithSize.SIZE * 2;

    public Matrix2fWithSize() {
        super();
    }

    public Matrix2fWithSize(Vector2f col0, Vector2f col1) {
        super(col0, col1);
    }

    @Override
    public int getSize() {
        return SIZE;
    }
}
