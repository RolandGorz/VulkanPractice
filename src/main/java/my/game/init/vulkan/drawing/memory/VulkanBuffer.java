package my.game.init.vulkan.drawing.memory;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;

import java.nio.LongBuffer;

public class VulkanBuffer {
    protected final long allocatedMemoryHandle;
    protected final int bufferSize;
    private final long vulkanBufferHandle;
    private final VkDevice device;

    public VulkanBuffer(final int bufferSize, final VkDevice device, final int bufferUsageFlags, final int memoryPropertyFlags) {
        this.device = device;
        this.bufferSize = bufferSize;
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            VkBufferCreateInfo bufferCreateInfo = VkBufferCreateInfo.calloc(memoryStack);
            bufferCreateInfo
                    .sType(VK13.VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                    .size(bufferSize)
                    .usage(bufferUsageFlags)
                    .sharingMode(VK13.VK_SHARING_MODE_EXCLUSIVE);
            LongBuffer bufferHandle = memoryStack.mallocLong(1);
            int result = VK13.vkCreateBuffer(device, bufferCreateInfo, null, bufferHandle);
            if (result != VK13.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to create vertex buffer. Error code: %d", result));
            }
            vulkanBufferHandle = bufferHandle.get(0);

            VkMemoryRequirements memoryRequirements = VkMemoryRequirements.calloc(memoryStack);
            VK13.vkGetBufferMemoryRequirements(device, vulkanBufferHandle, memoryRequirements);

            VkMemoryAllocateInfo memoryAllocateInfo = VkMemoryAllocateInfo.calloc(memoryStack);
            memoryAllocateInfo
                    .sType(VK13.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .allocationSize(memoryRequirements.size())
                    .memoryTypeIndex(findMemoryType(memoryRequirements.memoryTypeBits(),
                            memoryPropertyFlags,
                            memoryStack));
            LongBuffer allocatedMemoryHandleBuffer = memoryStack.mallocLong(1);
            //TODO It should be noted that in a real world application, you’re not supposed to actually call vkAllocateMemory
            // for every individual buffer. The maximum number of simultaneous memory allocations is limited by the maxMemoryAllocationCount
            // physical device limit, which may be as low as 4096 even on high end hardware like an NVIDIA GTX 1080.
            // The right way to allocate memory for a large number of objects at the same time is to create a custom allocator
            // that splits up a single allocation among many different objects by using the offset parameters that we’ve seen in many functions.
            // You can either implement such an allocator yourself, or use the VulkanMemoryAllocator library provided by the GPUOpen
            // initiative.
            int result2 = VK13.vkAllocateMemory(device, memoryAllocateInfo, null, allocatedMemoryHandleBuffer);
            if (result2 != VK13.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to allocate memory for buffer. Error code: %d", result2));
            }
            allocatedMemoryHandle = allocatedMemoryHandleBuffer.get(0);
            int result3 = VK13.vkBindBufferMemory(device, vulkanBufferHandle, allocatedMemoryHandle, 0);
            if (result3 != VK13.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to bind memory to buffer. Error code: %d", result3));
            }
        }
    }

    public long getVulkanBufferHandle() {
        return vulkanBufferHandle;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    private int findMemoryType(int typeFilter, int memoryPropertyFlags, MemoryStack memoryStack) {
        VkPhysicalDeviceMemoryProperties physicalDeviceMemoryProperties = VkPhysicalDeviceMemoryProperties.calloc(memoryStack);
        VK13.vkGetPhysicalDeviceMemoryProperties(device.getPhysicalDevice(), physicalDeviceMemoryProperties);
        for (int i = 0; i < physicalDeviceMemoryProperties.memoryTypeCount(); i++) {
            if ((typeFilter & (1 << i)) != 0 &&
                    (physicalDeviceMemoryProperties.memoryTypes(i).propertyFlags() & memoryPropertyFlags) == memoryPropertyFlags) {
                return i;
            }
        }
        throw new IllegalStateException(String.format("Failed to find memory type that supports typeFilter: %d and memoryPropertyFlags: %d", typeFilter, memoryPropertyFlags));
    }

    public void free() {
        VK13.vkDestroyBuffer(device, vulkanBufferHandle, null);
        VK13.vkFreeMemory(device, allocatedMemoryHandle, null);
    }
}
