package my.game.init.vulkan.drawing;

import com.google.common.collect.ImmutableList;
import my.game.init.vulkan.pipeline.GraphicsPipeline;
import my.game.init.vulkan.pipeline.RenderPass;
import my.game.init.vulkan.swapchain.SwapChain;
import my.game.render.GraphicsRenderer;
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

import java.nio.LongBuffer;
import java.util.List;

public class CommandBuffers {

    public final List<CommandBuffer> commandBuffers;

    private final GraphicsPipeline graphicsPipeline;
    private final RenderPass renderPass;

    //Command buffers will be automatically freed when their command pool is destroyed, so we don’t need explicit cleanup.
    public CommandBuffers(CommandPool commandPool, RenderPass renderPass, GraphicsPipeline graphicsPipeline) {
        this.graphicsPipeline = graphicsPipeline;
        this.renderPass = renderPass;
        ImmutableList.Builder<CommandBuffer> commandBuffersBuilder = ImmutableList.builder();
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            VkCommandBufferAllocateInfo commandBufferAllocateInfo = VkCommandBufferAllocateInfo.calloc(memoryStack);
            commandBufferAllocateInfo
                    .sType(VK13.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .commandPool(commandPool.getCommandPoolPointer())
                    .level(VK13.VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    .commandBufferCount(GraphicsRenderer.MAX_FRAMES_IN_FLIGHT);
            PointerBuffer commandBuffersPointerBuffer = memoryStack.mallocPointer(GraphicsRenderer.MAX_FRAMES_IN_FLIGHT);
            int result = VK13.vkAllocateCommandBuffers(commandPool.getLogicalDevice().vkDevice(),
                    commandBufferAllocateInfo, commandBuffersPointerBuffer);
            if (result != VK13.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to create command buffer. Error code: %d", result));
            }
            for (int i = 0; i < commandBuffersPointerBuffer.capacity(); ++i) {
                VkCommandBuffer vkCommandBuffer = new VkCommandBuffer(commandBuffersPointerBuffer.get(i),
                        commandPool.getLogicalDevice().vkDevice());
                commandBuffersBuilder.add(new CommandBuffer(vkCommandBuffer));
            }
        }
        this.commandBuffers = commandBuffersBuilder.build();
    }

    public CommandBuffer get(int index) {
        return commandBuffers.get(index);
    }

    public class CommandBuffer {
        private final VkCommandBuffer commandBuffer;

        private CommandBuffer(VkCommandBuffer commandBuffer) {
            this.commandBuffer = commandBuffer;
        }

        public VkCommandBuffer getCommandBuffer() {
            return commandBuffer;
        }

        public void recordCommandBuffer(int imageIndex, SwapChain swapChain, FrameBuffers frameBuffers, VertexBuffer vertexBuffer) {
            beginCommandBuffer();
            beginRenderPass(imageIndex, swapChain, frameBuffers);
            VK13.vkCmdBindPipeline(commandBuffer, VK13.VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline.getGraphicsPipelinePointer());
            try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                VkExtent2D swapChainExtent = swapChain
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
                LongBuffer vertices = memoryStack.mallocLong(1);
                vertices.put(vertexBuffer.getVertexBufferHandle());
                vertices.flip();
                LongBuffer offsets = memoryStack.mallocLong(1);
                offsets.put(0);
                offsets.flip();
                VK13.vkCmdBindVertexBuffers(commandBuffer, 0, vertices, offsets);
            }
            VK13.vkCmdDraw(commandBuffer, vertexBuffer.VERTICES.size(), 1, 0, 0);
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

        private void beginRenderPass(int imageIndex, SwapChain swapChain, FrameBuffers frameBuffers) {
            try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                VkRect2D renderArea = VkRect2D.calloc(memoryStack);
                renderArea.offset()
                        .x(0)
                        .y(0);
                renderArea.extent(swapChain.getSwapChainExtent());

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
                        .renderPass(renderPass.getRenderPassPointer())
                        .framebuffer(frameBuffers.getSwapChainFrameBuffers().get(imageIndex))
                        .renderArea(renderArea)
                        .pClearValues(clearValue);
                VK13.vkCmdBeginRenderPass(commandBuffer, renderPassBeginInfo, VK13.VK_SUBPASS_CONTENTS_INLINE);
            }
        }
    }
}
