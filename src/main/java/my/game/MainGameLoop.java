package my.game;

import my.game.init.vulkan.VulkanInstance;
import my.game.init.vulkan.VulkanInstanceWithDebug;
import my.game.init.vulkan.VulkanInstanceWithoutDebug;
import my.game.init.vulkan.devices.logical.ImmutableLogicalDevice;
import my.game.init.vulkan.devices.logical.LogicalDevice;
import my.game.init.vulkan.devices.physical.PhysicalDeviceRetriever;
import my.game.init.vulkan.drawing.CommandPool;
import my.game.init.vulkan.pipeline.shaders.ShaderCompiler;
import my.game.init.window.WindowHandle;
import my.game.init.window.WindowSurface;
import my.game.render.GraphicsRenderer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.vulkan.VK13;

public class MainGameLoop {
    private volatile boolean RUNNING = true;
    private final WindowHandle windowHandle;
    private final VulkanInstance vulkanInstance;
    private final LogicalDevice logicalDevice;
    private final PhysicalDeviceRetriever chosenPhysicalDevice;
    private final WindowSurface windowSurface;
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
        commandPool = new CommandPool(logicalDevice);
        graphicsRenderer = new GraphicsRenderer(logicalDevice, commandPool, chosenPhysicalDevice.physicalDeviceInformation(), windowHandle, windowSurface);
    }

    public void start() {
        if (RUNNING) {
            GLFW.glfwShowWindow(windowHandle.getWindowHandlePointer());
            while (!GLFW.glfwWindowShouldClose(windowHandle.getWindowHandlePointer()) && RUNNING) {
                GLFW.glfwPollEvents();
                graphicsRenderer.drawFrame();
            }
            destroy();
        } else {
            throw new IllegalStateException("Game loop has already been stopped. Cannot start again");
        }
    }

    public void stop() {
        RUNNING = false;
    }

    private void destroy() {
        VK13.vkDeviceWaitIdle(logicalDevice.vkDevice());
        graphicsRenderer.free();
        commandPool.free();
        logicalDevice.free();
        chosenPhysicalDevice.free();
        windowSurface.free();
        vulkanInstance.free();
        windowHandle.free();
    }
}
