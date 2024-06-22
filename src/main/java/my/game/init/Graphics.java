package my.game.init;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.Set;

public class Graphics {
    private void validateVulkanExtensions(MemoryStack stack) {
        IntBuffer extensionCount = stack.mallocInt(1); // int*
        int result  = VK13.vkEnumerateInstanceExtensionProperties((ByteBuffer) null, extensionCount, null);
        if (result == VK13.VK_SUCCESS) {
            System.out.println("vkEnumerateInstanceExtensionProperties returned success");
        } else {
            System.out.println("vkEnumerateInstanceExtensionProperties returned failure");
        }
        System.out.printf("%d extensions supported\n", extensionCount.get(0));
        VkExtensionProperties.Buffer vkExtensionPropertiesBuffer = VkExtensionProperties.malloc(extensionCount.get(0), stack);
        int result2 = VK13.vkEnumerateInstanceExtensionProperties((ByteBuffer) null, extensionCount, vkExtensionPropertiesBuffer);
        if (result2 == VK13.VK_SUCCESS) {
            System.out.println("vkEnumerateInstanceExtensionProperties returned success");
        } else {
            System.out.println("vkEnumerateInstanceExtensionProperties returned failure");
        }
        System.out.printf("%d extensions supported\n", extensionCount.get(0));
        Set<String> supportedExtensions = new HashSet<>();
        for (VkExtensionProperties x : vkExtensionPropertiesBuffer) {
            System.out.printf("%s\n", x.extensionNameString());
            supportedExtensions.add(x.extensionNameString());
        }

        PointerBuffer glfwRequiredExtensions = GLFWVulkan.glfwGetRequiredInstanceExtensions();
        if (glfwRequiredExtensions == null) {
            throw new RuntimeException("glfwGetRequiredInstanceExtensions returned null");
        }
        for (int i = 0; i < glfwRequiredExtensions.capacity(); ++i) {
            String curr = MemoryUtil.memASCII(glfwRequiredExtensions.get(i));
            if (supportedExtensions.contains(curr)) {
                System.out.printf("GLFW required extension %s is supported\n", curr);
            } else {
                System.out.printf("GLFW required extension %s is not supported\n", curr);
                throw new RuntimeException(String.format("GLFW required extension: %s is not supported\n", curr));
            }
        }
    }

    public VkInstance initVulkan() {
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            validateVulkanExtensions(memoryStack);
            VkApplicationInfo appInfo = VkApplicationInfo.malloc(memoryStack);
            appInfo.sType(VK13.VK_STRUCTURE_TYPE_APPLICATION_INFO);
            appInfo.pApplicationName(MemoryUtil.memASCII("Hello Triangle"));
            appInfo.applicationVersion(VK13.VK_MAKE_VERSION(1, 0, 0));
            appInfo.pEngineName(MemoryUtil.memASCII("No Engine"));
            appInfo.engineVersion(VK13.VK_MAKE_VERSION(1, 0, 0));
            appInfo.apiVersion(VK13.VK_API_VERSION_1_0);

            VkInstanceCreateInfo vkInstanceCreateInfo = VkInstanceCreateInfo.malloc(memoryStack);
            vkInstanceCreateInfo.sType(VK13.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
            vkInstanceCreateInfo.pApplicationInfo(appInfo);
            PointerBuffer pointerBuffer = memoryStack.mallocPointer(1);
            int result = VK13.vkCreateInstance(vkInstanceCreateInfo, null, pointerBuffer);
            if (result != VK13.VK_SUCCESS) {
                throw new RuntimeException("creating vulkan instance failed");
            }
            return new VkInstance(pointerBuffer.get(0), vkInstanceCreateInfo);
        }
    }
}
