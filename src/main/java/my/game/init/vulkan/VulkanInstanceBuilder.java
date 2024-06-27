package my.game.init.vulkan;

import my.game.VulkanProject;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Platform;
import org.lwjgl.vulkan.EXTDebugUtils;
import org.lwjgl.vulkan.KHRPortabilityEnumeration;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.Set;

public class VulkanInstanceBuilder {

    private final DebugVulkanInstanceBuilder debugVulkanInstanceBuilder;

    private VkInstance vkInstance;

    public VulkanInstanceBuilder() {
        debugVulkanInstanceBuilder = DebugVulkanInstanceBuilder.getInstance();
    }

    public VkInstance initVulkan() {
        //Must use calloc when not initializing every value of a struct. Otherwise, garbage is at those values
        //and can result in a crash
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack);

            appInfo.sType(VK13.VK_STRUCTURE_TYPE_APPLICATION_INFO)
                    .pApplicationName(MemoryStack.stackASCII("Hello Triangle"))
                    .applicationVersion(VK13.VK_MAKE_VERSION(1, 0, 0))
                    .pEngineName(MemoryStack.stackASCII("No Engine"))
                    .engineVersion(VK13.VK_MAKE_VERSION(1, 0, 0))
                    .apiVersion(VK13.VK_API_VERSION_1_0);

            VkInstanceCreateInfo vkInstanceCreateInfo = VkInstanceCreateInfo.calloc(stack);
            vkInstanceCreateInfo.sType(VK13.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                    .pApplicationInfo(appInfo)
                    .ppEnabledExtensionNames(getRequiredExtensions());

            if(Platform.get() == Platform.MACOSX) {
                addRequiredMacExtensions(vkInstanceCreateInfo, stack);
                int updatedFlags = vkInstanceCreateInfo.flags() | KHRPortabilityEnumeration.VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR;
                vkInstanceCreateInfo.flags(updatedFlags);
            }

            if (VulkanProject.VULKAN_DEBUG) {
                debugVulkanInstanceBuilder.addDebugForInitializationAndDestruction(vkInstanceCreateInfo, stack);
                debugVulkanInstanceBuilder.addValidationLayers(vkInstanceCreateInfo, stack);
                vkInstanceCreateInfo.pNext(debugVulkanInstanceBuilder.getVkDebugUtilsMessengerCreateInfoEXT());
            }

            PointerBuffer vulkanInstancePointer = stack.mallocPointer(1);
            int result = VK13.vkCreateInstance(vkInstanceCreateInfo, null, vulkanInstancePointer);
            if (result != VK13.VK_SUCCESS) {
                throw new RuntimeException(String.format("creating vulkan instance failed error code %d", result));
            }
            vkInstance = new VkInstance(vulkanInstancePointer.get(0), vkInstanceCreateInfo);
        }
        if (VulkanProject.VULKAN_DEBUG) {
            debugVulkanInstanceBuilder.createDebugUtilsMessengerEXT(vkInstance);
        }
        return vkInstance;
    }

    private void addRequiredMacExtensions(VkInstanceCreateInfo vkInstanceCreateInfo, MemoryStack memoryStack) {
        PointerBuffer originalExtensions = vkInstanceCreateInfo.ppEnabledExtensionNames();
        int originalCapacity = originalExtensions == null ? 0 : originalExtensions.capacity();
        PointerBuffer newExtensions = memoryStack.mallocPointer(originalCapacity + 1);
        if (originalCapacity != 0) {
            newExtensions.put(originalExtensions);
        }
        newExtensions.put(memoryStack.UTF8(KHRPortabilityEnumeration.VK_KHR_PORTABILITY_ENUMERATION_EXTENSION_NAME));
        newExtensions.rewind();
        vkInstanceCreateInfo.ppEnabledExtensionNames(newExtensions);
    }

    public void free() {
        if (VulkanProject.VULKAN_DEBUG) {
            debugVulkanInstanceBuilder.destroyDebugUtilsMessengerEXT(vkInstance);
        }
        VK13.vkDestroyInstance(vkInstance, null);
        if (VulkanProject.VULKAN_DEBUG) {
            debugVulkanInstanceBuilder.free();
        }
    }

    private PointerBuffer getRequiredExtensions() {
        Set<String> supportedExtensions = new HashSet<>();
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
        for (int i = 0; i < glfwRequiredExtensions.capacity(); ++i) {
            String curr = MemoryUtil.memASCII(glfwRequiredExtensions.get(i));
            if (supportedExtensions.contains(curr)) {
                System.out.printf("GLFW required extension %s is supported%n", curr);
            } else {
                System.out.printf("GLFW required extension %s is not supported%n", curr);
                throw new RuntimeException(String.format("GLFW required extension: %s is not supported%n", curr));
            }
        }
        return glfwRequiredExtensions;
    }
}
