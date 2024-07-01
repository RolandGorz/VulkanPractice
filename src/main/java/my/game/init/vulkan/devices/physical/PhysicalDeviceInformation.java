package my.game.init.vulkan.devices.physical;

import my.game.init.vulkan.devices.queue.QueueFamilyIndexes;
import org.immutables.value.Value;
import org.lwjgl.vulkan.VkPhysicalDevice;

import java.util.Optional;

@Value.Immutable
public abstract class PhysicalDeviceInformation implements Comparable<PhysicalDeviceInformation> {

    public abstract VkPhysicalDevice getVkPhysicalDevice();
    public abstract int getScore();
    public abstract QueueFamilyIndexes getQueueFamilyIndexes();
    @Override
    public int compareTo(PhysicalDeviceInformation o) {
        return this.getScore() - o.getScore();
    }
}
