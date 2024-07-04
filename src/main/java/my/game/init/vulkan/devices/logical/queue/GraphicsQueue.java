package my.game.init.vulkan.devices.logical.queue;

import my.game.init.vulkan.devices.physical.ValidPhysicalDevice;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkQueue;

public class GraphicsQueue {
    private final VkQueue vkQueue;

    //VkQueue is implicitly freed when the vkDevice is freed. We do not need to free ourselves.
    public GraphicsQueue(ValidPhysicalDevice validPhysicalDevice, VkDevice vkDevice) {
        vkQueue = QueueUtil.getInstance().getQueue(
                validPhysicalDevice.physicalDeviceInformation().queueFamilyIndexes().graphicsQueueFamilyIndex(),
                vkDevice
        );
    }

    public VkQueue getVkQueue() {
        return vkQueue;
    }
}
