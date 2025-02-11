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

    private Boolean frameBufferResized = false;

    private final SDL_EventFilter sdlEventFilter = SDL_EventFilter.create(
            (userdata, event) -> {
                if (SDL_Event.create(event).type() == SDLEvents.SDL_EVENT_WINDOW_RESIZED) {
                    frameBufferResized = true;
                }
                return true;
            }
    );

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

        //Add event watch for resizing of window. Should this be handled here or handled in the main game loop? Good question.
        //Here for now since event watch will be invoked by the thread emitting the event itself (which may be a system/OS thread)
        //which would prevent us from blocking on our main loop and possibly allow us to have a smooth resize as the game runs.
        //For now just doing this way for posterity.
        SDLEvents.SDL_AddEventWatch(sdlEventFilter, MemoryUtil.NULL);
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
        // Free the event filter and destroy the window
        SDLEvents.SDL_RemoveEventWatch(sdlEventFilter, MemoryUtil.NULL);
        sdlEventFilter.free();
        SDLVideo.SDL_DestroyWindow(windowHandlePointer);
        SDLVulkan.SDL_Vulkan_UnloadLibrary();
        SDLInit.SDL_QuitSubSystem(SDLInit.SDL_INIT_VIDEO);
        SDLInit.SDL_Quit();
    }
}
