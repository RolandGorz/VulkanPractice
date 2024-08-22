package my.game.init.vulkan.drawing.transformation;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;
import org.lwjgl.vulkan.VkDevice;

import java.nio.LongBuffer;

public class DescriptorSetLayout {

    private long handle;
    private VkDevice device;

    public DescriptorSetLayout(VkDevice device) {
        this.device = device;
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            VkDescriptorSetLayoutBinding.Buffer uniformBufferObjectLayoutBinding = VkDescriptorSetLayoutBinding.calloc(1, memoryStack);
            uniformBufferObjectLayoutBinding.get(0)
                    .binding(0)
                    .descriptorType(VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                    .descriptorCount(1)
                    .stageFlags(VK10.VK_SHADER_STAGE_VERTEX_BIT)
                    .pImmutableSamplers(null);

            VkDescriptorSetLayoutCreateInfo descriptorSetLayoutCreateInfo = VkDescriptorSetLayoutCreateInfo.calloc(memoryStack);
            descriptorSetLayoutCreateInfo
                    .sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                    .pBindings(uniformBufferObjectLayoutBinding);

            LongBuffer descriptorSetLayoutBuffer = memoryStack.mallocLong(1);
            int result = VK10.vkCreateDescriptorSetLayout(device, descriptorSetLayoutCreateInfo, null, descriptorSetLayoutBuffer);
            if (result != VK10.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to create descriptor set layout. Error code: %d", result));
            }
            handle = descriptorSetLayoutBuffer.get(0);
        }
    }

    public long getHandle() {
        return handle;
    }

    public void free() {
        VK10.vkDestroyDescriptorSetLayout(device, handle, null);
    }
}
