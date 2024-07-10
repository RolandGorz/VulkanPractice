package my.game.init.vulkan.swapchain;

import com.google.common.collect.ImmutableList;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkComponentMapping;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkImageSubresourceRange;
import org.lwjgl.vulkan.VkImageViewCreateInfo;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.List;

public class SwapChainImages {
    final SwapChain swapChain;
    final List<Long> swapChainImagePointers;
    final List<Long> swapChainImageViewPointers;

    public SwapChainImages(SwapChain swapChain) {
        this.swapChain = swapChain;
        swapChainImagePointers = createSwapChainImages();
        swapChainImageViewPointers = createSwapChainImageViews();
    }

    public SwapChain getSwapChain() {
        return swapChain;
    }

    private List<Long> createSwapChainImages() {
        Long swapChainPointer = swapChain.getSwapChainPointer();
        VkDevice device = swapChain.getLogicalDevice().getLogicalDeviceInformation().vkDevice();
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
            ImmutableList.Builder<Long> builder = ImmutableList.builder();
            for (int i = 0; i < imagePointers.capacity(); ++i) {
                builder.add(imagePointers.get(i));
            }
            return builder.build();
        }
    }

    private List<Long> createSwapChainImageViews() {
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            VkComponentMapping vkComponentMapping = VkComponentMapping.calloc(memoryStack);
            vkComponentMapping
                    .r(VK13.VK_COMPONENT_SWIZZLE_IDENTITY)
                    .g(VK13.VK_COMPONENT_SWIZZLE_IDENTITY)
                    .b(VK13.VK_COMPONENT_SWIZZLE_IDENTITY)
                    .a(VK13.VK_COMPONENT_SWIZZLE_IDENTITY);
            VkImageSubresourceRange vkImageSubresourceRange = VkImageSubresourceRange.calloc(memoryStack);
            vkImageSubresourceRange
                    .aspectMask(VK13.VK_IMAGE_ASPECT_COLOR_BIT)
                    .baseMipLevel(0)
                    .levelCount(1)
                    .baseArrayLayer(0)
                    .layerCount(1);
            ImmutableList.Builder<Long> imageViewsBuilder = ImmutableList.builder();
            LongBuffer imageViewPointer = memoryStack.mallocLong(1);
            for (int i = 0; i < swapChainImagePointers.size(); ++i) {
                VkImageViewCreateInfo vkImageViewCreateInfo = VkImageViewCreateInfo.calloc(memoryStack);
                vkImageViewCreateInfo
                        .sType(VK13.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                        .image(swapChainImagePointers.get(i))
                        .viewType(VK13.VK_IMAGE_VIEW_TYPE_2D)
                        .format(swapChain.getSurfaceFormat().format())
                        .components(vkComponentMapping)
                        .subresourceRange(vkImageSubresourceRange);
                int result = VK13.vkCreateImageView(swapChain.getLogicalDevice().getLogicalDeviceInformation().vkDevice(), vkImageViewCreateInfo, null, imageViewPointer);
                if (result != VK13.VK_SUCCESS) {
                    throw new IllegalStateException(String.format("Failed to create image for swap chain image in position %d of the list of swap chain images. Error code %d",
                            i, result));
                }
                imageViewsBuilder.add(imageViewPointer.get(0));
            }
            return imageViewsBuilder.build();
        }
    }

    public void free() {
        for (Long x : swapChainImageViewPointers) {
            VK13.vkDestroyImageView(swapChain.getLogicalDevice().getLogicalDeviceInformation().vkDevice(), x, null);
        }
    }
}
