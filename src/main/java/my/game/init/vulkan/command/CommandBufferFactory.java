package my.game.init.vulkan.command;

import com.google.common.collect.ImmutableList;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkDevice;

import java.util.List;

public class CommandBufferFactory {

    private CommandBufferFactory() {}

    //Command buffers will be automatically freed when their command pool is destroyed, so we don’t need explicit cleanup.
    public static List<CommandBuffer> createCommandBuffers(CommandPool commandPool, int commandBufferCount) {
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            ImmutableList.Builder<CommandBuffer> commandBuffersBuilder = ImmutableList.builder();
            VkCommandBufferAllocateInfo commandBufferAllocateInfo = VkCommandBufferAllocateInfo.calloc(memoryStack);
            commandBufferAllocateInfo
                    .sType(VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .commandPool(commandPool.getCommandPoolHandle())
                    .level(VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    .commandBufferCount(commandBufferCount);
            PointerBuffer commandBuffersPointerBuffer = memoryStack.mallocPointer(commandBufferCount);
            int result = VK10.vkAllocateCommandBuffers(commandPool.getVkDevice(),
                    commandBufferAllocateInfo, commandBuffersPointerBuffer);
            if (result != VK10.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to create command buffer. Error code: %d", result));
            }
            for (int i = 0; i < commandBuffersPointerBuffer.capacity(); ++i) {
                VkCommandBuffer vkCommandBuffer = new VkCommandBuffer(commandBuffersPointerBuffer.get(i),
                        commandPool.getVkDevice());
                commandBuffersBuilder.add(new CommandBuffer(vkCommandBuffer, commandPool.getCommandPoolHandle()));
            }
            return commandBuffersBuilder.build();
        }
    }

    //Command buffers will be automatically freed when their command pool is destroyed, use this when you want to free earlier than that.
    public static void freeCommandBuffers(List<CommandBuffer> commandBuffers, MemoryStack memoryStack, VkDevice vkDevice, Long commandPool) {
            PointerBuffer pointerBuffer = memoryStack.callocPointer(commandBuffers.size());
            for (CommandBuffer commandBuffer : commandBuffers) {
                pointerBuffer.put(commandBuffer.getVkCommandBuffer());
            }
            pointerBuffer.flip();
            VK10.vkFreeCommandBuffers(vkDevice, commandPool, pointerBuffer);
    }
}
