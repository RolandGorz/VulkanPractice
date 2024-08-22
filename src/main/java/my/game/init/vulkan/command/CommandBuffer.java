package my.game.init.vulkan.command;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;

public class CommandBuffer {
    private final VkCommandBuffer vkCommandBuffer;
    private final long commandPoolHandle;

    protected CommandBuffer(VkCommandBuffer vkCommandBuffer, final long commandPoolHandle) {
        this.vkCommandBuffer = vkCommandBuffer;
        this.commandPoolHandle = commandPoolHandle;
    }

    public VkCommandBuffer getVkCommandBuffer() {
        return vkCommandBuffer;
    }

    public long getCommandPoolHandle() {
        return commandPoolHandle;
    }

    public void runCommand(final int commandBufferBeginInfoFlags, final CommandBufferAction commandBufferAction) {
        beginCommandBuffer(commandBufferBeginInfoFlags);
        commandBufferAction.run(vkCommandBuffer);
        int result = VK10.vkEndCommandBuffer(vkCommandBuffer);
        if (result != VK10.VK_SUCCESS) {
            throw new IllegalStateException(String.format("Failed to record command buffer. Error code: %d", result));
        }
    }

    private void beginCommandBuffer(final int commandBufferBeginInfoFlags) {
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            VkCommandBufferBeginInfo commandBufferBeginInfo = VkCommandBufferBeginInfo.calloc(memoryStack);
            commandBufferBeginInfo
                    .sType(VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                    .flags(commandBufferBeginInfoFlags)
                    .pInheritanceInfo(null);
            int result = VK10.vkBeginCommandBuffer(vkCommandBuffer, commandBufferBeginInfo);
            if (result != VK10.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to begin to record command buffer. Error code: %d", result));
            }
        }
    }
}
