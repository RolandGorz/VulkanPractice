package my.game.init.vulkan.devices.logical.queue;

import my.game.init.vulkan.devices.physical.PhysicalDevice;
import org.lwjgl.vulkan.VkDevice;

public class PresentationQueue extends VulkanQueue{
    //VkQueue is implicitly freed when the vkDevice is freed. We do not need to free ourselves.
    public PresentationQueue(PhysicalDevice validPhysicalDevice, VkDevice vkDevice) {
        super(validPhysicalDevice.physicalDeviceInformation().queueFamilyIndexes().presentationQueueFamilyIndex(),
                vkDevice);
    }
}
