package my.game.init.vulkan.devices.logical.queue;

import org.lwjgl.vulkan.VkDevice;

public class TransferVulkanQueue extends VulkanQueue {
    public TransferVulkanQueue(Integer queueIndex, VkDevice vkDevice) {
        super(queueIndex, vkDevice);
    }
}
