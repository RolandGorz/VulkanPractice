package my.game;

import my.game.init.Graphics;
import my.game.init.Window;
import my.game.shaders.ShaderLoader;
import org.lwjgl.Version;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.vulkan.EXTDebugUtils;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkInstance;

//TODO properly free memory when there are exceptions. Right now im just letting it leak like crazy especially in
// the places where I just throw runtime exception
public class HelloWorld {

    public static boolean VULKAN_DEBUG = Boolean.parseBoolean(System.getProperty("myGameVulkanDebug"));

    public static void main(String[] args) {
        System.out.println("Hello LWJGL " + Version.getVersion() + "!");

        ShaderLoader shaderLoader = new ShaderLoader();
        shaderLoader.loadShaders();

        Window window = new Window();
        long windowPointer = window.initialize();
        Graphics graphics = new Graphics();
        VkInstance vkInstance = graphics.initVulkan();
        //TODO checking if debug is enabled or not for this part sucks. Clean it up
        long pDebugUtilsMessengerEXT = 0L;
        if (VULKAN_DEBUG) {
            pDebugUtilsMessengerEXT = graphics.createDebugUtilsMessengerEXT(vkInstance);
        }

        GLFW.glfwShowWindow(windowPointer);
        while (!GLFW.glfwWindowShouldClose(windowPointer)) {
            GLFW.glfwPollEvents();
        }

        if (VULKAN_DEBUG) {
            EXTDebugUtils.vkDestroyDebugUtilsMessengerEXT(vkInstance, pDebugUtilsMessengerEXT, null);
        }
        //Free vulkan
        VK13.vkDestroyInstance(vkInstance, null);

        // Free the window callbacks and destroy the window
        Callbacks.glfwFreeCallbacks(windowPointer);
        GLFW.glfwDestroyWindow(windowPointer);

        // Terminate GLFW and free the error callback
        GLFW.glfwTerminate();
        GLFW.glfwSetErrorCallback(null).free();
    }
}