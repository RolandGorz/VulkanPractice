package my.game.init.vulkan.devices.queue;

import my.game.init.vulkan.devices.physical.ImmutablePhysicalDeviceInformation;
import my.game.init.window.WindowSurface;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSurface;
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

    public QueueFamilyIndexes getFamilyIndexes(VkPhysicalDevice physicalDevice, WindowSurface windowSurface) {
        Optional<Integer> graphicsFamilyIndex = Optional.empty();
        Optional<Integer> presentationFamilyIndex = Optional.empty();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer queueFamilyCount = stack.mallocInt(1);
            VK13.vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCount, null);
            VkQueueFamilyProperties.Buffer queueFamilyProperties = VkQueueFamilyProperties.malloc(queueFamilyCount.get(0), stack);
            VK13.vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCount, queueFamilyProperties);
            for (int i = 0; i < queueFamilyProperties.capacity(); ++i) {
                if (queueSupportsGraphics(queueFamilyProperties, i)) {
                    graphicsFamilyIndex = Optional.of(i);
                }
                if (queueSupportsPresentation(stack, physicalDevice, i, windowSurface)) {
                    presentationFamilyIndex = Optional.of(i);
                }
                if (graphicsFamilyIndex.isPresent() && presentationFamilyIndex.isPresent()) {
                    break;
                }
            }
        }
        return ImmutableQueueFamilyIndexes.builder()
                .graphicsQueueFamilyIndex(graphicsFamilyIndex)
                .presentationQueueFamilyIndex(presentationFamilyIndex)
                .build();
    }

    private boolean queueSupportsGraphics(VkQueueFamilyProperties.Buffer queueFamilyProperties, int index) {
        return (queueFamilyProperties.get(index).queueFlags() & VK13.VK_QUEUE_GRAPHICS_BIT) == VK13.VK_QUEUE_GRAPHICS_BIT;
    }

    private boolean queueSupportsPresentation(MemoryStack stack, VkPhysicalDevice physicalDevice, int index, WindowSurface windowSurface) {
        IntBuffer presentationSupported = stack.mallocInt(1);
        int result = KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(physicalDevice, index, windowSurface.getWindowSurfaceHandle(), presentationSupported);
        if (result != VK13.VK_SUCCESS) {
            throw new IllegalStateException(String.format("Error occurred when trying to determine Physical Device Surface Support. Error code : %d", result));
        }
        return presentationSupported.get(0) == VK13.VK_TRUE;
    }
}
