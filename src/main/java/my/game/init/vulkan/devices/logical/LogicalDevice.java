package my.game.init.vulkan.devices.logical;

import my.game.init.vulkan.devices.logical.queue.GraphicsQueue;
import my.game.init.vulkan.devices.logical.queue.PresentationQueue;
import my.game.init.vulkan.devices.physical.PhysicalDeviceRetriever;
import org.immutables.value.Value;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;

import java.nio.FloatBuffer;
import java.util.Set;

@Value.Immutable
@Value.Style(strictBuilder = true)
public abstract class LogicalDevice {
    abstract PhysicalDeviceRetriever physicalDevice();

    @Value.Derived
    public VkDevice vkDevice() {
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            //We only want 1 queue, so we are allocating 1 float in the buffer and then setting it as top priority.
            //The quantity of pQueuePriorities is required to be equal to the number of queues.
            // This is why we don't need to and are unable to set the queueCount in vkDeviceQueueCreateInfo.
            FloatBuffer queuePriorities = memoryStack.mallocFloat(1);
            queuePriorities.put(1.0F);
            queuePriorities.flip();
            Set<Integer> uniqueIndexes = physicalDevice().physicalDeviceInformation().uniqueQueueIndexes();
            VkDeviceQueueCreateInfo.Buffer vkDeviceQueueCreateInfos = VkDeviceQueueCreateInfo.calloc(uniqueIndexes.size(), memoryStack);
            for (Integer index : uniqueIndexes) {
                VkDeviceQueueCreateInfo.Buffer info = VkDeviceQueueCreateInfo.calloc(1, memoryStack);
                info.sType(VK13.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                        .queueFamilyIndex(index)
                        .pQueuePriorities(queuePriorities);
                vkDeviceQueueCreateInfos.put(info);
            }
            vkDeviceQueueCreateInfos.flip();
            PointerBuffer requiredDeviceExtensions = memoryStack.callocPointer(PhysicalDeviceRetriever.REQUIRED_DEVICE_EXTENSIONS.size() + PhysicalDeviceRetriever.OPTIONAL_DEVICE_EXTENSIONS.size());
            for (String x : PhysicalDeviceRetriever.REQUIRED_DEVICE_EXTENSIONS) {
                requiredDeviceExtensions.put(memoryStack.UTF8(x));
            }
            for (String x : PhysicalDeviceRetriever.OPTIONAL_DEVICE_EXTENSIONS) {
                if (physicalDevice().physicalDeviceInformation().supportedExtensions().contains(x)) {
                    requiredDeviceExtensions.put(memoryStack.UTF8(x));
                }
            }
            requiredDeviceExtensions.flip();
            VkPhysicalDeviceFeatures vkPhysicalDeviceFeatures = VkPhysicalDeviceFeatures.calloc(memoryStack);
            VkDeviceCreateInfo vkDeviceCreateInfo = VkDeviceCreateInfo.calloc(memoryStack);
            vkDeviceCreateInfo
                    .sType(VK13.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                    .pQueueCreateInfos(vkDeviceQueueCreateInfos)
                    .pEnabledFeatures(vkPhysicalDeviceFeatures)
                    .ppEnabledExtensionNames(requiredDeviceExtensions);
            PointerBuffer logicalDevice = memoryStack.mallocPointer(1);
            int result = VK13.vkCreateDevice(physicalDevice().physicalDeviceInformation().physicalDevice(), vkDeviceCreateInfo, null, logicalDevice);
            if (result != VK13.VK_SUCCESS) {
                throw new RuntimeException(String.format("Failed to create device. Error code: %s", result));
            }
            return new VkDevice(logicalDevice.get(), physicalDevice().physicalDeviceInformation().physicalDevice(), vkDeviceCreateInfo);
        }
    }

    @Value.Derived
    public GraphicsQueue graphicsQueue() {
        return new GraphicsQueue(physicalDevice().physicalDeviceInformation().graphicsQueueIndex(), vkDevice());
    }

    @Value.Derived
    public PresentationQueue presentationQueue() {
        return new PresentationQueue(physicalDevice().physicalDeviceInformation().presentationQueueIndex(), vkDevice());
    }

    public void free() {
        VK13.vkDestroyDevice(vkDevice(), null);
    }
}
