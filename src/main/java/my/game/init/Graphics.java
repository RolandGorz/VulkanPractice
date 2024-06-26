package my.game.init;

import my.game.VulkanProject;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.EXTDebugUtils;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackDataEXT;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackEXT;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkLayerProperties;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//TODO pop stack sooner. Think about how to free memory asap. ValidateVulkanExtensions is an example of stupidly not popping
// stack until the end
public class Graphics {

    //TODO cleanly start using singletons.
    private static VkDebugUtilsMessengerCreateInfoEXT vkDebugUtilsMessengerCreateInfoEXTinstance;
    private static VkDebugUtilsMessengerCallbackEXT vkDebugUtilsMessengerCallbackEXTinstance;

    private PointerBuffer validateVulkanExtensions() {
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

    private void validateVulkanLayers(List<String> requestedValidationLayers) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer layerCount = stack.mallocInt(1); // int*
            int result = VK13.vkEnumerateInstanceLayerProperties(layerCount, null);
            if (result == VK13.VK_SUCCESS) {
                System.out.println("vkEnumerateInstanceLayerProperties returned success");
            } else {
                System.out.printf("vkEnumerateInstanceLayerProperties returned failure code %d%n", result);
            }
            System.out.printf("%d available layers%n", layerCount.get(0));
            VkLayerProperties.Buffer vkLayerPropertiesBuffer = VkLayerProperties.malloc(layerCount.get(0), stack);
            int result2 = VK13.vkEnumerateInstanceLayerProperties(layerCount, vkLayerPropertiesBuffer);
            if (result2 == VK13.VK_SUCCESS) {
                System.out.println("vkEnumerateInstanceLayerProperties returned success");
            } else {
                System.out.println("vkEnumerateInstanceLayerProperties returned failure");
            }
            System.out.printf("%d extensions supported%n", layerCount.get(0));
            for (VkLayerProperties x : vkLayerPropertiesBuffer) {
                System.out.printf("%s%n", x.layerNameString());
            }
            for (String x : requestedValidationLayers) {
                boolean found = false;
                for (VkLayerProperties vkLayerProperty : vkLayerPropertiesBuffer) {
                    if (vkLayerProperty.layerNameString().equals(x)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new RuntimeException(String.format("Failed to find validation layer %s", x));
                }
            }
            System.out.println("All requested validation layers found");
        }
    }

    private PointerBuffer addKhronosValidationLayer(MemoryStack stack) {
        List<String> requestedValidationLayers = new ArrayList<>();
        requestedValidationLayers.add("VK_LAYER_KHRONOS_validation");
        validateVulkanLayers(requestedValidationLayers);
        PointerBuffer validationLayers = stack.mallocPointer(requestedValidationLayers.size());
        for (String layer : requestedValidationLayers) {
            validationLayers.put(stack.UTF8(layer));
        }
        return validationLayers.rewind();
    }

    private PointerBuffer addExtensions(MemoryStack stack) {
        PointerBuffer glfwRequiredExtensions = validateVulkanExtensions();
        if (VulkanProject.VULKAN_DEBUG) {
            PointerBuffer extensions = stack.mallocPointer(glfwRequiredExtensions.capacity() + 1);
            extensions.put(glfwRequiredExtensions);
            extensions.put(stack.UTF8(EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME));
            return extensions.rewind();
        } else {
            return glfwRequiredExtensions;
        }
    }

    public void freeVkDebugUtilsMessengerCreateInfoEXT() {
        if (vkDebugUtilsMessengerCreateInfoEXTinstance != null) {
            vkDebugUtilsMessengerCreateInfoEXTinstance.free();
            vkDebugUtilsMessengerCallbackEXTinstance.free();
        }
    }

    private VkDebugUtilsMessengerCreateInfoEXT getVkDebugUtilsMessengerCreateInfoEXT() {
        if (vkDebugUtilsMessengerCreateInfoEXTinstance == null) {
            VkDebugUtilsMessengerCallbackEXT callback = VkDebugUtilsMessengerCallbackEXT.create(
                    (messageSeverity, messageTypes, pCallbackData, pUserData) -> {
                        final String severity;
                        if (messageSeverity == EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT) {
                            severity = "Verbose";
                        } else if (messageSeverity == EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT) {
                            severity = "Warning";
                        } else if (messageSeverity == EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT) {
                            severity = "Info";
                        } else {
                            severity = "Error";
                        }
                        //Previous commit said not to free things created by BufferUtils. That is wrong. I just don't need
                        // to free this VkDebugUtilsMessengerCallbackDataEXT since it's a struct created from an existing
                        // memory address that will be freed by whatever created it.
                        VkDebugUtilsMessengerCallbackDataEXT data = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
                        System.out.printf("%s pCallBackData.pMessage: %s%n", severity, MemoryUtil.memASCII(data.pMessage()));
                        return VK13.VK_FALSE;
                    }
            );
            VkDebugUtilsMessengerCreateInfoEXT vkDebugUtilsMessengerCreateInfoEXT = VkDebugUtilsMessengerCreateInfoEXT.calloc();
            vkDebugUtilsMessengerCreateInfoEXT
                    .sType(EXTDebugUtils.VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
                    .messageSeverity(
                            EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT |
                                    EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT |
                                    EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT)
                    .messageType(
                            EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT |
                                    EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT |
                                    EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT)
                    .pfnUserCallback(callback)
                    .pUserData(MemoryUtil.NULL);
            vkDebugUtilsMessengerCreateInfoEXTinstance = vkDebugUtilsMessengerCreateInfoEXT;
            vkDebugUtilsMessengerCallbackEXTinstance = callback;
            return vkDebugUtilsMessengerCreateInfoEXT;
        } else {
            return vkDebugUtilsMessengerCreateInfoEXTinstance;
        }
    }

    public long createDebugUtilsMessengerEXT(VkInstance vkInstance) {

        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            LongBuffer longBuffer = memoryStack.callocLong(1);
            int result = EXTDebugUtils.vkCreateDebugUtilsMessengerEXT(vkInstance,
                    getVkDebugUtilsMessengerCreateInfoEXT(), null, longBuffer);
            if (result != VK13.VK_SUCCESS) {
                throw new RuntimeException(String.format("creating debug utils messenger failed error code %d", result));
            }
            return longBuffer.get();
        }
    }

    public VkInstance initVulkan() {
        //Must use calloc when not initializing every value of a struct. Otherwise, garbage is at those values
        //and can result in a crash
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack);
            appInfo.sType(VK13.VK_STRUCTURE_TYPE_APPLICATION_INFO);
            appInfo.pApplicationName(MemoryStack.stackASCII("Hello Triangle"));
            appInfo.applicationVersion(VK13.VK_MAKE_VERSION(1, 0, 0));
            appInfo.pEngineName(MemoryStack.stackASCII("No Engine"));
            appInfo.engineVersion(VK13.VK_MAKE_VERSION(1, 0, 0));
            appInfo.apiVersion(VK13.VK_API_VERSION_1_0);

            VkInstanceCreateInfo vkInstanceCreateInfo = VkInstanceCreateInfo.calloc(stack);
            vkInstanceCreateInfo.sType(VK13.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
            vkInstanceCreateInfo.pApplicationInfo(appInfo);
            vkInstanceCreateInfo.ppEnabledExtensionNames(addExtensions(stack));
            if (VulkanProject.VULKAN_DEBUG) {
                vkInstanceCreateInfo.ppEnabledLayerNames(addKhronosValidationLayer(stack));
                vkInstanceCreateInfo.pNext(getVkDebugUtilsMessengerCreateInfoEXT());
            }

            PointerBuffer vulkanInstancePointer = stack.mallocPointer(1);
            int result = VK13.vkCreateInstance(vkInstanceCreateInfo, null, vulkanInstancePointer);
            if (result != VK13.VK_SUCCESS) {
                throw new RuntimeException(String.format("creating vulkan instance failed error code %d", result));
            }
            return new VkInstance(vulkanInstancePointer.get(0), vkInstanceCreateInfo);
        }
    }
}
