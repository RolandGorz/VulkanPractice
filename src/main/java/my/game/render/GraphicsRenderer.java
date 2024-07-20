package my.game.render;

import my.game.init.vulkan.VulkanUtil;
import my.game.init.vulkan.devices.logical.LogicalDevice;
import my.game.init.vulkan.drawing.CommandBuffer;
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

public class GraphicsRenderer {

    private final LogicalDevice logicalDevice;
    private final SwapChain swapChain;
    private final CommandBuffer commandBuffer;
    private final LongBuffer imageAvailableSemaphore;
    private final LongBuffer renderFinishedSemaphore;
    private final LongBuffer inFlightFence;

    public GraphicsRenderer(LogicalDevice logicalDevice, SwapChain swapChain, CommandBuffer commandBuffer) {
        this.logicalDevice = logicalDevice;
        this.commandBuffer = commandBuffer;
        this.swapChain = swapChain;
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.calloc(memoryStack);
            semaphoreCreateInfo
                    .sType(VK13.VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
            VkFenceCreateInfo fenceCreateInfo = VkFenceCreateInfo.calloc(memoryStack);
            fenceCreateInfo
                    .sType(VK13.VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                    .flags(VK13.VK_FENCE_CREATE_SIGNALED_BIT);
            imageAvailableSemaphore = MemoryUtil.memAllocLong(1);
            int result = VK13.vkCreateSemaphore(logicalDevice.vkDevice(), semaphoreCreateInfo, null, imageAvailableSemaphore);
            if (result != VK13.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to create semaphore. Error code: %d", result));
            }
            renderFinishedSemaphore = MemoryUtil.memAllocLong(1);
            int result2 = VK13.vkCreateSemaphore(logicalDevice.vkDevice(), semaphoreCreateInfo, null, renderFinishedSemaphore);
            if (result2 != VK13.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to create semaphore. Error code: %d", result2));
            }
            inFlightFence = MemoryUtil.memAllocLong(1);
            int result3 = VK13.vkCreateFence(logicalDevice.vkDevice(), fenceCreateInfo, null, inFlightFence);
            if (result3 != VK13.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to create fence. Error code: %d", result3));
            }
        }
    }

    public void drawFrame() {
        VkDevice device = logicalDevice.vkDevice();
        VK13.vkWaitForFences(device, inFlightFence, true, VulkanUtil.UINT64_MAX);
        VK13.vkResetFences(device, inFlightFence);
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            IntBuffer imageIndex = memoryStack.mallocInt(1);
            KHRSwapchain.vkAcquireNextImageKHR(device, swapChain.getSwapChainPointer(), VulkanUtil.UINT64_MAX, imageAvailableSemaphore.get(0), VK13.VK_NULL_HANDLE, imageIndex);
            VK13.vkResetCommandBuffer(commandBuffer.getCommandBuffer(), 0);
            commandBuffer.recordCommandBuffer(imageIndex.get(0));

            IntBuffer waitStages = memoryStack.mallocInt(1);
            waitStages.put(VK13.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
            waitStages.flip();

            PointerBuffer commandBuffersPointer = memoryStack.mallocPointer(1);
            commandBuffersPointer.put(commandBuffer.getCommandBuffer());
            commandBuffersPointer.flip();

            VkSubmitInfo vkSubmitInfo = VkSubmitInfo.calloc(memoryStack);
            vkSubmitInfo
                    .sType(VK13.VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pWaitSemaphores(imageAvailableSemaphore)
                    .waitSemaphoreCount(1)
                    .pWaitDstStageMask(waitStages)
                    .pCommandBuffers(commandBuffersPointer)
                    .pSignalSemaphores(renderFinishedSemaphore);

            int result = VK13.vkQueueSubmit(logicalDevice.graphicsQueue().getVkQueue(), vkSubmitInfo, inFlightFence.get(0));
            if (result != VK13.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to submit draw command buffer. Error code: %d", result));
            }

            LongBuffer swapChains = memoryStack.mallocLong(1);
            swapChains.put(swapChain.getSwapChainPointer());
            swapChains.flip();
            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(memoryStack);
            presentInfo
                    .sType(KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                    .pWaitSemaphores(renderFinishedSemaphore)
                    .pSwapchains(swapChains)
                    .swapchainCount(1)
                    .pImageIndices(imageIndex)
                    .pResults(null);
            KHRSwapchain.vkQueuePresentKHR(logicalDevice.presentationQueue().getVkQueue(), presentInfo);
        }
    }

    public void free() {
        VK13.vkDestroyFence(logicalDevice.vkDevice(), inFlightFence.get(0), null);
        MemoryUtil.memFree(inFlightFence);
        VK13.vkDestroySemaphore(logicalDevice.vkDevice(), renderFinishedSemaphore.get(0), null);
        MemoryUtil.memFree(renderFinishedSemaphore);
        VK13.vkDestroySemaphore(logicalDevice.vkDevice(), imageAvailableSemaphore.get(0), null);
        MemoryUtil.memFree(imageAvailableSemaphore);
    }
}
