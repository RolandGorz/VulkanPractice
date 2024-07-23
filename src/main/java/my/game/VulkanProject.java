package my.game;

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

    /*Mac issue
    3.3.3 lwjgl
        Identifier=libMoltenVK.dylib
Format=Mach-O thin (arm64)
CodeDirectory v=20400 size=43722 flags=0x20002(adhoc,linker-signed) hashes=1363+0 location=embedded
Signature=adhoc
Info.plist=not bound
TeamIdentifier=not set
Sealed Resources=none
Internal requirements=none
    3.3.4 lwjgl
    Identifier=libMoltenVK-555549442dc301f2957d3c769e91db56ff3e95b7
Format=Mach-O thin (arm64)
CodeDirectory v=20400 size=57773 flags=0x2(adhoc) hashes=1796+5 location=embedded
Signature=adhoc
Info.plist=not bound
TeamIdentifier=not set
Sealed Resources=none
Internal requirements count=0 size=12
     */

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