package my.game.init.vulkan.drawing;

import my.game.init.vulkan.math.Vector2fWithSize;
import my.game.init.vulkan.math.Vector3fWithSize;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.List;

public class VertexBuffer {

    private final long vertexBufferHandle;

    private final long allocatedMemoryHandle;

    private final VkDevice device;

    public List<Vertex> VERTICES = List.of(
            new Vertex(new Vector2fWithSize(0.0f, -0.5f), new Vector3fWithSize(1.0f, 0.0f, 0.0f)),
            new Vertex(new Vector2fWithSize(0.5f, 0.5f), new Vector3fWithSize(0.0f, 1.0f, 0.0f)),
            new Vertex(new Vector2fWithSize(-0.5f, 0.5f), new Vector3fWithSize(0.0f, 0.0f, 1.0f))
    );

    public VertexBuffer(VkDevice device) {
        this.device = device;
        int bufferSize = 0;
        for (Vertex v : VERTICES) {
            bufferSize += v.getSize();
        }
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            VkBufferCreateInfo bufferCreateInfo = VkBufferCreateInfo.calloc(memoryStack);
            bufferCreateInfo
                    .sType(VK13.VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                    .size(bufferSize)
                    .usage(VK13.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT)
                    .sharingMode(VK13.VK_SHARING_MODE_EXCLUSIVE);
            LongBuffer bufferHandle = memoryStack.mallocLong(1);
            int result = VK13.vkCreateBuffer(device, bufferCreateInfo, null, bufferHandle);
            if (result != VK13.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to create vertex buffer. Error code: %d", result));
            }
            vertexBufferHandle = bufferHandle.get(0);

            VkMemoryRequirements memoryRequirements = VkMemoryRequirements.calloc(memoryStack);
            VK13.vkGetBufferMemoryRequirements(device, vertexBufferHandle, memoryRequirements);

            VkMemoryAllocateInfo memoryAllocateInfo = VkMemoryAllocateInfo.calloc(memoryStack);
            memoryAllocateInfo
                    .sType(VK13.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .allocationSize(memoryRequirements.size())
                    .memoryTypeIndex(findMemoryType(memoryRequirements.memoryTypeBits(),
                            VK13.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK13.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                            memoryStack));
            LongBuffer allocatedMemoryHandleBuffer = memoryStack.mallocLong(1);
            int result2 = VK13.vkAllocateMemory(device, memoryAllocateInfo, null, allocatedMemoryHandleBuffer);
            if (result2 != VK13.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to allocate memory for buffer. Error code: %d", result2));
            }
            allocatedMemoryHandle = allocatedMemoryHandleBuffer.get(0);
            int result3 = VK13.vkBindBufferMemory(device, vertexBufferHandle, allocatedMemoryHandle, 0);
            if (result3 != VK13.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to bind memory to buffer. Error code: %d", result3));
            }
            PointerBuffer data = memoryStack.callocPointer(1);
            int result4 = VK13.vkMapMemory(device, allocatedMemoryHandle, 0, bufferSize, 0, data);
            if (result4 != VK13.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to map memory. Error code: %d", result4));
            }
            ByteBuffer byteBuffer = data.getByteBuffer(bufferSize);
            for (int i = 0; i < VERTICES.size(); ++i) {
                Vertex curr = VERTICES.get(i);
                Vector2fWithSize currPos = curr.getPos();
                Vector3fWithSize currColor = curr.getColor();
                currPos.get(i * curr.getSize(), byteBuffer);
                currColor.get(i * curr.getSize() + currPos.getSize(), byteBuffer);
            }
            VK13.vkUnmapMemory(device, allocatedMemoryHandle);
        }
    }

    public long getVertexBufferHandle() {
        return vertexBufferHandle;
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
        VK13.vkDestroyBuffer(device, vertexBufferHandle, null);
        VK13.vkFreeMemory(device, allocatedMemoryHandle, null);
    }
}
