package my.game.init.vulkan.drawing.memory;

import my.game.init.vulkan.command.CommandBuffer;
import my.game.init.vulkan.command.CommandBufferFactory;
import my.game.init.vulkan.command.CommandPool;
import my.game.init.vulkan.devices.logical.LogicalDevice;
import my.game.init.vulkan.devices.logical.queue.TransferVulkanQueue;
import my.game.init.vulkan.struct.Vertex;
import my.game.init.vulkan.math.Vector2fWithSize;
import my.game.init.vulkan.math.Vector3fWithSize;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkBufferCopy;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkSubmitInfo;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

public class VertexBuffer {

    private final VulkanBuffer stagingBuffer;
    private final VulkanBuffer vertexBuffer;
    private final int bufferSize;

    public VertexBuffer(LogicalDevice logicalDevice, List<Vertex> vertices, CommandPool commandPool) {
        int size = 0;
        for (Vertex v : vertices) {
            size += v.getSize();
        }
        bufferSize = size;
        stagingBuffer = new VulkanBuffer(bufferSize, logicalDevice.vkDevice(), VK13.VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                VK13.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK13.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
        vertexBuffer = new VulkanBuffer(bufferSize, logicalDevice.vkDevice(),
                VK13.VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK13.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                VK13.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            PointerBuffer stagingData = memoryStack.callocPointer(1);
            int result4 = VK13.vkMapMemory(logicalDevice.vkDevice(), stagingBuffer.allocatedMemoryHandle, 0, bufferSize, 0, stagingData);
            if (result4 != VK13.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to map memory. Error code: %d", result4));
            }
            ByteBuffer stagingDataByteBuffer = stagingData.getByteBuffer(bufferSize);
            for (int i = 0; i < vertices.size(); ++i) {
                Vertex curr = vertices.get(i);
                Vector2fWithSize currPos = curr.pos();
                Vector3fWithSize currColor = curr.color();
                currPos.get(i * curr.getSize(), stagingDataByteBuffer);
                currColor.get(i * curr.getSize() + currPos.getSize(), stagingDataByteBuffer);
            }
            VK13.vkUnmapMemory(logicalDevice.vkDevice(), stagingBuffer.allocatedMemoryHandle);
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
                    .size(bufferSize);
            VK13.vkCmdCopyBuffer(commandBuffer.getVkCommandBuffer(), stagingBuffer.getVulkanBufferHandle(), vertexBuffer.getVulkanBufferHandle(), vkBufferCopy);
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

    public VulkanBuffer getBuffer() {
        return vertexBuffer;
    }

    public void free() {
        vertexBuffer.free();
    }
}
