package my.game.init.vulkan.devices.physical;

import com.google.common.collect.ImmutableList;
import org.lwjgl.vulkan.KHRSwapchain;

import java.util.List;

public record ValidPhysicalDevice (PhysicalDeviceInformation physicalDeviceInformation) {
    public static List<String> REQUIRED_DEVICE_EXTENSIONS = ImmutableList.of(
            KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME
    );
}
