package my.game.init.vulkan.devices.queue;

import my.game.init.vulkan.devices.logical.LogicalDeviceInformation;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkQueue;

public class GraphicsQueue {
    VkQueue vkQueue;

    //VkQueue is implicitly freed when the vkDevice is freed. We do not need to free ourselves.
    public GraphicsQueue(LogicalDeviceInformation logicalDeviceInformation) {
        if (logicalDeviceInformation.physicalDeviceInformation().graphicsQueueFamilyIndex().isEmpty()) {
            throw new RuntimeException("No graphics queue family found. Cannot create graphics queue");
        }
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            PointerBuffer graphicsQueue = memoryStack.mallocPointer(1);
            VK13.vkGetDeviceQueue(logicalDeviceInformation.vkDevice(),
                    logicalDeviceInformation.physicalDeviceInformation().graphicsQueueFamilyIndex().get(), 0, graphicsQueue);
            vkQueue = new VkQueue(graphicsQueue.get(0), logicalDeviceInformation.vkDevice());
        }
    }
}
