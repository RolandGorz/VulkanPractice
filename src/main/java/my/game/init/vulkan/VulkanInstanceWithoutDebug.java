package my.game.init.vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkInstanceCreateInfo;

public class VulkanInstanceWithoutDebug extends VulkanInstance {
    public VulkanInstanceWithoutDebug() {
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            VkInstanceCreateInfo createInfo = super.createCreateInfo(memoryStack);
            super.createVulkanInstance(memoryStack, createInfo);
        }
    }
}
