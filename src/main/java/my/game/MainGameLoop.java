package my.game;

import my.game.init.vulkan.VulkanInstance;
import my.game.init.vulkan.VulkanInstanceWithDebug;
import my.game.init.vulkan.VulkanInstanceWithoutDebug;
import my.game.init.vulkan.devices.physical.PhysicalDevice;
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
    private final PhysicalDevice chosenPhysicalDevice;
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
        chosenPhysicalDevice = new PhysicalDevice(vulkanInstance.getHandle(), windowSurface);
        logicalDevice = new LogicalDevice(chosenPhysicalDevice);
        swapChain = new SwapChain(logicalDevice, windowHandle, windowSurface);
        swapChainImages = new SwapChainImages(swapChain);
        renderPass = new RenderPass(swapChainImages);
        graphicsPipeline = new GraphicsPipeline(swapChainImages, renderPass);
        frameBuffers = new FrameBuffers(renderPass);
        commandPool = new CommandPool(logicalDevice);
        CommandBuffer commandBuffer = new CommandBuffer(commandPool, frameBuffers, graphicsPipeline);
        graphicsRenderer = new GraphicsRenderer(logicalDevice, swapChain, commandBuffer);
    }

    public void start() {
        GLFW.glfwShowWindow(windowHandle.getWindowHandlePointer());
        while (!GLFW.glfwWindowShouldClose(windowHandle.getWindowHandlePointer())) {
            GLFW.glfwPollEvents();
            graphicsRenderer.drawFrame();
        }
        VK13.vkDeviceWaitIdle(logicalDevice.getLogicalDeviceInformation().vkDevice());
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
