package my.game.init.vulkan.devices.logical;

import my.game.init.vulkan.devices.physical.PhysicalDeviceInformation;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;

import java.nio.FloatBuffer;

public class LogicalDevice {
    private final LogicalDeviceInformation logicalDeviceInformation;
    public LogicalDevice(PhysicalDeviceInformation physicalDeviceInformation) {
        if (physicalDeviceInformation.graphicsQueueFamilyIndex().isEmpty()) {
            throw new IllegalStateException(String.format("PhysicalDeviceInformation does not have a graphics queue " +
                    "family associated with it. %s", physicalDeviceInformation));
        }
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            VkDeviceQueueCreateInfo.Buffer vkDeviceQueueCreateInfo = VkDeviceQueueCreateInfo.calloc(1, memoryStack);
            VkPhysicalDeviceFeatures vkPhysicalDeviceFeatures = VkPhysicalDeviceFeatures.calloc(memoryStack);
            //We only want 1 queue, so we are allocating 1 float in the buffer and then setting it as top priority.
            //The quantity of pQueuePriorities is required to be equal to the number of queues.
            // This is why we don't need to and are unable to set the queueCount in vkDeviceQueueCreateInfo.
            FloatBuffer queuePriorities = memoryStack.mallocFloat(1);
            queuePriorities.put(1.0F);
            queuePriorities.rewind();
            vkDeviceQueueCreateInfo
                    .sType(VK13.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                    .queueFamilyIndex(physicalDeviceInformation.graphicsQueueFamilyIndex().get())
                    .pQueuePriorities(queuePriorities);
            VkDeviceCreateInfo vkDeviceCreateInfo = VkDeviceCreateInfo.calloc(memoryStack);
            vkDeviceCreateInfo
                    .sType(VK13.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                    .pQueueCreateInfos(vkDeviceQueueCreateInfo)
                    .pEnabledFeatures(vkPhysicalDeviceFeatures);
            PointerBuffer logicalDevice = memoryStack.mallocPointer(1);
            int result = VK13.vkCreateDevice(physicalDeviceInformation.physicalDevice(), vkDeviceCreateInfo, null, logicalDevice);
            if (result != VK13.VK_SUCCESS) {
                throw new RuntimeException(String.format("Failed to create device. Error code: %s", result));
            }
            VkDevice vkDevice =  new VkDevice(logicalDevice.get(), physicalDeviceInformation.physicalDevice(), vkDeviceCreateInfo);
            logicalDeviceInformation = new LogicalDeviceInformation(physicalDeviceInformation, vkDevice);
        }
    }

    public LogicalDeviceInformation getLogicalDeviceInformation() {
        return logicalDeviceInformation;
    }

    public void free() {
        VK13.vkDestroyDevice(logicalDeviceInformation.vkDevice(), null);
    }
}
