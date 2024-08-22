package my.game.init.vulkan.command;

import com.google.common.collect.ImmutableList;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkDevice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public static void freeCommandBuffers(List<CommandBuffer> commandBuffers, MemoryStack memoryStack) {
        Map<VkDevice, Map<Long, List<VkCommandBuffer>>> map = new HashMap<>();
        for (CommandBuffer c : commandBuffers) {
            map.compute(c.getVkCommandBuffer().getDevice(), (k, v) -> {
                if (v == null) {
                    Map<Long, List<VkCommandBuffer>> insert = new HashMap<>();
                    List<VkCommandBuffer> vkCommandBuffers = new ArrayList<>();
                    vkCommandBuffers.add(c.getVkCommandBuffer());
                    insert.put(c.getCommandPoolHandle(), vkCommandBuffers);
                    return insert;
                } else {
                    List<VkCommandBuffer> vkCommandBuffers = v.getOrDefault(c.getCommandPoolHandle(), new ArrayList<>());
                    vkCommandBuffers.add(c.getVkCommandBuffer());
                    v.put(c.getCommandPoolHandle(), vkCommandBuffers);
                    return v;
                }
            });
        }
        //For each unique device and each unique command pool in the list of command buffers we free them as a group.
        //We could assume that the command buffers passed to us all belong to one device and command pool but that's unsafe to assume.
        for (Map.Entry<VkDevice, Map<Long, List<VkCommandBuffer>>> deviceBucket : map.entrySet()) {
            for (Map.Entry<Long, List<VkCommandBuffer>> commandPoolBucket : deviceBucket.getValue().entrySet()) {
                //TODO shouldn't use memory stack here. Could run out of stack
                PointerBuffer pointerBuffer = memoryStack.callocPointer(commandPoolBucket.getValue().size());
                for (VkCommandBuffer vkCommandBuffer : commandPoolBucket.getValue()) {
                    pointerBuffer.put(vkCommandBuffer);
                }
                pointerBuffer.flip();
                VK10.vkFreeCommandBuffers(deviceBucket.getKey(), commandPoolBucket.getKey(), pointerBuffer);
            }
        }
    }
}
