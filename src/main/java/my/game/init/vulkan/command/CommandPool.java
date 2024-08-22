package my.game.init.vulkan.command;

import my.game.init.vulkan.devices.logical.queue.VulkanQueue;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
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
                    .sType(VK10.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                    .flags(VK10.VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
                    .queueFamilyIndex(vulkanQueue.getQueueIndex());
            LongBuffer commandPoolPointerBuffer = memoryStack.mallocLong(1);
            int result = VK10.vkCreateCommandPool(vkDevice, vkCommandPoolCreateInfo, null, commandPoolPointerBuffer);
            if (result != VK10.VK_SUCCESS) {
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
        VK10.vkDestroyCommandPool(vkDevice, commandPoolHandle, null);
    }
}
