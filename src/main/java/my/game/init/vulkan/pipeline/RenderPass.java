package my.game.init.vulkan.pipeline;

import my.game.init.vulkan.swapchain.SwapChain;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkAttachmentDescription;
import org.lwjgl.vulkan.VkAttachmentReference;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkRenderPassCreateInfo;
import org.lwjgl.vulkan.VkSubpassDependency;
import org.lwjgl.vulkan.VkSubpassDescription;

import java.nio.LongBuffer;

public class RenderPass {
    private final VkDevice device;
    private final SwapChain swapChain;
    private final long renderPassPointer;

    public RenderPass(VkDevice device, SwapChain swapChain) {
        this.device = device;
        this.swapChain = swapChain;
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {

            VkAttachmentReference.Buffer colorAttachmentRefBuffer = VkAttachmentReference.malloc(1, memoryStack);
            VkAttachmentReference colorAttachmentRef = VkAttachmentReference.calloc(memoryStack);
            colorAttachmentRef
                    .attachment(0)
                    .layout(VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            colorAttachmentRefBuffer.put(colorAttachmentRef);
            colorAttachmentRefBuffer.flip();

            VkSubpassDescription.Buffer subpassDescriptionBuffer = VkSubpassDescription.malloc(1, memoryStack);
            VkSubpassDescription subpassDescription = VkSubpassDescription.calloc(memoryStack);
            subpassDescription
                    .pipelineBindPoint(VK10.VK_PIPELINE_BIND_POINT_GRAPHICS)
                    .pColorAttachments(colorAttachmentRefBuffer)
                    /*
                    Quote from Spasi author of lwjgl:

                    When there are multiple buffer members that are sized by the same count/length member,
                    then that member has an explicit setter. In the case of VkSubpassDescription,
                    both pColorAttachments and pResolveAttachments are sized by colorAttachmentCount.
                    One could argue that you may set pColorAttachments without setting pResolveAttachments.
                    You also cannot set pResolveAttachments without setting pColorAttachments.
                    Both are optional, but pResolveAttachments is "more" optional. Additional logic for this scenario
                    could be added to the LWJGL code generator, but I didn't think it was important enough.
                    There are currently only two such cases in structs: vkSubpassDescription::pColorAttachments
                    and AIMesh::mVertices.
                    */
                    .colorAttachmentCount(colorAttachmentRefBuffer.capacity());
            subpassDescriptionBuffer.put(subpassDescription);
            subpassDescriptionBuffer.flip();

            VkAttachmentDescription.Buffer colorAttachmentBuffer = VkAttachmentDescription.malloc(1, memoryStack);
            VkAttachmentDescription colorAttachment = VkAttachmentDescription.calloc(memoryStack);
            colorAttachment
                    /*As of writing this comment we only care about the first swap chain we created when getting the
                      surface format. We would need to recreate the render pass if for example
                      "when moving a window from a standard range to a high dynamic range monitor", but for now not
                      handling that use case.
                     */
                    .format(swapChain.getSurfaceFormat().format())
                    .samples(VK10.VK_SAMPLE_COUNT_1_BIT)
                    .loadOp(VK10.VK_ATTACHMENT_LOAD_OP_CLEAR)
                    .storeOp(VK10.VK_ATTACHMENT_STORE_OP_STORE)
                    .stencilLoadOp(VK10.VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                    .stencilStoreOp(VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE)
                    .initialLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED)
                    .finalLayout(KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
            colorAttachmentBuffer.put(colorAttachment);
            colorAttachmentBuffer.flip();

            VkSubpassDependency.Buffer subpassDependency = VkSubpassDependency.calloc(1, memoryStack);
            subpassDependency
                    .srcSubpass(VK10.VK_SUBPASS_EXTERNAL)
                    .dstSubpass(0)
                    .srcStageMask(VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .srcAccessMask(0)
                    .dstStageMask(VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .dstAccessMask(VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);

            VkRenderPassCreateInfo renderPassCreateInfo = VkRenderPassCreateInfo.calloc(memoryStack);
            renderPassCreateInfo
                    .sType(VK10.VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                    .pAttachments(colorAttachmentBuffer)
                    .pSubpasses(subpassDescriptionBuffer)
                    .pDependencies(subpassDependency);
            LongBuffer renderPassPointerBuffer = memoryStack.mallocLong(1);
            int result = VK10.vkCreateRenderPass(
                    device,
                    renderPassCreateInfo,
                    null,
                    renderPassPointerBuffer);
            if (result != VK10.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to create render pass. Error code: %d", result));
            }
            renderPassPointer = renderPassPointerBuffer.get(0);
        }
    }

    public RenderPass validateSwapChain(SwapChain newSwapChain) {
        if (swapChain.getSurfaceFormat().format() != newSwapChain.getSurfaceFormat().format()) {
            free();
            return new RenderPass(device, newSwapChain);
        } else {
            return this;
        }
    }

    public long getRenderPassPointer() {
        return renderPassPointer;
    }

    public void free() {
        VK10.vkDestroyRenderPass(device,
                renderPassPointer,
                null);
    }
}
