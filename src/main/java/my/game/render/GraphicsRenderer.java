package my.game.render;

import com.google.common.collect.ImmutableList;
import my.game.init.vulkan.VulkanUtil;
import my.game.init.vulkan.devices.logical.LogicalDevice;
import my.game.init.vulkan.devices.physical.PhysicalDeviceInformation;
import my.game.init.vulkan.drawing.CommandBuffers;
import my.game.init.vulkan.drawing.CommandPool;
import my.game.init.vulkan.drawing.FrameBuffers;
import my.game.init.vulkan.pipeline.GraphicsPipeline;
import my.game.init.vulkan.pipeline.RenderPass;
import my.game.init.vulkan.swapchain.SwapChain;
import my.game.init.vulkan.swapchain.SwapChainImages;
import my.game.init.window.WindowHandle;
import my.game.init.window.WindowSurface;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkFenceCreateInfo;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkSubmitInfo;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.List;

public class GraphicsRenderer {

    public static final int MAX_FRAMES_IN_FLIGHT = 2;
    private final LogicalDevice logicalDevice;
    private final CommandBuffers commandBuffers;
    private final RenderPass renderPass;
    private final PhysicalDeviceInformation physicalDeviceInformation;
    private final WindowHandle windowHandle;
    private final WindowSurface windowSurface;
    private final GraphicsPipeline graphicsPipeline;
    private final List<LongBuffer> imageAvailableSemaphores;
    private final List<LongBuffer> renderFinishedSemaphores;
    private final List<LongBuffer> inFlightFences;
    private int currentFrame = 0;
    private SwapChain swapChain;
    private SwapChainImages swapChainImages;
    private FrameBuffers frameBuffers;

    public GraphicsRenderer(LogicalDevice logicalDevice, CommandPool commandPool,
                            PhysicalDeviceInformation physicalDeviceInformation,
                            WindowHandle windowHandle, WindowSurface windowSurface) {
        this.logicalDevice = logicalDevice;
        this.physicalDeviceInformation = physicalDeviceInformation;
        this.windowHandle = windowHandle;
        this.windowSurface = windowSurface;
        this.swapChain = createSwapChain(logicalDevice, physicalDeviceInformation, windowHandle, windowSurface);
        this.swapChainImages = createImageViews(logicalDevice, swapChain);
        this.renderPass = new RenderPass(logicalDevice.vkDevice(), swapChain.getSurfaceFormat());
        this.graphicsPipeline = new GraphicsPipeline(logicalDevice.vkDevice(), renderPass);
        this.commandBuffers = new CommandBuffers(commandPool, renderPass, graphicsPipeline);
        this.frameBuffers = createFrameBuffers(logicalDevice, renderPass, swapChainImages, swapChain);
        ImmutableList.Builder<LongBuffer> imageAvailableSemaphoresBuilder = ImmutableList.builder();
        ImmutableList.Builder<LongBuffer> renderFinishedSemaphoresBuilder = ImmutableList.builder();
        ImmutableList.Builder<LongBuffer> inFlightFencesBuilder = ImmutableList.builder();
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.calloc(memoryStack);
            semaphoreCreateInfo
                    .sType(VK13.VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
            VkFenceCreateInfo fenceCreateInfo = VkFenceCreateInfo.calloc(memoryStack);
            fenceCreateInfo
                    .sType(VK13.VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                    .flags(VK13.VK_FENCE_CREATE_SIGNALED_BIT);
            for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; ++i) {
                LongBuffer imageAvailableSemaphore = MemoryUtil.memAllocLong(1);
                int result = VK13.vkCreateSemaphore(logicalDevice.vkDevice(), semaphoreCreateInfo, null, imageAvailableSemaphore);
                if (result != VK13.VK_SUCCESS) {
                    throw new IllegalStateException(String.format("Failed to create semaphore. Error code: %d", result));
                }
                LongBuffer renderFinishedSemaphore = MemoryUtil.memAllocLong(1);
                int result2 = VK13.vkCreateSemaphore(logicalDevice.vkDevice(), semaphoreCreateInfo, null, renderFinishedSemaphore);
                if (result2 != VK13.VK_SUCCESS) {
                    throw new IllegalStateException(String.format("Failed to create semaphore. Error code: %d", result2));
                }
                LongBuffer inFlightFence = MemoryUtil.memAllocLong(1);
                int result3 = VK13.vkCreateFence(logicalDevice.vkDevice(), fenceCreateInfo, null, inFlightFence);
                if (result3 != VK13.VK_SUCCESS) {
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

    private SwapChain createSwapChain(LogicalDevice logicalDevice, PhysicalDeviceInformation physicalDeviceInformation,
                                      WindowHandle windowHandle, WindowSurface windowSurface) {
        return new SwapChain(
                logicalDevice.vkDevice(),
                physicalDeviceInformation,
                windowHandle,
                windowSurface);
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
        VK13.vkWaitForFences(device, inFlightFences.get(currentFrame), true, VulkanUtil.UINT64_MAX);
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            IntBuffer imageIndex = memoryStack.mallocInt(1);
            int acquireNextImageResult = KHRSwapchain.vkAcquireNextImageKHR(device, swapChain.getSwapChainPointer(), VulkanUtil.UINT64_MAX,
                    imageAvailableSemaphores.get(currentFrame).get(0), VK13.VK_NULL_HANDLE, imageIndex);
            if (acquireNextImageResult == KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR) {
                recreateSwapChain(memoryStack);
                return;
            } else if (acquireNextImageResult != VK13.VK_SUCCESS && acquireNextImageResult != KHRSwapchain.VK_SUBOPTIMAL_KHR) {
                throw new IllegalStateException(String.format("Failed to acquire swap chain image! Error code: %d", acquireNextImageResult));
            }
            // Only reset the fence if we are submitting work otherwise vkWaitForFences will wait forever on a signal
            // that will never come.
            VK13.vkResetFences(device, inFlightFences.get(currentFrame));
            VK13.vkResetCommandBuffer(commandBuffers.get(currentFrame).getCommandBuffer(), 0);
            commandBuffers.get(currentFrame).recordCommandBuffer(imageIndex.get(0), swapChain, frameBuffers);

            IntBuffer waitStages = memoryStack.mallocInt(1);
            waitStages.put(VK13.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
            waitStages.flip();

            PointerBuffer commandBuffersPointer = memoryStack.mallocPointer(1);
            commandBuffersPointer.put(commandBuffers.get(currentFrame).getCommandBuffer());
            commandBuffersPointer.flip();

            VkSubmitInfo vkSubmitInfo = VkSubmitInfo.calloc(memoryStack);
            vkSubmitInfo
                    .sType(VK13.VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pWaitSemaphores(imageAvailableSemaphores.get(currentFrame))
                    .waitSemaphoreCount(1)
                    .pWaitDstStageMask(waitStages)
                    .pCommandBuffers(commandBuffersPointer)
                    .pSignalSemaphores(renderFinishedSemaphores.get(currentFrame));

            int result = VK13.vkQueueSubmit(logicalDevice.graphicsQueue().getVkQueue(), vkSubmitInfo, inFlightFences.get(currentFrame).get(0));
            if (result != VK13.VK_SUCCESS) {
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
            } else if (queuePresentResult != VK13.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to present swap chain image! Error code: %d", queuePresentResult));
            }
        }
        currentFrame = (currentFrame + 1) % MAX_FRAMES_IN_FLIGHT;
    }

    public void recreateSwapChain(MemoryStack memoryStack) {
        IntBuffer width = memoryStack.mallocInt(1);
        IntBuffer height = memoryStack.mallocInt(1);
        GLFW.glfwGetFramebufferSize(windowHandle.getWindowHandlePointer(), width, height);
        while (width.get(0) == 0 || height.get(0) == 0) {
            GLFW.glfwGetFramebufferSize(windowHandle.getWindowHandlePointer(), width, height);
            GLFW.glfwWaitEvents();
        }
        VK13.vkDeviceWaitIdle(logicalDevice.vkDevice());
        cleanupSwapChain();
        /*
        Note that we don’t recreate the renderpass here for simplicity.
        In theory it can be possible for the swap chain image format to change during an applications' lifetime,
        e.g. when moving a window from an standard range to an high dynamic range monitor.
        This may require the application to recreate the renderpass to make sure the change between dynamic
        ranges is properly reflected.
         */
        //TODO the disadvantage of this approach is that we need to stop all rendering before creating the new swap chain.
        // It is possible to create a new swap chain while drawing commands on an image from the old swap chain are still in-flight.
        // You need to pass the previous swap chain to the oldSwapchain field in the VkSwapchainCreateInfoKHR struct and destroy
        // the old swap chain as soon as you’ve finished using it.
        swapChain = createSwapChain(logicalDevice, physicalDeviceInformation, windowHandle, windowSurface);
        swapChainImages = createImageViews(logicalDevice, swapChain);
        frameBuffers = createFrameBuffers(logicalDevice, renderPass, swapChainImages, swapChain);
    }

    private void cleanupSwapChain() {
        frameBuffers.free();
        swapChainImages.free();
        swapChain.free();
    }

    public void free() {
        cleanupSwapChain();
        for (LongBuffer x : inFlightFences) {
            VK13.vkDestroyFence(logicalDevice.vkDevice(), x.get(0), null);
            MemoryUtil.memFree(x);
        }
        for (LongBuffer x : renderFinishedSemaphores) {
            VK13.vkDestroySemaphore(logicalDevice.vkDevice(), x.get(0), null);
            MemoryUtil.memFree(x);
        }
        for (LongBuffer x : imageAvailableSemaphores) {
            VK13.vkDestroySemaphore(logicalDevice.vkDevice(), x.get(0), null);
            MemoryUtil.memFree(x);
        }
        renderPass.free();
        graphicsPipeline.free();
    }
}
