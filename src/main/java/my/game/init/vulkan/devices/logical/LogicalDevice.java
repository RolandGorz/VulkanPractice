package my.game.init.vulkan.devices.logical;

import my.game.init.vulkan.devices.logical.queue.GraphicsQueue;
import my.game.init.vulkan.devices.logical.queue.PresentationQueue;
import my.game.init.vulkan.devices.logical.queue.TransferVulkanQueue;
import my.game.init.vulkan.devices.physical.PhysicalDeviceRetriever;
import org.immutables.value.Value;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures2;

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
                info.sType(VK10.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
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
            VkPhysicalDeviceFeatures2 vkPhysicalDeviceFeatures = VkPhysicalDeviceFeatures2.calloc(memoryStack);
            vkPhysicalDeviceFeatures.sType(VK11.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2);
            vkPhysicalDeviceFeatures.pNext(physicalDevice().physicalDeviceInformation().uniformBufferStandardLayoutFeatures());
            VkDeviceCreateInfo vkDeviceCreateInfo = VkDeviceCreateInfo.calloc(memoryStack);
            vkDeviceCreateInfo
                    .sType(VK10.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                    .pQueueCreateInfos(vkDeviceQueueCreateInfos)
                    .ppEnabledExtensionNames(requiredDeviceExtensions)
                    //If you already have your chain defined then use the pNext that takes a long as a parameter. The pNext that takes an object
                    //will set the Pnext of what you pass in to be the current value of pNext. The intention is that you do
                    //pNext(A).pNext(B).pNext(C) which results in a chain of C->B->A. This is because adding to the chain is an O(1)
                    //operation but this also leads to a problem where if you add an entire chain of your own then it just takes the head of
                    //your chain and adds it to the existing chain. If you use just the method with the long param then it will set the Pnext variable
                    //to whatever you give it without trying to help.
                    .pNext(vkPhysicalDeviceFeatures.address());
            PointerBuffer logicalDevice = memoryStack.mallocPointer(1);
            int result = VK10.vkCreateDevice(physicalDevice().physicalDeviceInformation().physicalDevice(), vkDeviceCreateInfo, null, logicalDevice);
            if (result != VK10.VK_SUCCESS) {
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

    @Value.Derived
    public TransferVulkanQueue transferVulkanQueue() {
        return new TransferVulkanQueue(physicalDevice().physicalDeviceInformation().transferQueueIndex(), vkDevice());
    }

    public void free() {
        VK10.vkDestroyDevice(vkDevice(), null);
    }
}
