package my.game.init.vulkan.drawing;

import com.google.common.collect.ImmutableList;
import my.game.init.vulkan.pipeline.RenderPass;
import my.game.init.vulkan.swapchain.SwapChainImages;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;

import java.nio.LongBuffer;
import java.util.List;

public class FrameBuffers {

    private final List<Long> swapChainFrameBuffers;
    private final RenderPass renderPass;

    public FrameBuffers(RenderPass renderPass) {
        this.renderPass = renderPass;
        final SwapChainImages swapChainImages = renderPass.getSwapChainImages();
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            ImmutableList.Builder<Long> swapChainFrameBuffersBuilder = ImmutableList.builder();
            LongBuffer swapChainFrameBuffersBuffer = memoryStack.mallocLong(1);
            for (int i = 0; i < swapChainImages.getSwapChainImageViewPointers().size(); ++i) {
                VkFramebufferCreateInfo framebufferCreateInfo = VkFramebufferCreateInfo.calloc(memoryStack);
                framebufferCreateInfo
                        .sType(VK13.VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                        .renderPass(renderPass.getRenderPassPointer())
                        .pAttachments(swapChainImages.getSwapChainImageViewPointers().get(i))
                        .width(swapChainImages.getSwapChain().getSwapChainExtent().width())
                        .height(swapChainImages.getSwapChain().getSwapChainExtent().height())
                        .layers(1);
                int result = VK13.vkCreateFramebuffer(swapChainImages.getSwapChain().getLogicalDevice().getLogicalDeviceInformation().vkDevice(),
                        framebufferCreateInfo,
                        null,
                        swapChainFrameBuffersBuffer);
                if (result != VK13.VK_SUCCESS) {
                    throw new IllegalStateException(String.format("Failed to create framebuffer. Error code %d", result));
                }
                swapChainFrameBuffersBuilder.add(swapChainFrameBuffersBuffer.get(0));
            }
            swapChainFrameBuffers = swapChainFrameBuffersBuilder.build();
        }
    }

    public List<Long> getSwapChainFrameBuffers() {
        return swapChainFrameBuffers;
    }

    public RenderPass getRenderPass() {
        return renderPass;
    }

    public void free() {
        for (Long x : swapChainFrameBuffers) {
            VK13.vkDestroyFramebuffer(
                    renderPass.getSwapChainImages().getSwapChain().getLogicalDevice().getLogicalDeviceInformation().vkDevice(),
                    x, null);
        }
    }
}
