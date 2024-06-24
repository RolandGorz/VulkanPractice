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
import java.util.List;

public class Devices {
    public void getPhysicalDevices(VkInstance vkInstance) {
        List<VkPhysicalDevice> vkPhysicalDeviceList = new ArrayList<>();
        try(MemoryStack memoryStack = MemoryStack.stackPush()) {
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
        for (VkPhysicalDevice vkPhysicalDevice : vkPhysicalDeviceList) {
            VkPhysicalDeviceProperties vkPhysicalDeviceProperties = VkPhysicalDeviceProperties.calloc();
            VkPhysicalDeviceFeatures vkPhysicalDeviceFeatures = VkPhysicalDeviceFeatures.calloc();
            VK13.vkGetPhysicalDeviceProperties(vkPhysicalDevice, vkPhysicalDeviceProperties);
            VK13.vkGetPhysicalDeviceFeatures(vkPhysicalDevice, vkPhysicalDeviceFeatures);
            System.out.printf("physical device \"%s\" geometry shader availability: %b%n",
                    vkPhysicalDeviceProperties.deviceNameString(), vkPhysicalDeviceFeatures.geometryShader());
        }
    }
}
