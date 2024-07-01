package my.game.main;

import my.game.init.vulkan.devices.logical.LogicalDevice;
import my.game.init.vulkan.devices.physical.PhysicalDeviceInformation;
import my.game.init.vulkan.devices.physical.PhysicalDevices;
import my.game.init.window.WindowHandle;
import my.game.init.vulkan.VulkanInstanceBuilder;
import my.game.init.vulkan.devices.queue.GraphicsQueue;
import my.game.init.window.WindowSurface;
import my.game.shaders.ShaderCompiler;
import my.game.shaders.ShaderLoader;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.vulkan.VkInstance;

import java.util.PriorityQueue;

public class MainGameLoop {

    private final ShaderCompiler shaderCompiler;
    private final ShaderLoader shaderLoader;
    private final VulkanInstanceBuilder vulkanInstanceBuilder;
    private final PhysicalDevices devices;
    private WindowHandle windowHandle;
    private LogicalDevice logicalDevice;
    private GraphicsQueue graphicsQueue;
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
        PriorityQueue<PhysicalDeviceInformation> physicalDeviceScores = devices.getPhysicalDevices(vulkanInstance, windowSurface);
        if (physicalDeviceScores.isEmpty() || physicalDeviceScores.peek().getScore() == 0) {
            throw new RuntimeException("No device found that is capable of rendering anything with. We give up");
        }
        PhysicalDeviceInformation chosenDevice = physicalDeviceScores.poll();
        logicalDevice = new LogicalDevice(chosenDevice);
        graphicsQueue = new GraphicsQueue(logicalDevice.getLogicalDeviceInformation());
    }

    private void destroy() {
        logicalDevice.free();
        windowSurface.free();
        vulkanInstanceBuilder.free();
        windowHandle.free();
    }
}
