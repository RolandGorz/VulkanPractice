package my.game.init.vulkan.devices.physical;

import com.google.common.collect.ImmutableList;
import my.game.init.window.WindowSurface;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import org.lwjgl.vulkan.VkQueueFamilyProperties;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class PhysicalDeviceRetriever {

    public static List<String> REQUIRED_DEVICE_EXTENSIONS = ImmutableList.of(
            KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME
    );
    //We only include these extensions if they are available. In the case of VK_KHR_portability_subset if it is available then it is required.
    public static List<String> OPTIONAL_DEVICE_EXTENSIONS = ImmutableList.of(
            "VK_KHR_portability_subset"
    );
    private PhysicalDeviceInformation physicalDeviceInformation;

    public PhysicalDeviceRetriever(final VkInstance vkInstance, final WindowSurface windowSurface) {
        PriorityQueue<PhysicalDeviceInformation> priorityQueue = getDevices(vkInstance, windowSurface);
        while (!priorityQueue.isEmpty()) {
            PhysicalDeviceInformation curr = priorityQueue.poll();
            if (curr.isValid()) {
                physicalDeviceInformation = curr;
                break;
            }
            curr.free();
        }
        while (!priorityQueue.isEmpty()) {
            priorityQueue.poll().free();
        }
        if (physicalDeviceInformation == null) {
            throw new RuntimeException("No device found that is capable of rendering anything with. We give up");
        }
    }

    public PhysicalDeviceInformation physicalDeviceInformation() {
        return physicalDeviceInformation;
    }

    public void free() {
        physicalDeviceInformation.free();
    }

    private PriorityQueue<PhysicalDeviceInformation> getDevices(VkInstance vkInstance, WindowSurface windowSurface) {
        List<VkPhysicalDevice> vkPhysicalDeviceList = new ArrayList<>();
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            IntBuffer deviceCount = memoryStack.mallocInt(1);
            int result = VK10.vkEnumeratePhysicalDevices(vkInstance, deviceCount, null);
            if (result == VK10.VK_SUCCESS) {
                System.out.println("vkEnumeratePhysicalDevices returned success");
            } else {
                System.out.printf("vkEnumeratePhysicalDevices returned failure code %d%n", result);
            }
            System.out.printf("%d physical devices%n", deviceCount.get(0));
            PointerBuffer devicesPointer = memoryStack.mallocPointer(deviceCount.get(0));
            int result2 = VK10.vkEnumeratePhysicalDevices(vkInstance, deviceCount, devicesPointer);
            if (result2 == VK10.VK_SUCCESS) {
                System.out.println("vkEnumeratePhysicalDevices returned success");
            } else {
                System.out.printf("vkEnumeratePhysicalDevices returned failure code %d%n", result2);
            }
            for (int i = 0; i < devicesPointer.capacity(); ++i) {
                vkPhysicalDeviceList.add(new VkPhysicalDevice(devicesPointer.get(i), vkInstance));
            }
        }
        PriorityQueue<PhysicalDeviceInformation> priorityQueue = new PriorityQueue<>(Comparator.reverseOrder());
        for (VkPhysicalDevice vkPhysicalDevice : vkPhysicalDeviceList) {
            PhysicalDeviceInformation curr = determineDeviceSuitability(vkPhysicalDevice, windowSurface);
            if (curr.score() > 0) {
                priorityQueue.add(curr);
            }
        }
        return priorityQueue;
    }

    private PhysicalDeviceInformation determineDeviceSuitability(VkPhysicalDevice vkPhysicalDevice, WindowSurface windowSurface) {
        int score = 0;
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            VkPhysicalDeviceProperties vkPhysicalDeviceProperties = VkPhysicalDeviceProperties.malloc(memoryStack);
            VkPhysicalDeviceFeatures vkPhysicalDeviceFeatures = VkPhysicalDeviceFeatures.malloc(memoryStack);
            VK10.vkGetPhysicalDeviceProperties(vkPhysicalDevice, vkPhysicalDeviceProperties);
            VK10.vkGetPhysicalDeviceFeatures(vkPhysicalDevice, vkPhysicalDeviceFeatures);
            System.out.printf("physical device \"%s\" geometry shader availability: %b%n",
                    vkPhysicalDeviceProperties.deviceNameString(), vkPhysicalDeviceFeatures.geometryShader());
            // Discrete GPUs have a significant performance advantage
            if (vkPhysicalDeviceProperties.deviceType() == VK10.VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU) {
                score += 1000;
            }
            // Maximum possible size of textures affects graphics quality
            score += vkPhysicalDeviceProperties.limits().maxImageDimension2D();
        }
        ImmutablePhysicalDeviceInformation.Builder builder = ImmutablePhysicalDeviceInformation.builder();
        builder.physicalDevice(vkPhysicalDevice)
                .score(score)
                .windowSurface(windowSurface);
        getFamilyIndexes(vkPhysicalDevice, windowSurface, builder);
        return builder.build();
    }

    private void getFamilyIndexes(VkPhysicalDevice physicalDevice, WindowSurface windowSurface, ImmutablePhysicalDeviceInformation.Builder builder) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer queueFamilyCount = stack.mallocInt(1);
            VK10.vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCount, null);
            VkQueueFamilyProperties.Buffer queueFamilyProperties = VkQueueFamilyProperties.malloc(queueFamilyCount.get(0), stack);
            VK10.vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCount, queueFamilyProperties);
            boolean foundSharedPresentationGraphicsQueue = false;
            boolean foundDedicatedTransferQueue = false;
            for (int i = 0; i < queueFamilyProperties.capacity(); ++i) {
                if (!foundSharedPresentationGraphicsQueue) {
                    foundSharedPresentationGraphicsQueue = queueSupportsGraphics(queueFamilyProperties, i, builder)
                            && queueSupportsPresentation(stack, physicalDevice, i, windowSurface, builder);
                }
                if (!foundDedicatedTransferQueue) {
                    foundDedicatedTransferQueue = queueIsDedicatedForTransfer(queueFamilyProperties, i, builder);
                }
                if (foundSharedPresentationGraphicsQueue && foundDedicatedTransferQueue) {
                    break;
                }
            }
        }
    }

    private boolean queueSupportsGraphics(VkQueueFamilyProperties.Buffer queueFamilyProperties, int index, ImmutablePhysicalDeviceInformation.Builder builder) {
        if ((queueFamilyProperties.get(index).queueFlags() & VK10.VK_QUEUE_GRAPHICS_BIT) == VK10.VK_QUEUE_GRAPHICS_BIT) {
            builder.graphicsQueueIndex(index);
            return true;
        }
        return false;
    }

    private boolean queueSupportsPresentation(MemoryStack stack, VkPhysicalDevice physicalDevice, int index, WindowSurface windowSurface, ImmutablePhysicalDeviceInformation.Builder builder) {
        IntBuffer presentationSupported = stack.mallocInt(1);
        int result = KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(physicalDevice, index, windowSurface.getWindowSurfaceHandle(), presentationSupported);
        if (result != VK10.VK_SUCCESS) {
            throw new IllegalStateException(String.format("Error occurred when trying to determine Physical Device Surface Support. Error code : %d", result));
        }
        if (presentationSupported.get(0) == VK10.VK_TRUE) {
            builder.presentationQueueIndex(index);
            return true;
        }
        return false;
    }

    private boolean queueIsDedicatedForTransfer(VkQueueFamilyProperties.Buffer queueFamilyProperties, int index, ImmutablePhysicalDeviceInformation.Builder builder) {
        if (((queueFamilyProperties.get(index).queueFlags() & VK10.VK_QUEUE_GRAPHICS_BIT) != VK10.VK_QUEUE_GRAPHICS_BIT) &&
                ((queueFamilyProperties.get(index).queueFlags() & VK10.VK_QUEUE_TRANSFER_BIT) == VK10.VK_QUEUE_TRANSFER_BIT)) {
            builder.transferQueueIndex(index);
            return true;
        }
        return false;
    }
}
