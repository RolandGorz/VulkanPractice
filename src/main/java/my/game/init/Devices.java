package my.game.init;

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
import java.util.Optional;
import java.util.PriorityQueue;

public class Devices {

    public PriorityQueue<PhysicalDeviceInformation> getPhysicalDevices(VkInstance vkInstance) {
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
            PhysicalDeviceInformation curr = determineDeviceSuitability(vkPhysicalDevice);
            if (curr.score() > 0) {
                priorityQueue.add(curr);
            }
        }
        return priorityQueue;
    }

    public PhysicalDeviceInformation determineDeviceSuitability(VkPhysicalDevice vkPhysicalDevice) {
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
            // Application can't function without geometry shaders
            if (!vkPhysicalDeviceFeatures.geometryShader()) {
                score = 0;
            }
        }
        QueueFamily queueFamily = QueueFamily.getInstance();
        return new PhysicalDeviceInformation(vkPhysicalDevice, score, queueFamily.getGraphicsFamilyIndex(vkPhysicalDevice));
    }

    public record PhysicalDeviceInformation(
            VkPhysicalDevice physicalDevice, int score, Optional<Integer> graphicsQueueFamilyIndex
    ) implements Comparable<PhysicalDeviceInformation> {

        @Override
        public int compareTo(PhysicalDeviceInformation o) {
            return this.score - o.score;
        }
    }
}
