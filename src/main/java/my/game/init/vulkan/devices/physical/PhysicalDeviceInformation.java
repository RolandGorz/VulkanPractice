package my.game.init.vulkan.devices.physical;

import org.lwjgl.vulkan.VkPhysicalDevice;

import java.util.Optional;

public record PhysicalDeviceInformation(
        VkPhysicalDevice physicalDevice, int score, Optional<Integer> graphicsQueueFamilyIndex
) implements Comparable<PhysicalDeviceInformation> {

    @Override
    public int compareTo(PhysicalDeviceInformation o) {
        return this.score - o.score;
    }
}
