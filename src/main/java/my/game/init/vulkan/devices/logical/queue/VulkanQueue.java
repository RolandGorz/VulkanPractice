package my.game.init.vulkan.devices.logical.queue;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkQueue;

public abstract class VulkanQueue {
    private final VkQueue vkQueue;

    //VkQueue is implicitly freed when the vkDevice is freed. We do not need to free ourselves.
    protected VulkanQueue(Integer queueIndex, VkDevice vkDevice) {
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            PointerBuffer graphicsQueue = memoryStack.mallocPointer(1);
            VK13.vkGetDeviceQueue(vkDevice,
                    queueIndex, 0, graphicsQueue);
            vkQueue =  new VkQueue(graphicsQueue.get(0), vkDevice);
        }
    }

    public VkQueue getVkQueue() {
        return vkQueue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VulkanQueue) {
            return vkQueue.address() == ((VulkanQueue)obj).vkQueue.address();
        }
        return false;
    }
}
