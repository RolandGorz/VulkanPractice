package my.game.init.vulkan.struct;

import my.game.init.vulkan.math.Matrix4fWithSize;
import org.joml.Matrix4f;

public record UniformBufferObject(Matrix4f model, Matrix4f view, Matrix4f proj) implements Struct {
    public static final int SIZE = Matrix4fWithSize.SIZE * 3;
    public static final int MODEL_OFFSET = 0;
    public static final int VIEW_OFFSET = Matrix4fWithSize.SIZE;
    public static final int PROJECTION_OFFSET = Matrix4fWithSize.SIZE * 2;

    @Override
    public int getSize() {
        return 0;
    }
}
