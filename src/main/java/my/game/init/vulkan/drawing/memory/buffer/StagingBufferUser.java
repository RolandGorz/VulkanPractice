package my.game.init.vulkan.drawing.memory.buffer;

import my.game.init.vulkan.command.CommandBuffer;
import my.game.init.vulkan.command.CommandBufferFactory;
import my.game.init.vulkan.command.CommandPool;
import my.game.init.vulkan.devices.logical.LogicalDevice;
import my.game.init.vulkan.devices.logical.queue.TransferVulkanQueue;
import my.game.init.vulkan.struct.Struct;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkBufferCopy;
import org.lwjgl.vulkan.VkSubmitInfo;

import java.util.Collections;
import java.util.List;

public class StagingBufferUser {
    protected final VulkanBuffer stagingBuffer;
    protected final VulkanBuffer destinationBuffer;
    private final int structEntriesCount;

    protected StagingBufferUser(final List<? extends Struct> structList, final LogicalDevice logicalDevice,
                                final int destinationBufferUsageFlags, CommandPool commandPool, VulkanBuffer.MemoryMapActon memoryMapActon) {
        int size = 0;
        for (Struct struct : structList) {
            size += struct.getSize();
        }
        structEntriesCount = structList.size();
        stagingBuffer = new VulkanBuffer(size, logicalDevice.vkDevice(), VK13.VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                VK13.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK13.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
        destinationBuffer = new VulkanBuffer(size, logicalDevice.vkDevice(),
                destinationBufferUsageFlags,
                VK13.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            stagingBuffer.mapMemoryWithAction(memoryStack, memoryMapActon);
            copyBuffer(commandPool, memoryStack, logicalDevice.transferVulkanQueue());
            stagingBuffer.free();
        }
    }

    //TODO You may wish to create a separate command pool for these kinds of short-lived buffers,
    // because the implementation may be able to apply memory allocation optimizations.
    // You should use the VK_COMMAND_POOL_CREATE_TRANSIENT_BIT flag during command pool generation in that case.
    private void copyBuffer(CommandPool commandPool, MemoryStack memoryStack, TransferVulkanQueue transferVulkanQueue) {
        CommandBuffer commandBuffer = CommandBufferFactory.createCommandBuffers(commandPool, 1).getFirst();
        commandBuffer.runCommand(VK13.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT,
                (vkCommandBuffer) -> {
                    VkBufferCopy.Buffer vkBufferCopy = VkBufferCopy.calloc(1, memoryStack);
                    vkBufferCopy
                            .srcOffset(0)
                            .dstOffset(0)
                            .size(stagingBuffer.bufferSize);
                    VK13.vkCmdCopyBuffer(commandBuffer.getVkCommandBuffer(), stagingBuffer.getVulkanBufferHandle(), destinationBuffer.getVulkanBufferHandle(), vkBufferCopy);
                });
        PointerBuffer commandBuffersPointer = memoryStack.mallocPointer(1);
        commandBuffersPointer.put(commandBuffer.getVkCommandBuffer());
        commandBuffersPointer.flip();
        VkSubmitInfo vkSubmitInfo = VkSubmitInfo.calloc(memoryStack);
        vkSubmitInfo
                .sType(VK13.VK_STRUCTURE_TYPE_SUBMIT_INFO)
                .pCommandBuffers(commandBuffersPointer);

        int result = VK13.vkQueueSubmit(transferVulkanQueue.getVkQueue(), vkSubmitInfo, VK13.VK_NULL_HANDLE);
        if (result != VK13.VK_SUCCESS) {
            throw new IllegalStateException(String.format("Failed to submit draw command buffer. Error code: %d", result));
        }
        VK13.vkQueueWaitIdle(transferVulkanQueue.getVkQueue());
        CommandBufferFactory.freeCommandBuffers(Collections.singletonList(commandBuffer), memoryStack);
    }

    //The number of vertices in what we are trying to draw basically. When using an index buffer it's the number of indexes
    // and when using a vertex buffer it's the number vertices.
    public int getStructEntriesCount() {
        return structEntriesCount;
    }

    public VulkanBuffer getDestinationBuffer() {
        return destinationBuffer;
    }

    public void free() {
        destinationBuffer.free();
    }
}
