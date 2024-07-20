package my.game.init.vulkan.devices.logical.queue;

import org.lwjgl.vulkan.VkDevice;

public class GraphicsQueue extends VulkanQueue {
    //VkQueue is implicitly freed when the vkDevice is freed. We do not need to free ourselves.
    public GraphicsQueue(final Integer index, final VkDevice vkDevice) {
        super(index, vkDevice);
    }
}
