package my.game.init.vulkan.command;

import my.game.init.vulkan.devices.logical.queue.VulkanQueue;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import org.lwjgl.vulkan.VkDevice;

import java.nio.LongBuffer;

public class CommandPool {

    private final Long commandPoolHandle;
    private final VkDevice vkDevice;

    public CommandPool(VkDevice vkDevice, VulkanQueue vulkanQueue) {
        this.vkDevice = vkDevice;
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            VkCommandPoolCreateInfo vkCommandPoolCreateInfo = VkCommandPoolCreateInfo.calloc(memoryStack);
            vkCommandPoolCreateInfo
                    .sType(VK13.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                    .flags(VK13.VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
                    .queueFamilyIndex(vulkanQueue.getQueueIndex());
            LongBuffer commandPoolPointerBuffer = memoryStack.mallocLong(1);
            int result = VK13.vkCreateCommandPool(vkDevice, vkCommandPoolCreateInfo, null, commandPoolPointerBuffer);
            if (result != VK13.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to create command pool. Error code: %d", result));
            }
            commandPoolHandle = commandPoolPointerBuffer.get(0);
        }
    }

    public Long getCommandPoolHandle() {
        return commandPoolHandle;
    }

    public VkDevice getVkDevice() {
        return vkDevice;
    }

    public void free() {
        VK13.vkDestroyCommandPool(vkDevice, commandPoolHandle, null);
    }
}
