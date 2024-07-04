package my.game.init.vulkan.devices.physical;

import org.immutables.value.Value;
import org.lwjgl.vulkan.VkPhysicalDevice;

@Value.Immutable
public abstract class PhysicalDeviceInformation implements Comparable<PhysicalDeviceInformation> {

    public abstract VkPhysicalDevice physicalDevice();
    public abstract int score();
    public abstract QueueFamilyIndexes queueFamilyIndexes();
    @Override
    public int compareTo(PhysicalDeviceInformation o) {
        return this.score() - o.score();
    }
}
