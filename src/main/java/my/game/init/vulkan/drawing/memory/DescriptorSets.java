package my.game.init.vulkan.drawing.memory;

import my.game.init.vulkan.drawing.memory.buffer.UniformBuffer;
import my.game.init.vulkan.drawing.transformation.DescriptorSetLayout;
import my.game.render.GraphicsRenderer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

public class DescriptorSets {
    private final List<Long> descriptorSetHandles;

    public DescriptorSets(VkDevice device, DescriptorPool descriptorPool, DescriptorSetLayout descriptorSetLayout, List<UniformBuffer> uniformBuffers) {
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            VkDescriptorSetAllocateInfo descriptorSetAllocateInfo = VkDescriptorSetAllocateInfo.calloc(memoryStack);
            LongBuffer layouts = memoryStack.mallocLong(GraphicsRenderer.MAX_FRAMES_IN_FLIGHT);
            for (int i = 0; i < layouts.capacity(); ++i) {
                layouts.put(descriptorSetLayout.getHandle());
            }
            layouts.flip();
            //In our case we will create one descriptor set for each frame in flight, all with the same layout.
            // Unfortunately we do need all the copies of the layout because the next function expects an array matching the number of sets.
            descriptorSetAllocateInfo
                    .sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                    .descriptorPool(descriptorPool.getDescriptorPoolHandle())
                    .pSetLayouts(layouts);
            LongBuffer descriptorSetsBuffer = memoryStack.mallocLong(GraphicsRenderer.MAX_FRAMES_IN_FLIGHT);
            int result = VK10.vkAllocateDescriptorSets(device, descriptorSetAllocateInfo, descriptorSetsBuffer);
            if (result != VK10.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to allocate descriptor sets. Error code: %d", result));
            }
            for (int i = 0; i < GraphicsRenderer.MAX_FRAMES_IN_FLIGHT; ++i) {
                VkDescriptorBufferInfo.Buffer vkDescriptorBufferInfo = VkDescriptorBufferInfo.calloc(1, memoryStack);
                vkDescriptorBufferInfo
                        .buffer(uniformBuffers.get(i).getVulkanBuffer().getVulkanBufferHandle())
                        .offset(0)
                        .range(VK10.VK_WHOLE_SIZE);
                VkWriteDescriptorSet.Buffer vkWriteDescriptorSet = VkWriteDescriptorSet.calloc(1, memoryStack);
                vkWriteDescriptorSet
                        .sType(VK10.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                        .dstSet(descriptorSetsBuffer.get(i))
                        .dstBinding(0)
                        .dstArrayElement(0)
                        .descriptorType(VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                        .descriptorCount(1)
                        .pBufferInfo(vkDescriptorBufferInfo)
                        .pImageInfo(null)
                        .pTexelBufferView(null);
                VK10.vkUpdateDescriptorSets(device, vkWriteDescriptorSet, null);
            }
            descriptorSetHandles = new ArrayList<>();
            for (int i = 0; i < descriptorSetsBuffer.capacity(); ++i) {
                descriptorSetHandles.add(descriptorSetsBuffer.get(i));
            }
        }
    }

    public List<Long> getDescriptorSetHandles() {
        return descriptorSetHandles;
    }
}
