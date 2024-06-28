package my.game.main;

import my.game.init.vulkan.devices.logical.LogicalDevice;
import my.game.init.vulkan.devices.physical.PhysicalDeviceInformation;
import my.game.init.vulkan.devices.physical.PhysicalDevices;
import my.game.init.Window;
import my.game.init.vulkan.VulkanInstanceBuilder;
import my.game.init.vulkan.devices.queue.GraphicsQueue;
import my.game.shaders.ShaderCompiler;
import my.game.shaders.ShaderLoader;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.vulkan.VkInstance;

import java.util.PriorityQueue;

public class MainGameLoop {

    private final ShaderCompiler shaderCompiler;
    private final ShaderLoader shaderLoader;
    private final Window window;
    private final VulkanInstanceBuilder vulkanInstanceBuilder;
    private final PhysicalDevices devices;
    private LogicalDevice logicalDevice;
    private GraphicsQueue graphicsQueue;

    private long windowPointer;

    public MainGameLoop() {
        shaderCompiler = new ShaderCompiler();
        shaderLoader = new ShaderLoader();
        window = new Window();
        vulkanInstanceBuilder = new VulkanInstanceBuilder();
        devices = new PhysicalDevices();
    }

    public void start() {
        initialize();
        GLFW.glfwShowWindow(windowPointer);
        while (!GLFW.glfwWindowShouldClose(windowPointer)) {
            GLFW.glfwPollEvents();
        }
        destroy();
    }

    private void initialize() {
        shaderCompiler.compileShaders();
        shaderLoader.loadShaders();
        windowPointer = window.initialize();
        VkInstance vulkanInstance = vulkanInstanceBuilder.initVulkan();
        PriorityQueue<PhysicalDeviceInformation> physicalDeviceScores = devices.getPhysicalDevices(vulkanInstance);
        if (physicalDeviceScores.isEmpty() || physicalDeviceScores.peek().score() == 0) {
            throw new RuntimeException("No device found that is capable of rendering anything with. We give up");
        }
        PhysicalDeviceInformation chosenDevice = physicalDeviceScores.poll();
        logicalDevice = new LogicalDevice(chosenDevice);
        graphicsQueue = new GraphicsQueue(logicalDevice.getLogicalDeviceInformation());
    }

    private void destroy() {
        logicalDevice.free();
        vulkanInstanceBuilder.free();

        // Free the window callbacks and destroy the window
        Callbacks.glfwFreeCallbacks(windowPointer);
        GLFW.glfwDestroyWindow(windowPointer);

        // Terminate GLFW and free the error callback
        GLFW.glfwTerminate();
        GLFW.glfwSetErrorCallback(null).free();
    }
}
