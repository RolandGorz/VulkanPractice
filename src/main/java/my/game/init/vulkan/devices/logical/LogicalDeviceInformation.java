package my.game.init.vulkan.devices.logical;

import my.game.init.vulkan.devices.logical.queue.GraphicsQueue;
import my.game.init.vulkan.devices.logical.queue.PresentationQueue;
import my.game.init.vulkan.devices.physical.PhysicalDevice;
import org.immutables.value.Value;
import org.lwjgl.vulkan.VkDevice;

@Value.Immutable
public abstract class LogicalDeviceInformation {
    public abstract PhysicalDevice validPhysicalDevice();
    public abstract VkDevice vkDevice();
    @Value.Derived
    public GraphicsQueue graphicsQueue() {
        return new GraphicsQueue(validPhysicalDevice(), vkDevice());
    }
    @Value.Derived
    public PresentationQueue presentationQueue() {
        return new PresentationQueue(validPhysicalDevice(), vkDevice());
    }
}
