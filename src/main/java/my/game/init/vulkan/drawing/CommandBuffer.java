package my.game.init.vulkan.drawing;

import my.game.init.vulkan.pipeline.GraphicsPipeline;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkClearColorValue;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;
import org.lwjgl.vulkan.VkViewport;

public class CommandBuffer {

    private final VkCommandBuffer commandBuffer;
    private final FrameBuffers frameBuffers;
    private final GraphicsPipeline graphicsPipeline;

    public CommandBuffer(CommandPool commandPool, FrameBuffers frameBuffers, GraphicsPipeline graphicsPipeline) {
        this.frameBuffers = frameBuffers;
        this.graphicsPipeline = graphicsPipeline;
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            VkCommandBufferAllocateInfo commandBufferAllocateInfo = VkCommandBufferAllocateInfo.calloc(memoryStack);
            commandBufferAllocateInfo
                    .sType(VK13.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .commandPool(commandPool.getCommandPoolPointer())
                    .level(VK13.VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    .commandBufferCount(1);
            PointerBuffer commandBuffersPointerBuffer = memoryStack.mallocPointer(1);
            int result = VK13.vkAllocateCommandBuffers(commandPool.getLogicalDevice().getLogicalDeviceInformation().vkDevice(),
                    commandBufferAllocateInfo, commandBuffersPointerBuffer);
            if (result != VK13.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to create command buffer. Error code: %d", result));
            }
            commandBuffer = new VkCommandBuffer(commandBuffersPointerBuffer.get(0),
                    commandPool.getLogicalDevice().getLogicalDeviceInformation().vkDevice());
        }
    }

    public void recordCommandBuffer(int imageIndex) {
        beginCommandBuffer();
        beginRenderPass(imageIndex);
        VK13.vkCmdBindPipeline(commandBuffer, VK13.VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline.getGraphicsPipelinePointer());
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            VkExtent2D swapChainExtent = frameBuffers
                    .getRenderPass()
                    .getSwapChainImages()
                    .getSwapChain()
                    .getSwapChainExtent();
            VkViewport.Buffer viewport = VkViewport.calloc(1, memoryStack);
            viewport
                    .x(0)
                    .y(0)
                    .width(swapChainExtent.width())
                    .height(swapChainExtent.height())
                    .minDepth(0)
                    .maxDepth(1);
            VK13.vkCmdSetViewport(commandBuffer, 0, viewport);
            VkRect2D.Buffer scissor = VkRect2D.calloc(1, memoryStack);
            scissor.offset()
                    .x(0)
                    .y(0);
            scissor.extent(swapChainExtent);
            VK13.vkCmdSetScissor(commandBuffer, 0, scissor);
        }
        VK13.vkCmdDraw(commandBuffer, 3, 1,0, 0);
        VK13.vkCmdEndRenderPass(commandBuffer);
        int result = VK13.vkEndCommandBuffer(commandBuffer);
        if (result != VK13.VK_SUCCESS) {
            throw new IllegalStateException(String.format("Failed to record command buffer. Error code: %d", result));
        }
    }

    private void beginCommandBuffer() {
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            VkCommandBufferBeginInfo commandBufferBeginInfo = VkCommandBufferBeginInfo.calloc(memoryStack);
            commandBufferBeginInfo
                    .sType(VK13.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                    .flags(0)
                    .pInheritanceInfo(null);
            int result = VK13.vkBeginCommandBuffer(commandBuffer, commandBufferBeginInfo);
            if (result != VK13.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to begin to record command buffer. Error code: %d", result));
            }
        }
    }

    private void beginRenderPass(int imageIndex) {
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            VkRect2D renderArea = VkRect2D.calloc(memoryStack);
            renderArea.offset()
                    .x(0)
                    .y(0);
            renderArea.extent(frameBuffers
                    .getRenderPass()
                    .getSwapChainImages()
                    .getSwapChain()
                    .getSwapChainExtent());

            VkClearColorValue clearColorValue = VkClearColorValue.calloc(memoryStack);
            clearColorValue
                    .float32(0, 0.0f)
                    .float32(1, 0.0f)
                    .float32(2, 0.0f)
                    .float32(3, 1.0f);
            VkClearValue.Buffer clearValue = VkClearValue.calloc(1, memoryStack);
            clearValue
                    .color(clearColorValue);
            VkRenderPassBeginInfo renderPassBeginInfo = VkRenderPassBeginInfo.calloc(memoryStack);
            renderPassBeginInfo
                    .sType(VK13.VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                    .renderPass(frameBuffers.getRenderPass().getRenderPassPointer())
                    .framebuffer(frameBuffers.getSwapChainFrameBuffers().get(imageIndex))
                    .renderArea(renderArea)
                    .pClearValues(clearValue);
            VK13.vkCmdBeginRenderPass(commandBuffer, renderPassBeginInfo, VK13.VK_SUBPASS_CONTENTS_INLINE);
        }
    }
}
