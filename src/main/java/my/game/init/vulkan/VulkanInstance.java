package my.game.init.vulkan;

import com.google.common.collect.ImmutableSet;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.KHRGetPhysicalDeviceProperties2;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.Set;

public class VulkanInstance {

    protected VkInstance vkInstance;
    //If this exists we add it because VK_KHR_portability_subset will require it if we use that
    private final ImmutableSet<String> OPTIONAL_EXTENSIONS = ImmutableSet.of(
            KHRGetPhysicalDeviceProperties2.VK_KHR_GET_PHYSICAL_DEVICE_PROPERTIES_2_EXTENSION_NAME
    );

    VulkanInstance() {
    }

    protected VkInstanceCreateInfo createCreateInfo(final MemoryStack memoryStack) {
        //Must use calloc when not initializing every value of a struct. Otherwise, garbage is at those values
        //and can result in a crash
        VkApplicationInfo appInfo = VkApplicationInfo.calloc(memoryStack);

        appInfo.sType(VK13.VK_STRUCTURE_TYPE_APPLICATION_INFO)
                .pApplicationName(MemoryStack.stackASCII("Hello Triangle"))
                .applicationVersion(VK13.VK_MAKE_VERSION(1, 0, 0))
                .pEngineName(MemoryStack.stackASCII("No Engine"))
                .engineVersion(VK13.VK_MAKE_VERSION(1, 0, 0))
                .apiVersion(VK13.VK_API_VERSION_1_0);

        VkInstanceCreateInfo vkInstanceCreateInfo = VkInstanceCreateInfo.calloc(memoryStack);
        vkInstanceCreateInfo.sType(VK13.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                .pApplicationInfo(appInfo)
                .ppEnabledExtensionNames(getExtensions(memoryStack));

        return vkInstanceCreateInfo;
    }

    protected void createVulkanInstance(final MemoryStack memoryStack, final VkInstanceCreateInfo vkInstanceCreateInfo) {
        PointerBuffer vulkanInstancePointer = memoryStack.mallocPointer(1);
        int result = VK13.vkCreateInstance(vkInstanceCreateInfo, null, vulkanInstancePointer);
        if (result != VK13.VK_SUCCESS) {
            throw new RuntimeException(String.format("creating vulkan instance failed error code %d", result));
        }
        vkInstance = new VkInstance(vulkanInstancePointer.get(0), vkInstanceCreateInfo);
    }

    public VkInstance getHandle() {
        return vkInstance;
    }

    public void free() {
        VK13.vkDestroyInstance(vkInstance, null);
    }

    private PointerBuffer getExtensions(MemoryStack memoryStack) {
        Set<String> supportedExtensions = new HashSet<>();
        //Extending the stack to prevent running out of memory
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer extensionCount = stack.mallocInt(1); // int*
            int result = VK13.vkEnumerateInstanceExtensionProperties((ByteBuffer) null, extensionCount, null);
            if (result == VK13.VK_SUCCESS) {
                System.out.println("vkEnumerateInstanceExtensionProperties returned success");
            } else {
                System.out.printf("vkEnumerateInstanceExtensionProperties returned failure code %d%n", result);
            }
            System.out.printf("%d extensions supported%n", extensionCount.get(0));
            VkExtensionProperties.Buffer vkExtensionPropertiesBuffer = VkExtensionProperties.malloc(extensionCount.get(0), stack);
            int result2 = VK13.vkEnumerateInstanceExtensionProperties((ByteBuffer) null, extensionCount, vkExtensionPropertiesBuffer);
            if (result2 == VK13.VK_SUCCESS) {
                System.out.println("vkEnumerateInstanceExtensionProperties returned success");
            } else {
                System.out.printf("vkEnumerateInstanceExtensionProperties returned failure code %d%n", result2);
            }
            System.out.printf("%d extensions supported%n", extensionCount.get(0));
            for (VkExtensionProperties x : vkExtensionPropertiesBuffer) {
                System.out.printf("%s%n", x.extensionNameString());
                supportedExtensions.add(x.extensionNameString());
            }
        }
        PointerBuffer glfwRequiredExtensions = GLFWVulkan.glfwGetRequiredInstanceExtensions();
        if (glfwRequiredExtensions == null) {
            throw new RuntimeException("glfwGetRequiredInstanceExtensions returned null");
        }
        PointerBuffer extensions = memoryStack.callocPointer(glfwRequiredExtensions.capacity() + OPTIONAL_EXTENSIONS.size());
        for (int i = 0; i < glfwRequiredExtensions.capacity(); ++i) {
            String curr = MemoryUtil.memASCII(glfwRequiredExtensions.get(i));
            if (supportedExtensions.contains(curr)) {
                System.out.printf("GLFW required extension %s is supported%n", curr);
            } else {
                System.out.printf("GLFW required extension %s is not supported%n", curr);
                throw new RuntimeException(String.format("GLFW required extension: %s is not supported%n", curr));
            }
        }
        extensions.put(glfwRequiredExtensions);
        for (String x : OPTIONAL_EXTENSIONS) {
            if (supportedExtensions.contains(x)) {
                extensions.put(MemoryStack.stackASCII(x));
            }
        }
        extensions.flip();
        return extensions;
    }
}
