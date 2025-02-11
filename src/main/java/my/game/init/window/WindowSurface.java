package my.game.init.window;

import org.lwjgl.sdl.SDLError;
import org.lwjgl.sdl.SDLVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.VkInstance;

import java.nio.LongBuffer;

public class WindowSurface {
    private final long windowSurfaceHandle;
    private final VkInstance vkInstance;

    public WindowSurface(VkInstance vkInstance, WindowHandle windowHandle) {
        this.vkInstance = vkInstance;
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            LongBuffer longBuffer = memoryStack.mallocLong(1);
            if (!SDLVulkan.SDL_Vulkan_CreateSurface(windowHandle.getWindowHandlePointer(), vkInstance, null,
                    longBuffer)) {
                throw new IllegalStateException(SDLError.SDL_GetError());
            }
            windowSurfaceHandle = longBuffer.get(0);
        }
    }

    public long getWindowSurfaceHandle() {
        return windowSurfaceHandle;
    }

    public void free() {
        KHRSurface.vkDestroySurfaceKHR(vkInstance, windowSurfaceHandle, null);
    }
}
