package my.game.init.vulkan.drawing.memory;

import my.game.render.GraphicsRenderer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkDescriptorPoolCreateInfo;
import org.lwjgl.vulkan.VkDescriptorPoolSize;
import org.lwjgl.vulkan.VkDevice;

import java.nio.LongBuffer;

public class DescriptorPool {
    private final long descriptorPoolHandle;
    private final VkDevice device;

    public DescriptorPool(VkDevice device) {
        this.device = device;
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            VkDescriptorPoolSize.Buffer descriptorPoolSize = VkDescriptorPoolSize.calloc(1, memoryStack);
            descriptorPoolSize
                    .type(VK13.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                    .descriptorCount(GraphicsRenderer.MAX_FRAMES_IN_FLIGHT);
            VkDescriptorPoolCreateInfo descriptorPoolCreateInfo = VkDescriptorPoolCreateInfo.calloc(memoryStack);
            descriptorPoolCreateInfo
                    .sType(VK13.VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                    .pPoolSizes(descriptorPoolSize)
                    .maxSets(GraphicsRenderer.MAX_FRAMES_IN_FLIGHT);
            LongBuffer descriptorPoolBuffer = memoryStack.mallocLong(1);
            int result = VK13.vkCreateDescriptorPool(device, descriptorPoolCreateInfo, null, descriptorPoolBuffer);
            if (result != VK13.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to create descriptor pool. Error code: %d", result));
            }
            descriptorPoolHandle = descriptorPoolBuffer.get(0);
        }
    }

    public long getDescriptorPoolHandle() {
        return descriptorPoolHandle;
    }

    public void free() {
        VK13.vkDestroyDescriptorPool(device, descriptorPoolHandle, null);
    }
}
