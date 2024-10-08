package my.game.init.vulkan.drawing.memory.buffer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;

import java.nio.ByteBuffer;
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
                    .sType(VK10.VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                    .size(bufferSize)
                    .usage(bufferUsageFlags)
                    .sharingMode(VK10.VK_SHARING_MODE_EXCLUSIVE);
            LongBuffer bufferHandle = memoryStack.mallocLong(1);
            int result = VK10.vkCreateBuffer(device, bufferCreateInfo, null, bufferHandle);
            if (result != VK10.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to create vertex buffer. Error code: %d", result));
            }
            vulkanBufferHandle = bufferHandle.get(0);

            VkMemoryRequirements memoryRequirements = VkMemoryRequirements.calloc(memoryStack);
            VK10.vkGetBufferMemoryRequirements(device, vulkanBufferHandle, memoryRequirements);

            VkMemoryAllocateInfo memoryAllocateInfo = VkMemoryAllocateInfo.calloc(memoryStack);
            memoryAllocateInfo
                    .sType(VK10.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
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
            int result2 = VK10.vkAllocateMemory(device, memoryAllocateInfo, null, allocatedMemoryHandleBuffer);
            if (result2 != VK10.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to allocate memory for buffer. Error code: %d", result2));
            }
            allocatedMemoryHandle = allocatedMemoryHandleBuffer.get(0);
            int result3 = VK10.vkBindBufferMemory(device, vulkanBufferHandle, allocatedMemoryHandle, 0);
            if (result3 != VK10.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to bind memory to buffer. Error code: %d", result3));
            }
        }
    }

    public long getVulkanBufferHandle() {
        return vulkanBufferHandle;
    }

    private int findMemoryType(int typeFilter, int memoryPropertyFlags, MemoryStack memoryStack) {
        VkPhysicalDeviceMemoryProperties physicalDeviceMemoryProperties = VkPhysicalDeviceMemoryProperties.calloc(memoryStack);
        VK10.vkGetPhysicalDeviceMemoryProperties(device.getPhysicalDevice(), physicalDeviceMemoryProperties);
        for (int i = 0; i < physicalDeviceMemoryProperties.memoryTypeCount(); i++) {
            if ((typeFilter & (1 << i)) != 0 &&
                    (physicalDeviceMemoryProperties.memoryTypes(i).propertyFlags() & memoryPropertyFlags) == memoryPropertyFlags) {
                return i;
            }
        }
        throw new IllegalStateException(String.format("Failed to find memory type that supports typeFilter: %d and memoryPropertyFlags: %d", typeFilter, memoryPropertyFlags));
    }

    protected void mapMemoryWithAction(MemoryStack memoryStack, MemoryMapActon memoryMapActon) {
        PointerBuffer data = memoryStack.callocPointer(1);
        int result = VK10.vkMapMemory(device, allocatedMemoryHandle, 0, bufferSize, 0, data);
        if (result != VK10.VK_SUCCESS) {
            throw new IllegalStateException(String.format("Failed to map memory. Error code: %d", result));
        }
        ByteBuffer dataByteBuffer = data.getByteBuffer(bufferSize);
        memoryMapActon.mapMemory(dataByteBuffer);
        VK10.vkUnmapMemory(device, allocatedMemoryHandle);
    }

    //TODO make this so it can only be called once. Possibly split VulkanBuffer between staging and persistent
    protected PointerBuffer persistentMemoryMap() {
        PointerBuffer persistentMappedMemory = MemoryUtil.memAllocPointer(1);
        int result = VK10.vkMapMemory(device, allocatedMemoryHandle, 0, bufferSize, 0, persistentMappedMemory);
        if (result != VK10.VK_SUCCESS) {
            throw new IllegalStateException(String.format("Failed to map memory. Error code: %d", result));
        }
        return persistentMappedMemory;
    }

    public void free() {
        VK10.vkDestroyBuffer(device, vulkanBufferHandle, null);
        VK10.vkFreeMemory(device, allocatedMemoryHandle, null);
    }

    protected interface MemoryMapActon {
        void mapMemory(ByteBuffer stagingDataByteBuffer);
    }
}
