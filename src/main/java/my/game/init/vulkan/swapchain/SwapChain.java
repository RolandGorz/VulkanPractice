package my.game.init.vulkan.swapchain;

import my.game.init.vulkan.VulkanUtil;
import my.game.init.vulkan.devices.physical.PhysicalDeviceInformation;
import my.game.init.vulkan.devices.physical.SwapChainSupportDetails;
import my.game.init.window.WindowHandle;
import my.game.init.window.WindowSurface;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

public class SwapChain {
    private final VkDevice device;
    private final long swapChainPointer;
    private final VkSurfaceFormatKHR surfaceFormat;
    private final VkExtent2D swapChainExtent;

    public SwapChain(VkDevice device, PhysicalDeviceInformation physicalDeviceInformation, WindowHandle windowHandle, WindowSurface windowSurface, long oldSwapChainHandle) {
        this.device = device;
        SwapChainSupportDetails swapChainSupportDetails = physicalDeviceInformation.swapChainSupportDetails();
        surfaceFormat = chooseSwapSurfaceFormat(swapChainSupportDetails.formats());
        int presentMode = choosePresentMode(swapChainSupportDetails.presentModes());
        swapChainExtent = chooseSwapExtent(swapChainSupportDetails.capabilities(), windowHandle);
        swapChainPointer = createSwapChain(swapChainSupportDetails, windowSurface, physicalDeviceInformation, presentMode, oldSwapChainHandle);
    }

    private long createSwapChain(SwapChainSupportDetails swapChainSupportDetails, WindowSurface windowSurface, PhysicalDeviceInformation physicalDeviceInformation, int presentMode, long oldSwapChainHandle) {
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
                    .imageUsage(VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);
            if (physicalDeviceInformation.uniqueQueueIndexes().size() > 1) {
                IntBuffer queueFamilyIndexesPointer = memoryStack.mallocInt(physicalDeviceInformation.uniqueQueueIndexes().size());
                for (Integer x : physicalDeviceInformation.uniqueQueueIndexes()) {
                    queueFamilyIndexesPointer.put(x);
                }
                queueFamilyIndexesPointer.flip();
                vkSwapchainCreateInfoKHR
                        .imageSharingMode(VK10.VK_SHARING_MODE_CONCURRENT)
                        .pQueueFamilyIndices(queueFamilyIndexesPointer);
            } else {
                vkSwapchainCreateInfoKHR
                        .imageSharingMode(VK10.VK_SHARING_MODE_EXCLUSIVE)
                        .pQueueFamilyIndices(null);
            }
            vkSwapchainCreateInfoKHR
                    .preTransform(swapChainSupportDetails.capabilities().currentTransform())
                    .compositeAlpha(KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                    .presentMode(presentMode)
                    .clipped(true)
                    .oldSwapchain(oldSwapChainHandle);
            LongBuffer swapChainPointerBuffer = memoryStack.mallocLong(1);
            int result = KHRSwapchain.vkCreateSwapchainKHR(
                    device,
                    vkSwapchainCreateInfoKHR,
                    null,
                    swapChainPointerBuffer);
            KHRSwapchain.vkDestroySwapchainKHR(device, oldSwapChainHandle, null);
            if (result != VK10.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to create swap chain. Error code: %d", result));
            }
            return swapChainPointerBuffer.get(0);
        }
    }

    public Long getSwapChainPointer() {
        return swapChainPointer;
    }

    public VkSurfaceFormatKHR getSurfaceFormat() {
        return surfaceFormat;
    }

    public VkExtent2D getSwapChainExtent() {
        return swapChainExtent;
    }

    private VkSurfaceFormatKHR chooseSwapSurfaceFormat(VkSurfaceFormatKHR.Buffer formats) {
        for (int i = 0; i < formats.capacity(); ++i) {
            VkSurfaceFormatKHR curr = formats.get(i);
            if (curr.format() == VK10.VK_FORMAT_B8G8R8A8_SRGB && curr.colorSpace() == KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) {
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
        //Vulkan tells us to match the resolution of the window by setting the width and height in the currentExtent member.
        // However, some window managers do allow us to differ here and this is indicated by setting the width and height
        // in currentExtent to a special value: the maximum value of uint32_t. In that case we’ll pick the resolution that
        // best matches the window within the minImageExtent and maxImageExtent bounds. But we must specify the resolution
        // in the correct unit.
        //
        //GLFW uses two units when measuring sizes: pixels and screen coordinates. For example, the resolution {WIDTH, HEIGHT}
        // that we specified earlier when creating the window is measured in screen coordinates. But Vulkan works with pixels,
        // so the swap chain extent must be specified in pixels as well. Unfortunately, if you are using a high DPI display
        // (like Apple’s Retina display), screen coordinates don’t correspond to pixels. Instead, due to the higher pixel density,
        // the resolution of the window in pixel will be larger than the resolution in screen coordinates.
        // So if Vulkan doesn’t fix the swap extent for us, we can’t just use the original {WIDTH, HEIGHT}. Instead, we must use
        // glfwGetFramebufferSize to query the resolution of the window in pixel before matching it against the minimum and
        // maximum image extent.
        VkExtent2D vkExtent2D = VkExtent2D.malloc();
        if (capabilities.currentExtent().width() != VulkanUtil.UINT32_MAX) {
            vkExtent2D
                    .width(capabilities.currentExtent().width())
                    .height(capabilities.currentExtent().height());
            return vkExtent2D;
        }
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            IntBuffer width = memoryStack.mallocInt(1);
            IntBuffer height = memoryStack.mallocInt(1);
            GLFW.glfwGetFramebufferSize(windowHandle.getWindowHandlePointer(), width, height);

            vkExtent2D
                    .width(Math.clamp(width.get(0), capabilities.minImageExtent().width(), capabilities.maxImageExtent().width()))
                    .height(Math.clamp(height.get(0), capabilities.minImageExtent().height(), capabilities.maxImageExtent().height()));
            return vkExtent2D;
        }
    }

    public void freeSwapChainExtent() {
        swapChainExtent.free();
    }

    public void freeSwapChainPointer() {
        KHRSwapchain.vkDestroySwapchainKHR(device, swapChainPointer, null);
    }
}
