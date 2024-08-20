package my.game.init.vulkan.drawing.memory.buffer;

import my.game.init.vulkan.struct.UniformBufferObject;
import org.joml.Matrix4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkExtent2D;

import java.nio.ByteBuffer;

public class UniformBuffer {
    private final UniformBufferObject uniformBufferObject;
    private final PointerBuffer uniformBufferMapped;
    private final VulkanBuffer vulkanBuffer;
    private static final int BUFFER_SIZE = UniformBufferObject.SIZE;
    public UniformBuffer(VkDevice device) {
        this.uniformBufferObject = new UniformBufferObject(new Matrix4f());
        vulkanBuffer = new VulkanBuffer(BUFFER_SIZE, device, VK13.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
                VK13.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK13.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
        uniformBufferMapped = vulkanBuffer.persistentMemoryMap();
    }

    private void memCpy() {
        ByteBuffer uniformBufferMappedByteBuffer = uniformBufferMapped.getByteBuffer(BUFFER_SIZE);
        uniformBufferObject.model().get(UniformBufferObject.MODEL_OFFSET, uniformBufferMappedByteBuffer);
    }

    public VulkanBuffer getVulkanBuffer() {
        return vulkanBuffer;
    }

    //Using a UBO this way is not the most efficient way to pass frequently changing values to the shader.
    // A more efficient way to pass a small buffer of data to shaders are push constants.
    public void update(VkExtent2D swapChainExtent) {
        uniformBufferObject.model().identity();
        //uniformBufferObject.model().rotate((float) (GLFW.glfwGetTime() * Math.toRadians(90)), 0.0f, 0.0f, 1.0f);

        //Set flag to true if using perspective since vulkan is zero to one for ndc z range instead of -1 to 1 like opengl
        //uniformBufferObject.proj().perspective((float) Math.toRadians(45),
        //        (float)swapChainExtent.width() / (float)swapChainExtent.height(), 0.1f, 10.0f, true);
        memCpy();
        uniformBufferMapped.rewind();
    }

    public void free() {
        vulkanBuffer.free();
        uniformBufferMapped.free();
    }
}
