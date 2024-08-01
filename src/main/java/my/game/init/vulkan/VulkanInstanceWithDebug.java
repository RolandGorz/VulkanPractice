package my.game.init.vulkan;

import com.google.common.collect.ImmutableList;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Platform;
import org.lwjgl.vulkan.EXTDebugUtils;
import org.lwjgl.vulkan.KHRPortabilityEnumeration;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackDataEXT;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackEXT;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkLayerProperties;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.List;

public class VulkanInstanceWithDebug extends VulkanInstance {

    private final VkDebugUtilsMessengerCreateInfoEXT debugUtilsMessengerCreateInfo;
    private final VkDebugUtilsMessengerCallbackEXT callback;
    List<String> REQUESTED_VALIDATION_LAYERS = ImmutableList.of(
            "VK_LAYER_KHRONOS_validation"
    );
    private long pDebugUtilsMessengerEXT;

    public VulkanInstanceWithDebug() {
        callback = VkDebugUtilsMessengerCallbackEXT.create(
                (messageSeverity, messageTypes, pCallbackData, pUserData) -> {
                    final String severity = getSeverityString(messageSeverity);
                    //Previous commit said not to free things created by BufferUtils. That is wrong. I just don't need
                    // to free this VkDebugUtilsMessengerCallbackDataEXT since it's a struct created from an existing
                    // memory address that will be freed by whatever created it.
                    VkDebugUtilsMessengerCallbackDataEXT data = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
                    System.out.printf("%s pCallBackData.pMessage: %s%n", severity, data.pMessageString());
                    return VK13.VK_FALSE;
                }
        );
        debugUtilsMessengerCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc();
        debugUtilsMessengerCreateInfo
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
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            VkInstanceCreateInfo createInfo = super.createCreateInfo(memoryStack);
            if (Platform.get() == Platform.MACOSX) {
                addRequiredMacExtensions(createInfo, memoryStack);
                int updatedFlags = createInfo.flags() | KHRPortabilityEnumeration.VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR;
                createInfo.flags(updatedFlags);
            }
            addDebugForInitializationAndDestruction(createInfo, memoryStack);
            addValidationLayers(createInfo, memoryStack);
            createInfo.pNext(debugUtilsMessengerCreateInfo);
            super.createVulkanInstance(memoryStack, createInfo);
        }
        createDebugUtilsMessengerEXT(super.vkInstance);
    }

    private void addRequiredMacExtensions(VkInstanceCreateInfo vkInstanceCreateInfo, MemoryStack memoryStack) {
        PointerBuffer originalExtensions = vkInstanceCreateInfo.ppEnabledExtensionNames();
        int originalCapacity = originalExtensions == null ? 0 : originalExtensions.capacity();
        PointerBuffer newExtensions = memoryStack.mallocPointer(originalCapacity + 1);
        if (originalCapacity != 0) {
            newExtensions.put(originalExtensions);
        }
        newExtensions.put(memoryStack.UTF8(KHRPortabilityEnumeration.VK_KHR_PORTABILITY_ENUMERATION_EXTENSION_NAME));
        newExtensions.flip();
        vkInstanceCreateInfo.ppEnabledExtensionNames(newExtensions);
    }

    private String getSeverityString(int messageSeverity) {
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
        return severity;
    }

    public void addDebugForInitializationAndDestruction(VkInstanceCreateInfo vkInstanceCreateInfo, MemoryStack memoryStack) {
        PointerBuffer originalExtensions = vkInstanceCreateInfo.ppEnabledExtensionNames();
        int originalCapacity = originalExtensions == null ? 0 : originalExtensions.capacity();
        PointerBuffer newExtensions = memoryStack.mallocPointer(originalCapacity + 1);
        if (originalCapacity != 0) {
            newExtensions.put(originalExtensions);
        }
        newExtensions.put(memoryStack.UTF8(EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME));
        newExtensions.flip();
        vkInstanceCreateInfo.ppEnabledExtensionNames(newExtensions);
    }

    public void addValidationLayers(VkInstanceCreateInfo vkInstanceCreateInfo, MemoryStack stack) {
        validateValidationLayers(REQUESTED_VALIDATION_LAYERS);
        PointerBuffer validationLayers = stack.mallocPointer(REQUESTED_VALIDATION_LAYERS.size());
        for (String layer : REQUESTED_VALIDATION_LAYERS) {
            validationLayers.put(stack.UTF8(layer));
        }
        vkInstanceCreateInfo.ppEnabledLayerNames(validationLayers.flip());
    }

    public void createDebugUtilsMessengerEXT(VkInstance vkInstance) {
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            LongBuffer longBuffer = memoryStack.callocLong(1);
            int result = EXTDebugUtils.vkCreateDebugUtilsMessengerEXT(vkInstance,
                    debugUtilsMessengerCreateInfo, null, longBuffer);
            if (result != VK13.VK_SUCCESS) {
                throw new RuntimeException(String.format("creating debug utils messenger failed error code %d", result));
            }
            pDebugUtilsMessengerEXT = longBuffer.get();
        }
    }

    @Override
    public void free() {
        EXTDebugUtils.vkDestroyDebugUtilsMessengerEXT(vkInstance, pDebugUtilsMessengerEXT, null);
        super.free();
        debugUtilsMessengerCreateInfo.free();
        callback.free();
    }

    private void validateValidationLayers(List<String> requestedValidationLayers) {
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
            System.out.printf("%d available layers%n", layerCount.get(0));
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
}
