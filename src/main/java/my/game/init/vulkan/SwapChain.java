package my.game.init.vulkan;

import com.google.common.collect.ImmutableList;
import my.game.init.vulkan.devices.logical.LogicalDevice;
import my.game.init.vulkan.devices.physical.QueueFamilyIndexes;
import my.game.init.vulkan.devices.physical.SwapChainSupportDetails;
import my.game.init.window.WindowHandle;
import my.game.init.window.WindowSurface;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.List;

public class SwapChain {

    private final VkDevice device;
    private final long swapChainPointer;
    private final List<Long> swapChainImages;
    private final VkSurfaceFormatKHR swapChainImageFormat;
    private final VkExtent2D swapChainExtent;

    public SwapChain(LogicalDevice logicalDevice, WindowHandle windowHandle, WindowSurface windowSurface) {
        device = logicalDevice.getLogicalDeviceInformation().vkDevice();
        SwapChainSupportDetails swapChainSupportDetails = logicalDevice.getLogicalDeviceInformation().validPhysicalDevice().physicalDeviceInformation().swapChainSupportDetails();
        swapChainImageFormat = chooseSwapSurfaceFormat(swapChainSupportDetails.formats());
        int presentMode = choosePresentMode(swapChainSupportDetails.presentModes());
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            swapChainExtent = chooseSwapExtent(swapChainSupportDetails.capabilities(),
                    windowHandle, memoryStack);
            int imageCount = swapChainSupportDetails.capabilities().minImageCount() + 1;
            //We should also make sure to not exceed the maximum number of images while doing this, where 0 is a special value that means that there is no maximum:
            if (swapChainSupportDetails.capabilities().maxImageCount() > 0 && imageCount > swapChainSupportDetails.capabilities().maxImageCount()) {
                imageCount = swapChainSupportDetails.capabilities().maxImageCount();
            }
            VkSwapchainCreateInfoKHR vkSwapchainCreateInfoKHR = VkSwapchainCreateInfoKHR.calloc(memoryStack);
            vkSwapchainCreateInfoKHR
                    .sType(KHRSwapchain.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                    .surface(windowSurface.getWindowSurfaceHandle())
                    .minImageCount(imageCount)
                    .imageFormat(swapChainImageFormat.format())
                    .imageColorSpace(swapChainImageFormat.colorSpace())
                    .imageExtent(swapChainExtent)
                    .imageArrayLayers(1)
                    .imageUsage(VK13.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);
            QueueFamilyIndexes queueFamilyIndexes = logicalDevice.getLogicalDeviceInformation().validPhysicalDevice().physicalDeviceInformation().queueFamilyIndexes();
            if (queueFamilyIndexes.graphicsQueueFamilyIndex() != queueFamilyIndexes.presentationQueueFamilyIndex()) {
                IntBuffer queueFamilyIndexesPointer = memoryStack.mallocInt(2);
                queueFamilyIndexesPointer.put(queueFamilyIndexes.graphicsQueueFamilyIndex());
                queueFamilyIndexesPointer.put(queueFamilyIndexes.presentationQueueFamilyIndex());
                queueFamilyIndexesPointer.flip();
                vkSwapchainCreateInfoKHR
                        .imageSharingMode(VK13.VK_SHARING_MODE_CONCURRENT)
                        .queueFamilyIndexCount(2)
                        .pQueueFamilyIndices(queueFamilyIndexesPointer);
            } else {
                vkSwapchainCreateInfoKHR
                        .imageSharingMode(VK13.VK_SHARING_MODE_EXCLUSIVE)
                        .queueFamilyIndexCount(0)
                        .pQueueFamilyIndices(null);
            }
            vkSwapchainCreateInfoKHR
                    .preTransform(swapChainSupportDetails.capabilities().currentTransform())
                    .compositeAlpha(KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                    .presentMode(presentMode)
                    .clipped(true)
                    .oldSwapchain(VK13.VK_NULL_HANDLE);
            LongBuffer swapChainPointerBuffer = memoryStack.mallocLong(1);
            int result = KHRSwapchain.vkCreateSwapchainKHR(
                    logicalDevice.getLogicalDeviceInformation().vkDevice(),
                    vkSwapchainCreateInfoKHR,
                    null,
                    swapChainPointerBuffer);
            if (result != VK13.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to create swap chain. Error code: %d", result));
            }
            swapChainPointer = swapChainPointerBuffer.get(0);
        }
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            IntBuffer imageCount = memoryStack.mallocInt(1);
            int result = KHRSwapchain.vkGetSwapchainImagesKHR(device, swapChainPointer, imageCount, null);
            if (result != VK13.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to get count of swap chain images. Error code: %d", result));
            }
            LongBuffer imagePointers = memoryStack.mallocLong(imageCount.get(0));
            int result2 = KHRSwapchain.vkGetSwapchainImagesKHR(device, swapChainPointer, imageCount, imagePointers);
            if (result2 != VK13.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to get swap chain images. Error code: %d", result2));
            }
            ImmutableList.Builder<Long> builder = ImmutableList.<Long>builder();
            for (int i = 0; i < imagePointers.capacity(); ++i) {
                builder.add(imagePointers.get(i));
            }
            swapChainImages = builder.build();
        }
    }

    public VkDevice getDevice() {
        return device;
    }

    public long getSwapChainPointer() {
        return swapChainPointer;
    }

    public List<Long> getSwapChainImages() {
        return swapChainImages;
    }

    public VkSurfaceFormatKHR getSwapChainImageFormat() {
        return swapChainImageFormat;
    }

    public VkExtent2D getSwapChainExtent() {
        return swapChainExtent;
    }

    public void free() {
        KHRSwapchain.vkDestroySwapchainKHR(device, swapChainPointer, null);
        swapChainExtent.free();
    }

    private VkSurfaceFormatKHR chooseSwapSurfaceFormat(VkSurfaceFormatKHR.Buffer formats) {
        for (int i = 0; i < formats.capacity(); ++i) {
            VkSurfaceFormatKHR curr = formats.get(i);
            if (curr.format() == VK13.VK_FORMAT_B8G8R8A8_SRGB && curr.colorSpace() == KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) {
                return curr;
            }
        }
        return formats.get(0);
    }

    private int choosePresentMode(IntBuffer presentModes) {
        for (int i = 0; i < presentModes.capacity(); ++i) {
            int curr = presentModes.get(i);
            if (curr == KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR) {
                return curr;
            }
        }
        return KHRSurface.VK_PRESENT_MODE_FIFO_KHR;
    }

    private VkExtent2D chooseSwapExtent(VkSurfaceCapabilitiesKHR capabilities, WindowHandle windowHandle, MemoryStack memoryStack) {
        VkExtent2D vkExtent2D = VkExtent2D.malloc();
        if (capabilities.currentExtent().width() != VulkanUtil.UINT32_MAX) {
            vkExtent2D.set(
                    capabilities.currentExtent().width(),
                    capabilities.currentExtent().height());
            return vkExtent2D;
        }
        IntBuffer width = memoryStack.mallocInt(1);
        IntBuffer height = memoryStack.mallocInt(1);
        GLFW.glfwGetFramebufferSize(windowHandle.getWindowHandlePointer(), width, height);

        vkExtent2D.set(
                Math.clamp(width.get(0), capabilities.minImageExtent().width(), capabilities.maxImageExtent().width()),
                Math.clamp(height.get(0), capabilities.minImageExtent().height(), capabilities.maxImageExtent().height()));
        return vkExtent2D;
    }

}
