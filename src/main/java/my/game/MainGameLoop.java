package my.game;

import my.game.init.vulkan.VulkanInstance;
import my.game.init.vulkan.VulkanInstanceWithDebug;
import my.game.init.vulkan.VulkanInstanceWithoutDebug;
import my.game.init.vulkan.command.CommandPool;
import my.game.init.vulkan.devices.logical.ImmutableLogicalDevice;
import my.game.init.vulkan.devices.logical.LogicalDevice;
import my.game.init.vulkan.devices.physical.PhysicalDeviceRetriever;
import my.game.init.vulkan.pipeline.shaders.ShaderCompiler;
import my.game.init.window.WindowHandle;
import my.game.init.window.WindowSurface;
import my.game.render.GraphicsRenderer;
import org.lwjgl.sdl.SDLEvents;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;

public class MainGameLoop {
    private final WindowHandle windowHandle;
    private final VulkanInstance vulkanInstance;
    private final LogicalDevice logicalDevice;
    private final PhysicalDeviceRetriever chosenPhysicalDevice;
    private final WindowSurface windowSurface;
    private final CommandPool graphicsCommandPool;
    private final CommandPool transferCommandPool;
    private final GraphicsRenderer graphicsRenderer;
    private volatile boolean RUNNING = true;

    public MainGameLoop() {
        ShaderCompiler shaderCompiler = new ShaderCompiler();
        shaderCompiler.compileShaders();
        windowHandle = new WindowHandle();
        if (VulkanProject.VULKAN_DEBUG) {
            vulkanInstance = new VulkanInstanceWithDebug();
        } else {
            vulkanInstance = new VulkanInstanceWithoutDebug();
        }
        windowSurface = new WindowSurface(vulkanInstance.getHandle(), windowHandle);
        chosenPhysicalDevice = new PhysicalDeviceRetriever(vulkanInstance.getHandle(), windowSurface);
        logicalDevice = ImmutableLogicalDevice.builder().physicalDevice(chosenPhysicalDevice).build();
        graphicsCommandPool = new CommandPool(logicalDevice.vkDevice(), logicalDevice.graphicsQueue());
        transferCommandPool = new CommandPool(logicalDevice.vkDevice(), logicalDevice.transferVulkanQueue());
        graphicsRenderer = new GraphicsRenderer(logicalDevice, graphicsCommandPool, transferCommandPool, chosenPhysicalDevice.physicalDeviceInformation(), windowHandle, windowSurface);
    }

    public void start() {
        if (RUNNING) {
            SDL_Event event = SDL_Event.calloc();
            SDLVideo.SDL_ShowWindow(windowHandle.getWindowHandlePointer());
            mainLoop:
            while (RUNNING) {
                while(SDLEvents.SDL_PollEvent(event)) {
                    switch (event.type()) {
                        case SDLEvents.SDL_EVENT_QUIT:
                            RUNNING = false;
                            break mainLoop;
                        case SDLEvents.SDL_EVENT_WINDOW_RESIZED:
                            try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                                graphicsRenderer.recreateSwapChain(memoryStack);
                            }
                            break;
                    }
                }
                graphicsRenderer.drawFrame();
            }
            event.free();
            destroy();
        } else {
            throw new IllegalStateException("Game loop has already been stopped. Cannot start again");
        }
    }

    public void stop() {
        RUNNING = false;
    }

    private void destroy() {
        VK10.vkDeviceWaitIdle(logicalDevice.vkDevice());
        graphicsRenderer.free();
        transferCommandPool.free();
        graphicsCommandPool.free();
        logicalDevice.free();
        chosenPhysicalDevice.free();
        windowSurface.free();
        vulkanInstance.free();
        windowHandle.free();
    }
}
