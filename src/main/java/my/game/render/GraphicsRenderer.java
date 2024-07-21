package my.game.render;

import com.google.common.collect.ImmutableList;
import my.game.init.vulkan.VulkanUtil;
import my.game.init.vulkan.devices.logical.LogicalDevice;
import my.game.init.vulkan.drawing.CommandBuffers;
import my.game.init.vulkan.swapchain.SwapChain;
import org.lwjgl.PointerBuffer;
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
    private final SwapChain swapChain;
    private final CommandBuffers commandBuffers;
    private final List<LongBuffer> imageAvailableSemaphores;
    private final List<LongBuffer> renderFinishedSemaphores;
    private final List<LongBuffer> inFlightFences;
    private int currentFrame = 0;

    public GraphicsRenderer(LogicalDevice logicalDevice, SwapChain swapChain, CommandBuffers commandBuffers) {
        this.logicalDevice = logicalDevice;
        this.commandBuffers = commandBuffers;
        this.swapChain = swapChain;
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

    public void drawFrame() {
        VkDevice device = logicalDevice.vkDevice();
        VK13.vkWaitForFences(device, inFlightFences.get(currentFrame), true, VulkanUtil.UINT64_MAX);
        VK13.vkResetFences(device, inFlightFences.get(currentFrame));
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            IntBuffer imageIndex = memoryStack.mallocInt(1);
            KHRSwapchain.vkAcquireNextImageKHR(device, swapChain.getSwapChainPointer(), VulkanUtil.UINT64_MAX,
                    imageAvailableSemaphores.get(currentFrame).get(0), VK13.VK_NULL_HANDLE, imageIndex);
            VK13.vkResetCommandBuffer(commandBuffers.get(currentFrame).getCommandBuffer(), 0);
            commandBuffers.get(currentFrame).recordCommandBuffer(imageIndex.get(0));

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
            KHRSwapchain.vkQueuePresentKHR(logicalDevice.presentationQueue().getVkQueue(), presentInfo);
        }
        currentFrame = (currentFrame + 1) % MAX_FRAMES_IN_FLIGHT;
    }

    public void free() {
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
    }
}
