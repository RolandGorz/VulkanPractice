package my.game;

import org.lwjgl.Version;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkExtensionProperties;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class HelloWorld {

    public static void main(String[] args) {
        System.out.println("Hello LWJGL " + Version.getVersion() + "!");

        //TODO this is kind of stupid and will only work running from intellij. Think of how you want to handle shaders
        // properly.
        File fragmentShader = new File("src/main/resources/shaders/simple_shader.frag.spv");
        File vertexShader = new File("src/main/resources/shaders/simple_shader.vert.spv");

        System.out.printf("Fragment shader code size %d\n", fragmentShader.length());
        System.out.printf("Vertex shader code size %d\n", vertexShader.length());

        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!GLFW.glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure GLFW
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE); // the window will stay hidden after creation
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE); // the window will be resizable
        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_NO_API);

        // Create the window
        final long window = GLFW.glfwCreateWindow(300, 300, "Hello World!", MemoryUtil.NULL, MemoryUtil.NULL);
        if (window == MemoryUtil.NULL)
            throw new RuntimeException("Failed to create the GLFW window");


        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer extensionCount = stack.mallocInt(1); // int*
            int result  = VK13.vkEnumerateInstanceExtensionProperties((ByteBuffer) null, extensionCount, null);
            if (result == VK13.VK_SUCCESS) {
                System.out.println("vkEnumerateInstanceExtensionProperties returned success");
            } else {
                System.out.println("vkEnumerateInstanceExtensionProperties returned failure");
            }
            System.out.printf("%d extensions supported\n", extensionCount.get(0));
            VkExtensionProperties.Buffer vkExtensionPropertiesBuffer = VkExtensionProperties.malloc(extensionCount.get(0), stack);
            int result2 = VK13.vkEnumerateInstanceExtensionProperties((ByteBuffer) null, extensionCount, vkExtensionPropertiesBuffer);
            if (result == VK13.VK_SUCCESS) {
                System.out.println("vkEnumerateInstanceExtensionProperties returned success");
            } else {
                System.out.println("vkEnumerateInstanceExtensionProperties returned failure");
            }
            System.out.printf("%d extensions supported\n", extensionCount.get(0));
            for (VkExtensionProperties x : vkExtensionPropertiesBuffer) {
                System.out.printf("%s\n", x.extensionNameString());
            }
        } // the stack frame is popped automatically

        GLFW.glfwShowWindow(window);
        while (!GLFW.glfwWindowShouldClose(window)) {
            GLFW.glfwPollEvents();
        }

        // Free the window callbacks and destroy the window
        Callbacks.glfwFreeCallbacks(window);
        GLFW.glfwDestroyWindow(window);

        // Terminate GLFW and free the error callback
        GLFW.glfwTerminate();
        GLFW.glfwSetErrorCallback(null).free();
    }
}