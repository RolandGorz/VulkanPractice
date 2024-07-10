package my.game.init.vulkan.pipeline;

import my.game.init.vulkan.swapchain.SwapChainImages;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkAttachmentDescription;
import org.lwjgl.vulkan.VkAttachmentReference;
import org.lwjgl.vulkan.VkRenderPassCreateInfo;
import org.lwjgl.vulkan.VkSubpassDescription;

import java.nio.LongBuffer;

public class RenderPass {
    private final long renderPassPointer;
    private SwapChainImages swapChainImages;

    public RenderPass(SwapChainImages swapChainImages) {
        this.swapChainImages = swapChainImages;
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {

            VkAttachmentReference.Buffer colorAttachmentRefBuffer = VkAttachmentReference.malloc(1, memoryStack);
            VkAttachmentReference colorAttachmentRef = VkAttachmentReference.calloc(memoryStack);
            colorAttachmentRef
                    .attachment(0)
                    .layout(VK13.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            colorAttachmentRefBuffer.put(colorAttachmentRef);
            colorAttachmentRefBuffer.flip();

            VkSubpassDescription.Buffer subpassDescriptionBuffer = VkSubpassDescription.malloc(1, memoryStack);
            VkSubpassDescription subpassDescription = VkSubpassDescription.calloc(memoryStack);
            subpassDescription
                    .pipelineBindPoint(VK13.VK_PIPELINE_BIND_POINT_GRAPHICS)
                    .pColorAttachments(colorAttachmentRefBuffer);
            subpassDescriptionBuffer.put(subpassDescription);
            subpassDescriptionBuffer.flip();

            VkAttachmentDescription.Buffer colorAttachmentBuffer = VkAttachmentDescription.malloc(1, memoryStack);
            VkAttachmentDescription colorAttachment = VkAttachmentDescription.calloc(memoryStack);
            colorAttachment
                    .format(swapChainImages.getSwapChain().getSurfaceFormat().format())
                    .samples(VK13.VK_SAMPLE_COUNT_1_BIT)
                    .loadOp(VK13.VK_ATTACHMENT_LOAD_OP_CLEAR)
                    .storeOp(VK13.VK_ATTACHMENT_STORE_OP_STORE)
                    .stencilLoadOp(VK13.VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                    .stencilStoreOp(VK13.VK_ATTACHMENT_STORE_OP_DONT_CARE)
                    .initialLayout(VK13.VK_IMAGE_LAYOUT_UNDEFINED)
                    .finalLayout(KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
            colorAttachmentBuffer.put(colorAttachment);
            colorAttachmentBuffer.flip();

            VkRenderPassCreateInfo renderPassCreateInfo = VkRenderPassCreateInfo.calloc(memoryStack);
            renderPassCreateInfo
                    .sType(VK13.VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                    .pAttachments(colorAttachmentBuffer)
                    .pSubpasses(subpassDescriptionBuffer);
            LongBuffer renderPassPointerBuffer = memoryStack.mallocLong(1);
            int result = VK13.vkCreateRenderPass(
                    swapChainImages.getSwapChain().getLogicalDevice().getLogicalDeviceInformation().vkDevice(),
                    renderPassCreateInfo,
                    null,
                    renderPassPointerBuffer);
            if (result != VK13.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to create render pass. Error code: %d", result));
            }
            renderPassPointer = renderPassPointerBuffer.get(0);
        }
    }

    public void free() {
        VK13.vkDestroyRenderPass(swapChainImages.getSwapChain().getLogicalDevice().getLogicalDeviceInformation().vkDevice(),
                renderPassPointer,
                null);
    }
}
