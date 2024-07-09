package my.game;

import my.game.main.MainGameLoop;
import org.lwjgl.Version;
import org.lwjgl.system.Configuration;

/*IMPORTANT INFO
The ownership of application-owned memory is immediately acquired by any Vulkan command it is passed into.
Ownership of such memory must be released back to the application at the end of the duration of the command, so that the
application can alter or free this memory as soon as all the commands that acquired it have returned.
Basically I should free anything I create once the function that I call in vulkan completes.
 */
public class VulkanProject {

    public static boolean VULKAN_DEBUG;

    static {
        VULKAN_DEBUG = Boolean.parseBoolean(System.getProperty("myGameVulkanDebug"));
        if (VULKAN_DEBUG) {
            Configuration.DEBUG_MEMORY_ALLOCATOR.set(true);
            Configuration.DEBUG_STACK.set(true);
        }
    }

    public static void main(String[] args) {
        System.out.println("Hello LWJGL " + Version.getVersion() + "!");
        new MainGameLoop().start();
    }
}