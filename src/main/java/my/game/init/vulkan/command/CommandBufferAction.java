package my.game.init.vulkan.command;

import org.lwjgl.vulkan.VkCommandBuffer;

public interface CommandBufferAction {
    void run(final VkCommandBuffer commandBuffer);
}
