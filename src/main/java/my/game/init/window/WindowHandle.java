package my.game.init.window;

import my.game.render.GraphicsRenderer;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWWindowRefreshCallback;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;

public class WindowHandle {

    private final long windowHandlePointer;

    private GraphicsRenderer graphicsRenderer;

    private Boolean frameBufferResized = false;

    public WindowHandle() {
        // Set up an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!GLFW.glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure GLFW
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE); // the window will stay hidden after creation
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_NO_API);

        // Create the window
        windowHandlePointer = GLFW.glfwCreateWindow(800, 600, "Hello World!", MemoryUtil.NULL, MemoryUtil.NULL);
        GLFWFramebufferSizeCallback framebufferSizeCallback = GLFWFramebufferSizeCallback.create(((window, width, height) ->
                frameBufferResized = true));
        GLFW.glfwSetFramebufferSizeCallback(windowHandlePointer, framebufferSizeCallback);
        GLFWWindowRefreshCallback windowRefreshCallback = GLFWWindowRefreshCallback.create((window) -> {
            //This should always return and not busy wait like we do when minimized. If the width or height is 0 then we should just return.
            //TODO rendering still stops when not resizing and just holding corner of window / moving window
            try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                IntBuffer width = memoryStack.mallocInt(1);
                IntBuffer height = memoryStack.mallocInt(1);
                GLFW.glfwGetFramebufferSize(windowHandlePointer, width, height);
                if (width.get(0) == 0 || height.get(0) == 0) {
                    return;
                }
                graphicsRenderer.recreateSwapChain(width, height);
            }
            graphicsRenderer.drawFrame();
        });
        GLFW.glfwSetWindowRefreshCallback(windowHandlePointer, windowRefreshCallback);
        if (windowHandlePointer == MemoryUtil.NULL)
            throw new RuntimeException("Failed to create the GLFW window");
    }

    public void setGraphicsRenderer(GraphicsRenderer graphicsRenderer) {
        this.graphicsRenderer = graphicsRenderer;
    }

    //Reset the state after a query. If the resizing stopped then this should be false and if the resizing continues it will be set back to true;
    public boolean frameBufferResized() {
        if (frameBufferResized) {
            frameBufferResized = false;
            return true;
        }
        return false;
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
