package my.game;

import my.game.init.vulkan.VulkanInstance;
import my.game.init.vulkan.VulkanInstanceWithDebug;
import my.game.init.vulkan.VulkanInstanceWithoutDebug;
import my.game.init.vulkan.devices.logical.ImmutableLogicalDevice;
import my.game.init.vulkan.devices.physical.PhysicalDeviceRetriever;
import my.game.init.vulkan.drawing.CommandBuffer;
import my.game.init.vulkan.drawing.CommandPool;
import my.game.init.vulkan.devices.logical.LogicalDevice;
import my.game.init.vulkan.drawing.FrameBuffers;
import my.game.init.vulkan.pipeline.GraphicsPipeline;
import my.game.init.vulkan.pipeline.RenderPass;
import my.game.init.vulkan.pipeline.shaders.ShaderCompiler;
import my.game.init.vulkan.swapchain.SwapChain;
import my.game.init.vulkan.swapchain.SwapChainImages;
import my.game.init.window.WindowHandle;
import my.game.init.window.WindowSurface;
import my.game.render.GraphicsRenderer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.vulkan.VK13;

public class MainGameLoop {
    private final WindowHandle windowHandle;
    private final VulkanInstance vulkanInstance;
    private final LogicalDevice logicalDevice;
    private final PhysicalDeviceRetriever chosenPhysicalDevice;
    private final WindowSurface windowSurface;
    private final SwapChain swapChain;
    private final SwapChainImages swapChainImages;
    private final RenderPass renderPass;
    private final GraphicsPipeline graphicsPipeline;
    private final FrameBuffers frameBuffers;
    private final CommandPool commandPool;
    private final GraphicsRenderer graphicsRenderer;

    public MainGameLoop() {
        ShaderCompiler shaderCompiler = new ShaderCompiler();
        shaderCompiler.compileShaders();
        windowHandle = new WindowHandle();
        if (VulkanProject.VULKAN_DEBUG) {
            vulkanInstance = new VulkanInstanceWithDebug();
        } else {
            vulkanInstance = new VulkanInstanceWithoutDebug();
        }
        windowSurface = new WindowSurface(vulkanInstance.getHandle(), windowHandle);
        chosenPhysicalDevice = new PhysicalDeviceRetriever(vulkanInstance.getHandle(), windowSurface);
        logicalDevice = ImmutableLogicalDevice.builder().physicalDevice(chosenPhysicalDevice).build();
        swapChain = new SwapChain(
                logicalDevice.vkDevice(),
                chosenPhysicalDevice.physicalDeviceInformation(),
                windowHandle,
                windowSurface);
        swapChainImages = new SwapChainImages(logicalDevice.vkDevice(), swapChain);
        renderPass = new RenderPass(logicalDevice.vkDevice(), swapChain);
        graphicsPipeline = new GraphicsPipeline(logicalDevice.vkDevice(), renderPass);
        frameBuffers = new FrameBuffers(logicalDevice.vkDevice(), renderPass, swapChainImages, swapChain);
        commandPool = new CommandPool(logicalDevice);
        CommandBuffer commandBuffer = new CommandBuffer(commandPool, frameBuffers, renderPass, graphicsPipeline, swapChain);
        graphicsRenderer = new GraphicsRenderer(logicalDevice, swapChain, commandBuffer);
    }

    public void start() {
        GLFW.glfwShowWindow(windowHandle.getWindowHandlePointer());
        while (!GLFW.glfwWindowShouldClose(windowHandle.getWindowHandlePointer())) {
            graphicsRenderer.drawFrame();
            GLFW.glfwWaitEvents();
        }
        VK13.vkDeviceWaitIdle(logicalDevice.vkDevice());
        destroy();
    }

    private void destroy() {
        graphicsRenderer.free();
        commandPool.free();
        frameBuffers.free();
        graphicsPipeline.free();
        renderPass.free();
        swapChainImages.free();
        swapChain.free();
        logicalDevice.free();
        chosenPhysicalDevice.free();
        windowSurface.free();
        vulkanInstance.free();
        windowHandle.free();
    }
}
