package my.game.init.vulkan.devices.physical;

import my.game.init.window.WindowSurface;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class PhysicalDevices {

    public ValidPhysicalDevice getValidPhysicalDevice(VkInstance vkInstance, WindowSurface windowSurface) {
        List<VkPhysicalDevice> vkPhysicalDeviceList = new ArrayList<>();
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            IntBuffer deviceCount = memoryStack.mallocInt(1);
            int result = VK13.vkEnumeratePhysicalDevices(vkInstance, deviceCount, null);
            if (result == VK13.VK_SUCCESS) {
                System.out.println("vkEnumeratePhysicalDevices returned success");
            } else {
                System.out.printf("vkEnumeratePhysicalDevices returned failure code %d%n", result);
            }
            System.out.printf("%d physical devices%n", deviceCount.get(0));
            PointerBuffer devicesPointer = memoryStack.mallocPointer(deviceCount.get(0));
            int result2 = VK13.vkEnumeratePhysicalDevices(vkInstance, deviceCount, devicesPointer);
            if (result2 == VK13.VK_SUCCESS) {
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
        return new ValidPhysicalDevice(choosePhysicalDevice(priorityQueue));
    }

    private PhysicalDeviceInformation choosePhysicalDevice(PriorityQueue<PhysicalDeviceInformation> devices) {
        while (!devices.isEmpty()) {
            PhysicalDeviceInformation curr = devices.poll();
            if (curr.score() != 0 && curr.queueFamilyIndexes().isComplete() && curr.requiredDeviceExtensionsSupported()) {
                return curr;
            }
        }
        throw new RuntimeException("No device found that is capable of rendering anything with. We give up");
    }

    public PhysicalDeviceInformation determineDeviceSuitability(VkPhysicalDevice vkPhysicalDevice, WindowSurface windowSurface) {
        int score = 0;
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            VkPhysicalDeviceProperties vkPhysicalDeviceProperties = VkPhysicalDeviceProperties.malloc(memoryStack);
            VkPhysicalDeviceFeatures vkPhysicalDeviceFeatures = VkPhysicalDeviceFeatures.malloc(memoryStack);
            VK13.vkGetPhysicalDeviceProperties(vkPhysicalDevice, vkPhysicalDeviceProperties);
            VK13.vkGetPhysicalDeviceFeatures(vkPhysicalDevice, vkPhysicalDeviceFeatures);
            System.out.printf("physical device \"%s\" geometry shader availability: %b%n",
                    vkPhysicalDeviceProperties.deviceNameString(), vkPhysicalDeviceFeatures.geometryShader());
            // Discrete GPUs have a significant performance advantage
            if (vkPhysicalDeviceProperties.deviceType() == VK13.VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU) {
                score += 1000;
            }
            // Maximum possible size of textures affects graphics quality
            score += vkPhysicalDeviceProperties.limits().maxImageDimension2D();
        }
        DeviceQueueFamily queueFamily = DeviceQueueFamily.getInstance();
        return ImmutablePhysicalDeviceInformation.builder()
                .physicalDevice(vkPhysicalDevice)
                .score(score)
                .queueFamilyIndexes(queueFamily.getFamilyIndexes(vkPhysicalDevice, windowSurface))
                .build();
    }
}
