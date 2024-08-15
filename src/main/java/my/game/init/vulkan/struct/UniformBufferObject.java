package my.game.init.vulkan.struct;

import my.game.init.vulkan.math.Matrix4fWithSize;
import org.joml.Matrix4f;

public record UniformBufferObject(Matrix4f model) implements Struct {
    public static final int SIZE = Matrix4fWithSize.SIZE;
    public static final int MODEL_OFFSET = 0;

    @Override
    public int getSize() {
        return 0;
    }
}
