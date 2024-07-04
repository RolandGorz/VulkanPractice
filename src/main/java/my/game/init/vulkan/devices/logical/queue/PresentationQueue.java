package my.game.init.vulkan.devices.logical.queue;

import my.game.init.vulkan.devices.physical.ValidPhysicalDevice;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkQueue;

public class PresentationQueue {
    VkQueue vkQueue;

    //VkQueue is implicitly freed when the vkDevice is freed. We do not need to free ourselves.
    public PresentationQueue(ValidPhysicalDevice validPhysicalDevice, VkDevice vkDevice) {
        vkQueue = QueueUtil.getInstance().getQueue(
                validPhysicalDevice.physicalDeviceInformation().queueFamilyIndexes().presentationQueueFamilyIndex(),
                vkDevice
        );
    }

    public VkQueue getVkQueue() {
        return vkQueue;
    }
}
