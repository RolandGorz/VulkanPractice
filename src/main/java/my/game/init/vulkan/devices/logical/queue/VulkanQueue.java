package my.game.init.vulkan.devices.logical.queue;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkQueue;

public abstract class VulkanQueue {
    private final VkQueue vkQueue;
    private final Integer queueIndex;

    //VkQueue is implicitly freed when the vkDevice is freed. We do not need to free ourselves.
    protected VulkanQueue(Integer queueIndex, VkDevice vkDevice) {
        this.queueIndex = queueIndex;
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            PointerBuffer vulkanQueue = memoryStack.mallocPointer(1);
            VK10.vkGetDeviceQueue(vkDevice,
                    queueIndex, 0, vulkanQueue);
            vkQueue = new VkQueue(vulkanQueue.get(0), vkDevice);
        }
    }

    public VkQueue getVkQueue() {
        return vkQueue;
    }

    public Integer getQueueIndex() {
        return queueIndex;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VulkanQueue) {
            return vkQueue.address() == ((VulkanQueue) obj).vkQueue.address();
        }
        return false;
    }
}
