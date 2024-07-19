package my.game.init.vulkan.devices.logical.queue;

import my.game.init.vulkan.devices.physical.PhysicalDevice;
import org.lwjgl.vulkan.VkDevice;

public class GraphicsQueue extends VulkanQueue {
    //VkQueue is implicitly freed when the vkDevice is freed. We do not need to free ourselves.
    public GraphicsQueue(PhysicalDevice validPhysicalDevice, VkDevice vkDevice) {
        super(validPhysicalDevice.physicalDeviceInformation().queueFamilyIndexes().graphicsQueueFamilyIndex(),
                vkDevice);
    }
}
