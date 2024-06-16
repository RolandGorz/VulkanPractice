package my.game;

import org.lwjgl.Version;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK13;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class HelloWorld {

    public static void main(String[] args) {
        System.out.println("Hello LWJGL " + Version.getVersion() + "!");

        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!GLFW.glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure GLFW
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE); // the window will stay hidden after creation
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE); // the window will be resizable

        // Create the window
        final long window = GLFW.glfwCreateWindow(300, 300, "Hello World!", MemoryUtil.NULL, MemoryUtil.NULL);
        if (window == MemoryUtil.NULL)
            throw new RuntimeException("Failed to create the GLFW window");


        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer extensionCount = stack.mallocInt(1); // int*
            int result  = VK13.vkEnumerateInstanceExtensionProperties((ByteBuffer) null, extensionCount, null);
            System.out.printf("vkEnumerateInstanceExtensionProperties returned %d\n", result);
            System.out.printf("%d extensions supported\n", extensionCount.get(0));
        } // the stack frame is popped automatically

        GLFW.glfwMakeContextCurrent(window);
        // Enable v-sync
        GLFW.glfwSwapInterval(1);
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