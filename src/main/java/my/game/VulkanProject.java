package my.game;

import my.game.main.MainGameLoop;
import org.lwjgl.Version;
import org.lwjgl.system.Configuration;

import java.io.IOException;

//TODO properly free memory when there are exceptions. Right now im just letting it leak like crazy especially in
// the places where I just throw runtime exception
public class VulkanProject {

    public static boolean VULKAN_DEBUG;

    static {
        VULKAN_DEBUG = Boolean.parseBoolean(System.getProperty("myGameVulkanDebug"));
        if (VULKAN_DEBUG) {
            Configuration.DEBUG_MEMORY_ALLOCATOR.set(true);
            Configuration.DEBUG_STACK.set(true);
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Hello LWJGL " + Version.getVersion() + "!");
        new MainGameLoop().start();
    }
}