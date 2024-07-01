package my.game.init.window;

import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkInstance;

import java.nio.LongBuffer;

public class WindowSurface {
    private final long windowSurfaceHandle;
    private final VkInstance vkInstance;

    public WindowSurface(VkInstance vkInstance, WindowHandle windowHandle) {
        this.vkInstance = vkInstance;
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            LongBuffer longBuffer = memoryStack.mallocLong(1);
            int result = GLFWVulkan.glfwCreateWindowSurface(vkInstance, windowHandle.windowHandlePointer, null,
                    longBuffer);
            if (result != VK13.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to create window surface with error code %d", result));
            }
            windowSurfaceHandle = longBuffer.get(0);
        }
    }

    public void free() {
        KHRSurface.vkDestroySurfaceKHR(vkInstance, windowSurfaceHandle, null);
    }
}
