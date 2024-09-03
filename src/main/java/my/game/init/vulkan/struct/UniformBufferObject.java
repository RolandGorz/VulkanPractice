package my.game.init.vulkan.struct;

import my.game.init.vulkan.math.Matrix2fWithSize;
import my.game.init.vulkan.math.Matrix4fWithSize;
import org.joml.Matrix2f;
import org.joml.Matrix4f;

//Vulkan expects the data in your structure to be aligned in memory in a specific way, for example:
//
//Scalars have to be aligned by N (= 4 bytes given 32 bit floats).
//
//A vec2 must be aligned by 2N (= 8 bytes)
//
//A vec3 or vec4 must be aligned by 4N (= 16 bytes)
//
//A nested structure must be aligned by the base alignment of its members rounded up to a multiple of 16.
//
//A mat4 matrix must have the same alignment as a vec4.
//
//You can find the full list of alignment requirements in the specification.
public record UniformBufferObject(Matrix2f model) implements Struct {
    public static final int SIZE = Matrix2fWithSize.SIZE;
    public static final int MODEL_OFFSET = 0;

    @Override
    public int getSize() {
        return SIZE;
    }
}
