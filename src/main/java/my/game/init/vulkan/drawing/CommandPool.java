package my.game.init.vulkan.drawing;

import my.game.init.vulkan.devices.logical.LogicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;

import java.nio.LongBuffer;

public class CommandPool {

    private final Long commandPoolPointer;
    private final LogicalDevice logicalDevice;

    public CommandPool(LogicalDevice logicalDevice) {
        this.logicalDevice = logicalDevice;
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            VkCommandPoolCreateInfo vkCommandPoolCreateInfo = VkCommandPoolCreateInfo.calloc(memoryStack);
            vkCommandPoolCreateInfo
                    .sType(VK13.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                    .flags(VK13.VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
                    .queueFamilyIndex(logicalDevice.getLogicalDeviceInformation().graphicsQueue().getQueueIndex());
            LongBuffer commandPoolPointerBuffer = memoryStack.mallocLong(1);
            int result = VK13.vkCreateCommandPool(logicalDevice.getLogicalDeviceInformation().vkDevice(), vkCommandPoolCreateInfo, null, commandPoolPointerBuffer);
            if (result != VK13.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to create command pool. Error code: %d", result));
            }
            commandPoolPointer = commandPoolPointerBuffer.get(0);
        }
    }

    public Long getCommandPoolPointer() {
        return commandPoolPointer;
    }

    public LogicalDevice getLogicalDevice() {
        return logicalDevice;
    }

    public void free() {
        VK13.vkDestroyCommandPool(logicalDevice.getLogicalDeviceInformation().vkDevice(), commandPoolPointer, null);
    }
}
