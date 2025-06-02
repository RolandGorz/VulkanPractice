package my.game.init.window;

import org.lwjgl.sdl.SDLError;
import org.lwjgl.sdl.SDLEvents;
import org.lwjgl.sdl.SDLInit;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDLVulkan;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.sdl.SDL_EventFilter;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class WindowHandle {

    private final long windowHandlePointer;

    public WindowHandle() {

        //Initialize sdl for video since we will obviously have a window we want to present to and load vulkan explicitly
        //even though creating the window will load the default its best to be explicit.
        if (!SDLInit.SDL_InitSubSystem(SDLInit.SDL_INIT_VIDEO) | !SDLVulkan.SDL_Vulkan_LoadLibrary((ByteBuffer) null)) {
            throw new IllegalStateException(SDLError.SDL_GetError());
        }

        // Create the window
        windowHandlePointer = SDLVideo.SDL_CreateWindow("Hello World!", 800, 600,
                SDLVideo.SDL_WINDOW_HIDDEN | SDLVideo.SDL_WINDOW_RESIZABLE | SDLVideo.SDL_WINDOW_VULKAN);
        if (windowHandlePointer == MemoryUtil.NULL) {
            throw new IllegalStateException(SDLError.SDL_GetError());
        }
    }

    public long getWindowHandlePointer() {
        return windowHandlePointer;
    }

    public void free() {
        // Free the event filter and destroy the window
        SDLVideo.SDL_DestroyWindow(windowHandlePointer);
        SDLVulkan.SDL_Vulkan_UnloadLibrary();
        SDLInit.SDL_QuitSubSystem(SDLInit.SDL_INIT_VIDEO);
        SDLInit.SDL_Quit();
    }
}
