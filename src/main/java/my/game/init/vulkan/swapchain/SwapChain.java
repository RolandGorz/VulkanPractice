package my.game.init.vulkan.swapchain;

import my.game.init.vulkan.VulkanUtil;
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
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

public class SwapChain {

    private final LogicalDevice device;
    private final WindowHandle windowHandle;
    private final WindowSurface windowSurface;
    private final long swapChainPointer;
    private final VkSurfaceFormatKHR surfaceFormat;

    public SwapChain(LogicalDevice device, WindowHandle windowHandle, WindowSurface windowSurface) {
        this.device = device;
        this.windowHandle = windowHandle;
        this.windowSurface = windowSurface;
        SwapChainSupportDetails swapChainSupportDetails = this.device.getLogicalDeviceInformation().validPhysicalDevice().physicalDeviceInformation().swapChainSupportDetails();
        surfaceFormat = chooseSwapSurfaceFormat(swapChainSupportDetails.formats());
        int presentMode = choosePresentMode(swapChainSupportDetails.presentModes());
        swapChainPointer = createSwapChain(swapChainSupportDetails, this.windowSurface, this.device, presentMode);
    }

    private long createSwapChain(SwapChainSupportDetails swapChainSupportDetails, WindowSurface windowSurface, LogicalDevice logicalDevice, int presentMode) {
        VkExtent2D swapChainExtent = chooseSwapExtent(swapChainSupportDetails.capabilities(),
                this.windowHandle);
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
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
                    .imageFormat(surfaceFormat.format())
                    .imageColorSpace(surfaceFormat.colorSpace())
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
            return swapChainPointerBuffer.get(0);
        } finally {
            swapChainExtent.free();
            device.getLogicalDeviceInformation().validPhysicalDevice().physicalDeviceInformation().swapChainSupportDetails().free();
        }
    }

    public LogicalDevice getLogicalDevice() {
        return device;
    }

    public WindowHandle getWindowHandle() {
        return windowHandle;
    }

    public WindowSurface getWindowSurface() {
        return windowSurface;
    }

    public Long getSwapChainPointer() {
        return swapChainPointer;
    }

    public VkSurfaceFormatKHR getSurfaceFormat() {
        return surfaceFormat;
    }

    public void free() {
        KHRSwapchain.vkDestroySwapchainKHR(device.getLogicalDeviceInformation().vkDevice(), swapChainPointer, null);
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

    private VkExtent2D chooseSwapExtent(VkSurfaceCapabilitiesKHR capabilities, WindowHandle windowHandle) {
        VkExtent2D vkExtent2D = VkExtent2D.malloc();
        if (capabilities.currentExtent().width() != VulkanUtil.UINT32_MAX) {
            vkExtent2D.set(
                    capabilities.currentExtent().width(),
                    capabilities.currentExtent().height());
            return vkExtent2D;
        }
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            IntBuffer width = memoryStack.mallocInt(1);
            IntBuffer height = memoryStack.mallocInt(1);
            GLFW.glfwGetFramebufferSize(windowHandle.getWindowHandlePointer(), width, height);

            vkExtent2D.set(
                    Math.clamp(width.get(0), capabilities.minImageExtent().width(), capabilities.maxImageExtent().width()),
                    Math.clamp(height.get(0), capabilities.minImageExtent().height(), capabilities.maxImageExtent().height()));
            return vkExtent2D;
        }
    }

}
