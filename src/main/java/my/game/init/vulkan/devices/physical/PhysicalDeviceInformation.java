package my.game.init.vulkan.devices.physical;

import my.game.init.window.WindowSurface;
import org.immutables.value.Value;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkPhysicalDevice;

import java.nio.IntBuffer;
import java.util.HashSet;

@Value.Immutable
public abstract class PhysicalDeviceInformation implements Comparable<PhysicalDeviceInformation> {

    public abstract VkPhysicalDevice physicalDevice();
    public abstract int score();
    public abstract QueueFamilyIndexes queueFamilyIndexes();
    public abstract WindowSurface windowSurface();
    @Value.Derived
    public boolean requiredDeviceExtensionsSupported() {
        HashSet<String> supportedExtensions = new HashSet<>();
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            IntBuffer deviceExtensionPropertiesCount = memoryStack.mallocInt(1);
            int result = VK13.vkEnumerateDeviceExtensionProperties(physicalDevice(), (String)null, deviceExtensionPropertiesCount, null);
            if (result != VK13.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to enumerate device extension properties. Error code: %d5", result));
            }
            System.out.printf("Found %d device extension properties%n", deviceExtensionPropertiesCount.get(0));
            VkExtensionProperties.Buffer vkExtensionProperties = VkExtensionProperties.malloc(deviceExtensionPropertiesCount.get(0), memoryStack);
            int result2 = VK13.vkEnumerateDeviceExtensionProperties(physicalDevice(), (String)null, deviceExtensionPropertiesCount, vkExtensionProperties);
            if (result2 != VK13.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to enumerate device extension properties. Error code: %d5", result2));
            }
            for (int i = 0; i < vkExtensionProperties.capacity(); ++i) {
                System.out.printf("%s%n", vkExtensionProperties.get(i).extensionNameString());
                supportedExtensions.add(vkExtensionProperties.get(i).extensionNameString());
            }
        }
        boolean allFound = true;
        for (String x : ValidPhysicalDevice.REQUIRED_DEVICE_EXTENSIONS) {
            if (!supportedExtensions.contains(x)) {
                System.out.printf("Device extension %s not found%n", x);
                allFound = false;
            }
        }
        return allFound;
    }
    @Value.Derived
    public SwapChainSupportDetails swapChainSupportDetails() {
        return new SwapChainSupportDetails(physicalDevice(), windowSurface());
    }

    @Value.Derived
    protected boolean swapChainAdequate() {
        return swapChainSupportDetails().formats().capacity() > 0 && swapChainSupportDetails().presentModes().capacity() > 0;
    }

    public boolean isValid() {
        return score() != 0 && queueFamilyIndexes().isComplete() && requiredDeviceExtensionsSupported() && swapChainAdequate();
    }

    @Override
    public int compareTo(PhysicalDeviceInformation o) {
        return this.score() - o.score();
    }

    public void free() {
        swapChainSupportDetails().free();
    }
}
