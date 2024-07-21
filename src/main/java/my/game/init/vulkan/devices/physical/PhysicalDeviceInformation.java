package my.game.init.vulkan.devices.physical;

import com.google.common.collect.ImmutableSet;
import my.game.init.window.WindowSurface;
import org.immutables.value.Value;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkPhysicalDevice;

import java.nio.IntBuffer;
import java.util.Collections;
import java.util.Set;

@Value.Immutable
public abstract class PhysicalDeviceInformation implements Comparable<PhysicalDeviceInformation> {

    public abstract VkPhysicalDevice physicalDevice();

    abstract int score();

    @Value.Default
    public int graphicsQueueIndex() {
        return -1;
    }

    @Value.Default
    public int presentationQueueIndex() {
        return -1;
    }

    public abstract WindowSurface windowSurface();

    @Value.Derived
    public Set<String> supportedExtensions() {
        ImmutableSet.Builder<String> supportedExtensions = ImmutableSet.builder();
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            IntBuffer deviceExtensionPropertiesCount = memoryStack.mallocInt(1);
            int result = VK13.vkEnumerateDeviceExtensionProperties(physicalDevice(), (String) null, deviceExtensionPropertiesCount, null);
            if (result != VK13.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to enumerate device extension properties. Error code: %d5", result));
            }
            System.out.printf("Found %d device extension properties%n", deviceExtensionPropertiesCount.get(0));
            VkExtensionProperties.Buffer vkExtensionProperties = VkExtensionProperties.malloc(deviceExtensionPropertiesCount.get(0), memoryStack);
            int result2 = VK13.vkEnumerateDeviceExtensionProperties(physicalDevice(), (String) null, deviceExtensionPropertiesCount, vkExtensionProperties);
            if (result2 != VK13.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to enumerate device extension properties. Error code: %d5", result2));
            }
            for (int i = 0; i < vkExtensionProperties.capacity(); ++i) {
                System.out.printf("%s%n", vkExtensionProperties.get(i).extensionNameString());
                supportedExtensions.add(vkExtensionProperties.get(i).extensionNameString());
            }
        }
        return supportedExtensions.build();
    }

    @Value.Derived
    public boolean requiredDeviceExtensionsSupported() {
        boolean allFound = true;
        for (String x : PhysicalDeviceRetriever.REQUIRED_DEVICE_EXTENSIONS) {
            if (!supportedExtensions().contains(x)) {
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

    @Value.Derived
    public boolean isValid() {
        return score() != 0
                && graphicsQueueIndex() != -1
                && presentationQueueIndex() != -1
                && requiredDeviceExtensionsSupported()
                && swapChainAdequate();
    }

    @Value.Derived
    public Set<Integer> uniqueQueueIndexes() {
        if (isValid()) {
            return ImmutableSet.of(graphicsQueueIndex(), presentationQueueIndex());
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public int compareTo(PhysicalDeviceInformation o) {
        return this.score() - o.score();
    }

    public void free() {
        swapChainSupportDetails().free();
    }
}
