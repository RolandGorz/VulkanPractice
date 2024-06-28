package my.game.init.vulkan.devices.queue;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkQueueFamilyProperties;

import java.nio.IntBuffer;
import java.util.Optional;

public class DeviceQueueFamily {

    private static final DeviceQueueFamily INSTANCE = new DeviceQueueFamily();

    private DeviceQueueFamily() {}

    public static DeviceQueueFamily getInstance() {
        return INSTANCE;
    }

    public Optional<Integer> getGraphicsFamilyIndex(VkPhysicalDevice physicalDevice) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer queueFamilyCount = stack.mallocInt(1);
            VK13.vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCount, null);
            VkQueueFamilyProperties.Buffer queueFamilyProperties = VkQueueFamilyProperties.malloc(queueFamilyCount.get(0), stack);
            VK13.vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCount, queueFamilyProperties);
            for (int i = 0; i < queueFamilyProperties.capacity(); ++i) {
                if ((queueFamilyProperties.get(i).queueFlags() & VK13.VK_QUEUE_GRAPHICS_BIT) == VK13.VK_QUEUE_GRAPHICS_BIT) {
                    return Optional.of(i);
                }
            }
        }
        return Optional.empty();
    }
}
