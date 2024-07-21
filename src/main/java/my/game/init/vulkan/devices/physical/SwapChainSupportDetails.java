package my.game.init.vulkan.devices.physical;

import my.game.init.window.WindowSurface;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;

import java.nio.IntBuffer;

public class SwapChainSupportDetails {
    private final VkSurfaceCapabilitiesKHR capabilities;
    private final VkSurfaceFormatKHR.Buffer formats;
    private final IntBuffer presentModes;
    private final VkPhysicalDevice physicalDevice;
    private final WindowSurface windowSurface;

    protected SwapChainSupportDetails(VkPhysicalDevice physicalDevice, WindowSurface windowSurface) {
        this.physicalDevice = physicalDevice;
        this.windowSurface = windowSurface;
        capabilities = VkSurfaceCapabilitiesKHR.malloc();
        getPhysicalDeviceSurfaceCapabilities(physicalDevice, windowSurface, capabilities);
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            IntBuffer formatsCount = memoryStack.mallocInt(1);
            getFormatsCount(physicalDevice, windowSurface, formatsCount);
            formats = VkSurfaceFormatKHR.malloc(formatsCount.get(0));
            getPhysicalDeviceSurfaceFormats(physicalDevice, windowSurface, formatsCount, formats);
            IntBuffer presentModesCount = memoryStack.mallocInt(1);
            getPresentModesCount(physicalDevice, windowSurface, presentModesCount);
            presentModes = MemoryUtil.memAllocInt(presentModesCount.get(0));
            getPresentModes(physicalDevice, windowSurface, presentModesCount);
        }
    }

    public VkSurfaceCapabilitiesKHR capabilities() {
        //When the window resizes the height and width can change. Should query the capabilities fresh each time.
        getPhysicalDeviceSurfaceCapabilities(physicalDevice, windowSurface, capabilities);
        return capabilities;
    }

    public VkSurfaceFormatKHR.Buffer formats() {
        return formats;
    }

    public IntBuffer presentModes() {
        return presentModes;
    }

    public void getPhysicalDeviceSurfaceCapabilities(VkPhysicalDevice physicalDevice, WindowSurface windowSurface, VkSurfaceCapabilitiesKHR capabilities) {
        int result = KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(
                physicalDevice,
                windowSurface.getWindowSurfaceHandle(),
                capabilities);
        if (result != VK13.VK_SUCCESS) {
            throw new IllegalStateException(String.format("Failed to get physical device surface capabilities. Error code: %d", result));
        }
    }

    private void getFormatsCount(VkPhysicalDevice physicalDevice, WindowSurface windowSurface, IntBuffer formatsCount) {
        int result = KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(
                physicalDevice,
                windowSurface.getWindowSurfaceHandle(),
                formatsCount,
                null);
        if (result != VK13.VK_SUCCESS) {
            throw new IllegalStateException(String.format("Failed to get physical device surface formats count. Error code: %d", result));
        }
    }

    private void getPhysicalDeviceSurfaceFormats(VkPhysicalDevice physicalDevice,
                                                 WindowSurface windowSurface,
                                                 IntBuffer formatsCount,
                                                 VkSurfaceFormatKHR.Buffer formatsBuffer) {
        int result = KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(
                physicalDevice,
                windowSurface.getWindowSurfaceHandle(),
                formatsCount,
                formatsBuffer);
        if (result != VK13.VK_SUCCESS) {
            throw new IllegalStateException(String.format("Failed to get physical device surface formats. Error code: %d", result));
        }
    }

    private void getPresentModesCount(VkPhysicalDevice physicalDevice, WindowSurface windowSurface, IntBuffer presentModesCount) {
        int result = KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(
                physicalDevice,
                windowSurface.getWindowSurfaceHandle(),
                presentModesCount,
                null);
        if (result != VK13.VK_SUCCESS) {
            throw new IllegalStateException(String.format("Failed to get physical device surface present modes count. Error code: %d", result));
        }
    }

    private void getPresentModes(VkPhysicalDevice physicalDevice, WindowSurface windowSurface, IntBuffer presentModesCount) {
        int result = KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(
                physicalDevice,
                windowSurface.getWindowSurfaceHandle(),
                presentModesCount,
                presentModes);
        if (result != VK13.VK_SUCCESS) {
            throw new IllegalStateException(String.format("Failed to get physical device surface present modes. Error code: %d", result));
        }
    }

    protected void free() {
        capabilities.free();
        formats.free();
        MemoryUtil.memFree(presentModes);
    }
}
