package my.game.render;

import com.google.common.collect.ImmutableList;
import my.game.init.vulkan.VulkanUtil;
import my.game.init.vulkan.command.CommandBuffer;
import my.game.init.vulkan.command.CommandBufferFactory;
import my.game.init.vulkan.command.CommandPool;
import my.game.init.vulkan.devices.logical.LogicalDevice;
import my.game.init.vulkan.devices.physical.PhysicalDeviceInformation;
import my.game.init.vulkan.drawing.FrameBuffers;
import my.game.init.vulkan.drawing.memory.DescriptorPool;
import my.game.init.vulkan.drawing.memory.DescriptorSets;
import my.game.init.vulkan.drawing.memory.buffer.IndexBuffer;
import my.game.init.vulkan.drawing.memory.buffer.UniformBuffer;
import my.game.init.vulkan.drawing.memory.buffer.VertexBuffer;
import my.game.init.vulkan.drawing.transformation.DescriptorSetLayout;
import my.game.init.vulkan.math.Vector2fWithSize;
import my.game.init.vulkan.math.Vector3fWithSize;
import my.game.init.vulkan.pipeline.GraphicsPipeline;
import my.game.init.vulkan.pipeline.RenderPass;
import my.game.init.vulkan.struct.Index;
import my.game.init.vulkan.struct.Vertex;
import my.game.init.vulkan.swapchain.SwapChain;
import my.game.init.vulkan.swapchain.SwapChainImages;
import my.game.init.window.WindowHandle;
import my.game.init.window.WindowSurface;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkClearColorValue;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkFenceCreateInfo;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkSubmitInfo;
import org.lwjgl.vulkan.VkViewport;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

public class GraphicsRenderer {

    public static final int MAX_FRAMES_IN_FLIGHT = 2;
    private final LogicalDevice logicalDevice;
    private final List<CommandBuffer> graphicsCommandBuffers;
    private RenderPass renderPass;
    private final PhysicalDeviceInformation physicalDeviceInformation;
    private final WindowHandle windowHandle;
    private final WindowSurface windowSurface;
    private GraphicsPipeline graphicsPipeline;
    private final VertexBuffer vertexBuffer;
    private final IndexBuffer indexBuffer;
    private final List<LongBuffer> imageAvailableSemaphores;
    private final List<LongBuffer> renderFinishedSemaphores;
    private final List<LongBuffer> inFlightFences;
    private final DescriptorSetLayout descriptorSetLayout;
    private final List<UniformBuffer> uniformBuffers;
    private final DescriptorPool descriptorPool;
    private final DescriptorSets descriptorSets;
    private int currentFrame = 0;
    private SwapChain swapChain;
    private SwapChainImages swapChainImages;
    private FrameBuffers frameBuffers;

    public GraphicsRenderer(LogicalDevice logicalDevice, CommandPool graphicsCommandPool,
                            CommandPool transferCommandPool,
                            PhysicalDeviceInformation physicalDeviceInformation,
                            WindowHandle windowHandle, WindowSurface windowSurface) {
        this.logicalDevice = logicalDevice;
        this.physicalDeviceInformation = physicalDeviceInformation;
        this.windowHandle = windowHandle;
        this.windowSurface = windowSurface;
        this.swapChain = new SwapChain(
                logicalDevice.vkDevice(),
                physicalDeviceInformation,
                windowHandle,
                windowSurface,
                VK10.VK_NULL_HANDLE);
        this.swapChainImages = createImageViews(logicalDevice, swapChain);
        this.renderPass = new RenderPass(logicalDevice.vkDevice(), swapChain);
        this.descriptorSetLayout = new DescriptorSetLayout(logicalDevice.vkDevice());
        this.graphicsPipeline = new GraphicsPipeline(logicalDevice.vkDevice(), renderPass, descriptorSetLayout);

        List<Vertex> vertexList = List.of(
                new Vertex(new Vector2fWithSize(-0.5f, -0.5f), new Vector3fWithSize(1.0f, 0.0f, 0.0f)),
                new Vertex(new Vector2fWithSize(0.5f, -0.5f), new Vector3fWithSize(0.0f, 1.0f, 0.0f)),
                new Vertex(new Vector2fWithSize(0.5f, 0.5f), new Vector3fWithSize(0.0f, 0.0f, 1.0f)),
                new Vertex(new Vector2fWithSize(-0.5f, 0.5f), new Vector3fWithSize(1.0f, 1.0f, 1.0f))
        );

        List<Index> indexes = List.of(
                new Index((short) 0),
                new Index((short) 1),
                new Index((short) 2),
                new Index((short) 2),
                new Index((short) 3),
                new Index((short) 0)
        );
        this.vertexBuffer = new VertexBuffer(logicalDevice, vertexList, transferCommandPool);
        this.indexBuffer = new IndexBuffer(logicalDevice, indexes, transferCommandPool);
        this.uniformBuffers = new ArrayList<>();
        for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; ++i) {
            uniformBuffers.add(new UniformBuffer(logicalDevice.vkDevice()));
        }
        descriptorPool = new DescriptorPool(logicalDevice.vkDevice());
        descriptorSets = new DescriptorSets(logicalDevice.vkDevice(), descriptorPool, descriptorSetLayout, uniformBuffers);
        this.graphicsCommandBuffers = CommandBufferFactory.createCommandBuffers(graphicsCommandPool, MAX_FRAMES_IN_FLIGHT);
        this.frameBuffers = createFrameBuffers(logicalDevice, renderPass, swapChainImages, swapChain);
        ImmutableList.Builder<LongBuffer> imageAvailableSemaphoresBuilder = ImmutableList.builder();
        ImmutableList.Builder<LongBuffer> renderFinishedSemaphoresBuilder = ImmutableList.builder();
        ImmutableList.Builder<LongBuffer> inFlightFencesBuilder = ImmutableList.builder();
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.calloc(memoryStack);
            semaphoreCreateInfo
                    .sType(VK10.VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
            VkFenceCreateInfo fenceCreateInfo = VkFenceCreateInfo.calloc(memoryStack);
            fenceCreateInfo
                    .sType(VK10.VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                    .flags(VK10.VK_FENCE_CREATE_SIGNALED_BIT);
            for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; ++i) {
                LongBuffer imageAvailableSemaphore = MemoryUtil.memAllocLong(1);
                int result = VK10.vkCreateSemaphore(logicalDevice.vkDevice(), semaphoreCreateInfo, null, imageAvailableSemaphore);
                if (result != VK10.VK_SUCCESS) {
                    throw new IllegalStateException(String.format("Failed to create semaphore. Error code: %d", result));
                }
                LongBuffer renderFinishedSemaphore = MemoryUtil.memAllocLong(1);
                int result2 = VK10.vkCreateSemaphore(logicalDevice.vkDevice(), semaphoreCreateInfo, null, renderFinishedSemaphore);
                if (result2 != VK10.VK_SUCCESS) {
                    throw new IllegalStateException(String.format("Failed to create semaphore. Error code: %d", result2));
                }
                LongBuffer inFlightFence = MemoryUtil.memAllocLong(1);
                int result3 = VK10.vkCreateFence(logicalDevice.vkDevice(), fenceCreateInfo, null, inFlightFence);
                if (result3 != VK10.VK_SUCCESS) {
                    throw new IllegalStateException(String.format("Failed to create fence. Error code: %d", result3));
                }
                imageAvailableSemaphoresBuilder.add(imageAvailableSemaphore);
                renderFinishedSemaphoresBuilder.add(renderFinishedSemaphore);
                inFlightFencesBuilder.add(inFlightFence);
            }
            imageAvailableSemaphores = imageAvailableSemaphoresBuilder.build();
            renderFinishedSemaphores = renderFinishedSemaphoresBuilder.build();
            inFlightFences = inFlightFencesBuilder.build();
        }
    }

    private SwapChainImages createImageViews(LogicalDevice logicalDevice, SwapChain swapChain) {
        return new SwapChainImages(logicalDevice.vkDevice(), swapChain);
    }

    private FrameBuffers createFrameBuffers(LogicalDevice logicalDevice, RenderPass renderPass,
                                            SwapChainImages swapChainImages, SwapChain swapChain) {
        return new FrameBuffers(logicalDevice.vkDevice(), renderPass, swapChainImages, swapChain);
    }

    public void drawFrame() {
        VkDevice device = logicalDevice.vkDevice();
        VK10.vkWaitForFences(device, inFlightFences.get(currentFrame), true, VulkanUtil.UINT64_MAX);
        uniformBuffers.get(currentFrame).update(swapChain.getSwapChainExtent());
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            IntBuffer imageIndex = memoryStack.mallocInt(1);
            int acquireNextImageResult = KHRSwapchain.vkAcquireNextImageKHR(device, swapChain.getSwapChainPointer(), VulkanUtil.UINT64_MAX,
                    imageAvailableSemaphores.get(currentFrame).get(0), VK10.VK_NULL_HANDLE, imageIndex);
            if (acquireNextImageResult == KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR) {
                recreateSwapChain(memoryStack);
                return;
            } else if (acquireNextImageResult != VK10.VK_SUCCESS && acquireNextImageResult != KHRSwapchain.VK_SUBOPTIMAL_KHR) {
                throw new IllegalStateException(String.format("Failed to acquire swap chain image! Error code: %d", acquireNextImageResult));
            }
            // Only reset the fence if we are submitting work otherwise vkWaitForFences will wait forever on a signal
            // that will never come.
            VK10.vkResetFences(device, inFlightFences.get(currentFrame));
            VK10.vkResetCommandBuffer(graphicsCommandBuffers.get(currentFrame).getVkCommandBuffer(), 0);
            graphicsCommandBuffers.get(currentFrame)
                    .runCommand(0,
                            (vkCommandBuffer) ->
                                    recordCommandBuffer(imageIndex.get(0), swapChain, frameBuffers, vertexBuffer, indexBuffer, vkCommandBuffer));

            IntBuffer waitStages = memoryStack.mallocInt(1);
            waitStages.put(VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
            waitStages.flip();

            PointerBuffer commandBuffersPointer = memoryStack.mallocPointer(1);
            commandBuffersPointer.put(graphicsCommandBuffers.get(currentFrame).getVkCommandBuffer());
            commandBuffersPointer.flip();

            VkSubmitInfo vkSubmitInfo = VkSubmitInfo.calloc(memoryStack);
            vkSubmitInfo
                    .sType(VK10.VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pWaitSemaphores(imageAvailableSemaphores.get(currentFrame))
                    .waitSemaphoreCount(1)
                    .pWaitDstStageMask(waitStages)
                    .pCommandBuffers(commandBuffersPointer)
                    .pSignalSemaphores(renderFinishedSemaphores.get(currentFrame));

            int result = VK10.vkQueueSubmit(logicalDevice.graphicsQueue().getVkQueue(), vkSubmitInfo, inFlightFences.get(currentFrame).get(0));
            if (result != VK10.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to submit draw command buffer. Error code: %d", result));
            }

            LongBuffer swapChains = memoryStack.mallocLong(1);
            swapChains.put(swapChain.getSwapChainPointer());
            swapChains.flip();
            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(memoryStack);
            presentInfo
                    .sType(KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                    .pWaitSemaphores(renderFinishedSemaphores.get(currentFrame))
                    .pSwapchains(swapChains)
                    .swapchainCount(1)
                    .pImageIndices(imageIndex)
                    .pResults(null);
            int queuePresentResult = KHRSwapchain.vkQueuePresentKHR(logicalDevice.presentationQueue().getVkQueue(), presentInfo);
            if (queuePresentResult == KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR || queuePresentResult == KHRSwapchain.VK_SUBOPTIMAL_KHR || windowHandle.frameBufferResized()) {
                recreateSwapChain(memoryStack);
            } else if (queuePresentResult != VK10.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to present swap chain image! Error code: %d", queuePresentResult));
            }
        }
        currentFrame = (currentFrame + 1) % MAX_FRAMES_IN_FLIGHT;
    }

    public void recordCommandBuffer(int imageIndex, SwapChain swapChain, FrameBuffers frameBuffers, VertexBuffer vertexBuffer,
                                    IndexBuffer indexBuffer, VkCommandBuffer vkCommandBuffer) {
        beginRenderPass(imageIndex, swapChain, frameBuffers, vkCommandBuffer);
        VK10.vkCmdBindPipeline(vkCommandBuffer, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline.getGraphicsPipelinePointer());
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
            VK10.vkCmdSetViewport(vkCommandBuffer, 0, viewport);
            VkRect2D.Buffer scissor = VkRect2D.calloc(1, memoryStack);
            scissor.offset()
                    .x(0)
                    .y(0);
            scissor.extent(swapChainExtent);
            VK10.vkCmdSetScissor(vkCommandBuffer, 0, scissor);
            LongBuffer vertices = memoryStack.mallocLong(1);
            vertices.put(vertexBuffer.getDestinationBuffer().getVulkanBufferHandle());
            vertices.flip();
            LongBuffer offsets = memoryStack.mallocLong(1);
            offsets.put(0);
            offsets.flip();
            VK10.vkCmdBindVertexBuffers(vkCommandBuffer, 0, vertices, offsets);
            VK10.vkCmdBindIndexBuffer(vkCommandBuffer, indexBuffer.getDestinationBuffer().getVulkanBufferHandle(), 0, VK10.VK_INDEX_TYPE_UINT16);
            LongBuffer currDescriptorSetBuffer = memoryStack.mallocLong(1);
            currDescriptorSetBuffer.put(descriptorSets.getDescriptorSetHandles().get(currentFrame));
            currDescriptorSetBuffer.flip();
            VK10.vkCmdBindDescriptorSets(vkCommandBuffer, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline.getPipelineLayoutPointer(), 0, currDescriptorSetBuffer, null);
        }
        VK10.vkCmdDrawIndexed(vkCommandBuffer, indexBuffer.getStructEntriesCount(), 1, 0, 0, 0);
        VK10.vkCmdEndRenderPass(vkCommandBuffer);
    }

    private void beginRenderPass(int imageIndex, SwapChain swapChain, FrameBuffers frameBuffers, VkCommandBuffer vkCommandBuffer) {
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
                    .sType(VK10.VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                    .renderPass(renderPass.getRenderPassPointer())
                    .framebuffer(frameBuffers.getSwapChainFrameBuffers().get(imageIndex))
                    .renderArea(renderArea)
                    .pClearValues(clearValue);
            VK10.vkCmdBeginRenderPass(vkCommandBuffer, renderPassBeginInfo, VK10.VK_SUBPASS_CONTENTS_INLINE);
        }
    }

    public void recreateSwapChain(MemoryStack memoryStack) {
        IntBuffer width = memoryStack.mallocInt(1);
        IntBuffer height = memoryStack.mallocInt(1);
        GLFW.glfwGetFramebufferSize(windowHandle.getWindowHandlePointer(), width, height);
        while (width.get(0) == 0 || height.get(0) == 0) {
            GLFW.glfwGetFramebufferSize(windowHandle.getWindowHandlePointer(), width, height);
            GLFW.glfwWaitEvents();
        }
        VK10.vkDeviceWaitIdle(logicalDevice.vkDevice());
        cleanup();
        swapChain = new SwapChain(
                logicalDevice.vkDevice(),
                physicalDeviceInformation,
                windowHandle,
                windowSurface,
                swapChain.getSwapChainPointer());
        renderPass = renderPass.validateSwapChain(swapChain);
        graphicsPipeline = graphicsPipeline.validateRenderPass(logicalDevice.vkDevice(), renderPass, descriptorSetLayout);
        swapChainImages = createImageViews(logicalDevice, swapChain);
        frameBuffers = createFrameBuffers(logicalDevice, renderPass, swapChainImages, swapChain);
    }

    private void cleanup() {
        frameBuffers.free();
        swapChainImages.free();
        swapChain.freeSwapChainExtent();
    }

    public void free() {
        cleanup();
        swapChain.freeSwapChainPointer();
        descriptorPool.free();
        for (UniformBuffer u : uniformBuffers) {
            u.free();
        }
        indexBuffer.free();
        vertexBuffer.free();
        for (LongBuffer x : inFlightFences) {
            VK10.vkDestroyFence(logicalDevice.vkDevice(), x.get(0), null);
            MemoryUtil.memFree(x);
        }
        for (LongBuffer x : renderFinishedSemaphores) {
            VK10.vkDestroySemaphore(logicalDevice.vkDevice(), x.get(0), null);
            MemoryUtil.memFree(x);
        }
        for (LongBuffer x : imageAvailableSemaphores) {
            VK10.vkDestroySemaphore(logicalDevice.vkDevice(), x.get(0), null);
            MemoryUtil.memFree(x);
        }
        graphicsPipeline.free();
        descriptorSetLayout.free();
        renderPass.free();
    }
}
