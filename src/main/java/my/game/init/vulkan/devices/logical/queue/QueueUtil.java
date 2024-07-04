package my.game.init.vulkan.devices.logical.queue;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkQueue;

public class QueueUtil {

    private static final QueueUtil INSTANCE = new QueueUtil();

    public static QueueUtil getInstance() {
        return INSTANCE;
    }

    private QueueUtil() {}

    public VkQueue getQueue(Integer queueIndex, VkDevice vkDevice) {
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            PointerBuffer graphicsQueue = memoryStack.mallocPointer(1);
            VK13.vkGetDeviceQueue(vkDevice,
                    queueIndex, 0, graphicsQueue);
            return new VkQueue(graphicsQueue.get(0), vkDevice);
        }
    }
}
