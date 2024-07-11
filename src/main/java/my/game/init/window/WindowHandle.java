package my.game.init.window;

import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.system.MemoryUtil;

public class WindowHandle {

    final long windowHandlePointer;

    public WindowHandle() {
        // Set up an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!GLFW.glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure GLFW
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE); // the window will stay hidden after creation
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE); // the window will not be resizable
        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_NO_API);

        // Create the window
        windowHandlePointer = GLFW.glfwCreateWindow(800, 600, "Hello World!", MemoryUtil.NULL, MemoryUtil.NULL);
        if (windowHandlePointer == MemoryUtil.NULL)
            throw new RuntimeException("Failed to create the GLFW window");
    }

    public long getWindowHandlePointer() {
        return windowHandlePointer;
    }

    public void free() {
        // Free the window callbacks and destroy the window
        Callbacks.glfwFreeCallbacks(windowHandlePointer);
        GLFW.glfwDestroyWindow(windowHandlePointer);

        // Terminate GLFW and free the error callback
        GLFW.glfwTerminate();
        GLFW.glfwSetErrorCallback(null).free();
    }
}
