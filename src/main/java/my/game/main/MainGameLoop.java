package my.game.main;

import my.game.init.vulkan.VulkanInstanceBuilder;
import my.game.init.vulkan.VulkanUtil;
import my.game.init.vulkan.drawing.CommandBuffer;
import my.game.init.vulkan.drawing.CommandPool;
import my.game.init.vulkan.devices.logical.LogicalDevice;
import my.game.init.vulkan.devices.physical.PhysicalDevices;
import my.game.init.vulkan.devices.physical.ValidPhysicalDevice;
import my.game.init.vulkan.drawing.FrameBuffers;
import my.game.init.vulkan.pipeline.GraphicsPipeline;
import my.game.init.vulkan.pipeline.RenderPass;
import my.game.init.vulkan.pipeline.shaders.ShaderCompiler;
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
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkSubmitInfo;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

public class MainGameLoop {

    private final ShaderCompiler shaderCompiler;
    private final VulkanInstanceBuilder vulkanInstanceBuilder;
    private final PhysicalDevices devices;
    private WindowHandle windowHandle;
    private LogicalDevice logicalDevice;
    private ValidPhysicalDevice chosenPhysicalDevice;
    private WindowSurface windowSurface;
    private SwapChain swapChain;
    private SwapChainImages swapChainImages;
    private RenderPass renderPass;
    private GraphicsPipeline graphicsPipeline;
    private FrameBuffers frameBuffers;
    private CommandPool commandPool;
    private CommandBuffer commandBuffer;
    private LongBuffer imageAvailableSemaphore;
    private LongBuffer renderFinishedSemaphore;
    private LongBuffer inFlightFence;

    public MainGameLoop() {
        shaderCompiler = new ShaderCompiler();
        vulkanInstanceBuilder = new VulkanInstanceBuilder();
        devices = new PhysicalDevices();
    }

    public void start() {
        initialize();
        GLFW.glfwShowWindow(windowHandle.getWindowHandlePointer());
        while (!GLFW.glfwWindowShouldClose(windowHandle.getWindowHandlePointer())) {
            GLFW.glfwPollEvents();
            drawFrame();
        }
        VK13.vkDeviceWaitIdle(logicalDevice.getLogicalDeviceInformation().vkDevice());
        destroy();
    }

    private void drawFrame() {
        VkDevice device = logicalDevice.getLogicalDeviceInformation().vkDevice();
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

            int result = VK13.vkQueueSubmit(logicalDevice.getLogicalDeviceInformation().graphicsQueue().getVkQueue(), vkSubmitInfo, inFlightFence.get(0));
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
            KHRSwapchain.vkQueuePresentKHR(logicalDevice.getLogicalDeviceInformation().presentationQueue().getVkQueue(), presentInfo);
        }
    }

    private void initialize() {
        shaderCompiler.compileShaders();
        windowHandle = new WindowHandle();
        VkInstance vulkanInstance = vulkanInstanceBuilder.initVulkan();
        windowSurface = new WindowSurface(vulkanInstance, windowHandle);
        chosenPhysicalDevice = devices.getValidPhysicalDevice(vulkanInstance, windowSurface);
        logicalDevice = new LogicalDevice(chosenPhysicalDevice);
        swapChain = new SwapChain(logicalDevice, windowHandle, windowSurface);
        swapChainImages = new SwapChainImages(swapChain);
        renderPass = new RenderPass(swapChainImages);
        graphicsPipeline = new GraphicsPipeline(swapChainImages, renderPass);
        frameBuffers = new FrameBuffers(renderPass);
        commandPool = new CommandPool(logicalDevice);
        commandBuffer = new CommandBuffer(commandPool, frameBuffers, graphicsPipeline);
        createSyncObjects();
    }

    private void createSyncObjects() {
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.calloc(memoryStack);
            semaphoreCreateInfo
                    .sType(VK13.VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
            VkFenceCreateInfo fenceCreateInfo = VkFenceCreateInfo.calloc(memoryStack);
            fenceCreateInfo
                    .sType(VK13.VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                    .flags(VK13.VK_FENCE_CREATE_SIGNALED_BIT);
            imageAvailableSemaphore = MemoryUtil.memAllocLong(1);
            int result = VK13.vkCreateSemaphore(logicalDevice.getLogicalDeviceInformation().vkDevice(), semaphoreCreateInfo, null, imageAvailableSemaphore);
            if (result != VK13.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to create semaphore. Error code: %d", result));
            }
            renderFinishedSemaphore = MemoryUtil.memAllocLong(1);
            int result2 = VK13.vkCreateSemaphore(logicalDevice.getLogicalDeviceInformation().vkDevice(), semaphoreCreateInfo, null, renderFinishedSemaphore);
            if (result2 != VK13.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to create semaphore. Error code: %d", result2));
            }
            inFlightFence = MemoryUtil.memAllocLong(1);
            int result3 = VK13.vkCreateFence(logicalDevice.getLogicalDeviceInformation().vkDevice(), fenceCreateInfo, null, inFlightFence);
            if (result3 != VK13.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to create fence. Error code: %d", result3));
            }
        }
    }

    private void destroy() {
        VK13.vkDestroyFence(logicalDevice.getLogicalDeviceInformation().vkDevice(), inFlightFence.get(0), null);
        MemoryUtil.memFree(inFlightFence);
        VK13.vkDestroySemaphore(logicalDevice.getLogicalDeviceInformation().vkDevice(), renderFinishedSemaphore.get(0), null);
        MemoryUtil.memFree(renderFinishedSemaphore);
        VK13.vkDestroySemaphore(logicalDevice.getLogicalDeviceInformation().vkDevice(), imageAvailableSemaphore.get(0), null);
        MemoryUtil.memFree(imageAvailableSemaphore);
        commandPool.free();
        frameBuffers.free();
        graphicsPipeline.free();
        renderPass.free();
        swapChainImages.free();
        swapChain.free();
        logicalDevice.free();
        chosenPhysicalDevice.free();
        windowSurface.free();
        vulkanInstanceBuilder.free();
        windowHandle.free();
    }
}
