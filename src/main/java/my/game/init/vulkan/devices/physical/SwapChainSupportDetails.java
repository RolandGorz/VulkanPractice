package my.game.init.vulkan.devices.physical;

import my.game.init.window.WindowSurface;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;

public class SwapChainSupportDetails {
    final VkSurfaceCapabilitiesKHR vkSurfaceCapabilitiesKHR;

    protected SwapChainSupportDetails(VkPhysicalDevice physicalDevice, WindowSurface windowSurface) {
        vkSurfaceCapabilitiesKHR = VkSurfaceCapabilitiesKHR.malloc();
        int result = KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(
                physicalDevice,
                windowSurface.getWindowSurfaceHandle(),
                vkSurfaceCapabilitiesKHR);
        if (result != VK13.VK_SUCCESS) {
            throw new IllegalStateException(String.format("Failed to get physical device surface capabilities. Error code: %d", result));
        }
    }

    protected void free() {
        vkSurfaceCapabilitiesKHR.free();
    }
}
