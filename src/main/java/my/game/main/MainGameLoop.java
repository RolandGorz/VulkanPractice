package my.game.main;

import my.game.VulkanProject;
import my.game.init.Devices;
import my.game.init.Window;
import my.game.init.vulkan.VulkanInstanceBuilder;
import my.game.shaders.ShaderCompiler;
import my.game.shaders.ShaderLoader;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.vulkan.EXTDebugUtils;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkPhysicalDevice;

import java.util.PriorityQueue;

public class MainGameLoop {

    private final ShaderCompiler shaderCompiler;
    private final ShaderLoader shaderLoader;
    private final Window window;
    private final VulkanInstanceBuilder vulkanInstanceBuilder;
    private final Devices devices;

    private long windowPointer;
    private long pDebugUtilsMessengerEXT = 0L;
    private VkInstance vulkanInstance;

    public MainGameLoop() {
        shaderCompiler = new ShaderCompiler();
        shaderLoader = new ShaderLoader();
        window = new Window();
        vulkanInstanceBuilder = new VulkanInstanceBuilder();
        devices = new Devices();
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
        vulkanInstance = vulkanInstanceBuilder.initVulkan();
        PriorityQueue<Devices.PhysicalDeviceInformation> physicalDeviceScores = devices.getPhysicalDevices(vulkanInstance);
        if (physicalDeviceScores.isEmpty() || physicalDeviceScores.peek().score() == 0) {
            throw new RuntimeException("No device found that is capable of rendering anything with. We give up");
        }
        VkPhysicalDevice chosenDevice = physicalDeviceScores.poll().physicalDevice();
    }

    private void destroy() {
        vulkanInstanceBuilder.free();

        // Free the window callbacks and destroy the window
        Callbacks.glfwFreeCallbacks(windowPointer);
        GLFW.glfwDestroyWindow(windowPointer);

        // Terminate GLFW and free the error callback
        GLFW.glfwTerminate();
        GLFW.glfwSetErrorCallback(null).free();
    }
}
