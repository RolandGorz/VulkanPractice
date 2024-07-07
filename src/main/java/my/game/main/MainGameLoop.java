package my.game.main;

import my.game.init.vulkan.devices.logical.LogicalDevice;
import my.game.init.vulkan.devices.physical.PhysicalDevices;
import my.game.init.vulkan.devices.physical.ValidPhysicalDevice;
import my.game.init.window.WindowHandle;
import my.game.init.vulkan.VulkanInstanceBuilder;
import my.game.init.window.WindowSurface;
import my.game.shaders.ShaderCompiler;
import my.game.shaders.ShaderLoader;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.vulkan.VkInstance;

public class MainGameLoop {

    private final ShaderCompiler shaderCompiler;
    private final ShaderLoader shaderLoader;
    private final VulkanInstanceBuilder vulkanInstanceBuilder;
    private final PhysicalDevices devices;
    private WindowHandle windowHandle;
    private LogicalDevice logicalDevice;
    private ValidPhysicalDevice chosenPhysicalDevice;
    private WindowSurface windowSurface;

    public MainGameLoop() {
        shaderCompiler = new ShaderCompiler();
        shaderLoader = new ShaderLoader();
        vulkanInstanceBuilder = new VulkanInstanceBuilder();
        devices = new PhysicalDevices();
    }

    public void start() {
        initialize();
        GLFW.glfwShowWindow(windowHandle.getWindowHandlePointer());
        while (!GLFW.glfwWindowShouldClose(windowHandle.getWindowHandlePointer())) {
            GLFW.glfwPollEvents();
        }
        destroy();
    }

    private void initialize() {
        shaderCompiler.compileShaders();
        shaderLoader.loadShaders();
        windowHandle = new WindowHandle();
        VkInstance vulkanInstance = vulkanInstanceBuilder.initVulkan();
        windowSurface = new WindowSurface(vulkanInstance, windowHandle);
        chosenPhysicalDevice = devices.getValidPhysicalDevice(vulkanInstance, windowSurface);
        logicalDevice = new LogicalDevice(chosenPhysicalDevice);
    }

    private void destroy() {
        logicalDevice.free();
        chosenPhysicalDevice.free();
        windowSurface.free();
        vulkanInstanceBuilder.free();
        windowHandle.free();
    }
}
