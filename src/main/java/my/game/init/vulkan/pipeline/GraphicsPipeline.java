package my.game.init.vulkan.pipeline;

import com.google.common.collect.ImmutableList;
import my.game.init.vulkan.pipeline.shaders.LoadedShader;
import my.game.init.vulkan.pipeline.shaders.ShaderModule;
import my.game.init.vulkan.swapchain.SwapChainImages;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkOffset2D;
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineDynamicStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkPipelineViewportStateCreateInfo;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkViewport;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.List;

public class GraphicsPipeline {
    final ShaderModule simpleVertexShader;
    final ShaderModule simpleFragmentShader;
    final Long pipelineLayoutPointer;
    final SwapChainImages swapChainImages;

    List<Integer> dynamicStates = ImmutableList.of(
            VK13.VK_DYNAMIC_STATE_VIEWPORT,
            VK13.VK_DYNAMIC_STATE_SCISSOR
    );

    public GraphicsPipeline(final SwapChainImages swapChainImages) {
        this.swapChainImages = swapChainImages;
        VkDevice device = swapChainImages.getSwapChain().getLogicalDevice().getLogicalDeviceInformation().vkDevice();
        LoadedShader loadedVertex = new LoadedShader("shaders/compiled/simple_shader.vert.spv");
        simpleVertexShader = new ShaderModule(device, loadedVertex);
        loadedVertex.free();
        LoadedShader loadedFragment = new LoadedShader("shaders/compiled/simple_shader.frag.spv");
        simpleFragmentShader = new ShaderModule(device, loadedFragment);
        loadedFragment.free();
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            VkPipelineShaderStageCreateInfo vertexShaderStageInfo = VkPipelineShaderStageCreateInfo.calloc(memoryStack);
            vertexShaderStageInfo
                    .sType(VK13.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                    .stage(VK13.VK_SHADER_STAGE_VERTEX_BIT)
                    .module(simpleVertexShader.getShaderModulePointer())
                    .pName(memoryStack.UTF8("main"));
            VkPipelineShaderStageCreateInfo fragmentShaderStageInfo = VkPipelineShaderStageCreateInfo.calloc(memoryStack);
            fragmentShaderStageInfo
                    .sType(VK13.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                    .stage(VK13.VK_SHADER_STAGE_FRAGMENT_BIT)
                    .module(simpleFragmentShader.getShaderModulePointer())
                    .pName(memoryStack.UTF8("main"));

            IntBuffer pDynamicStates = memoryStack.mallocInt(dynamicStates.size());
            for (Integer x : dynamicStates) {
                pDynamicStates.put(x);
            }
            pDynamicStates.flip();
            VkPipelineDynamicStateCreateInfo dynamicStateCreateInfo = VkPipelineDynamicStateCreateInfo.calloc(memoryStack);
            dynamicStateCreateInfo
                    .sType(VK13.VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
                    .pDynamicStates(pDynamicStates);

            VkPipelineInputAssemblyStateCreateInfo inputAssemblyStateCreateInfo = VkPipelineInputAssemblyStateCreateInfo.calloc(memoryStack);
            inputAssemblyStateCreateInfo
                    .sType(VK13.VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
                    .topology(VK13.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
                    .primitiveRestartEnable(false);
            VkViewport.Buffer viewportBuffer = VkViewport.calloc(1, memoryStack);
            VkViewport viewport = VkViewport.calloc(memoryStack);
            viewport
                    .x(0.0f)
                    .y(0.0f)
                    .width(swapChainImages.getSwapChain().getSwapChainExtent().width())
                    .height(swapChainImages.getSwapChain().getSwapChainExtent().height())
                    .minDepth(0.0f)
                    .maxDepth(1.0f);
            viewportBuffer.put(viewport);
            viewportBuffer.flip();

            VkRect2D.Buffer scissorBuffer = VkRect2D.calloc(1, memoryStack);
            VkRect2D scissor = VkRect2D.calloc(memoryStack);
            VkOffset2D offset = VkOffset2D.calloc(memoryStack);
            offset
                    .x(0)
                    .y(0);
            scissor
                    .offset(offset)
                    .extent(swapChainImages.getSwapChain().getSwapChainExtent());
            scissorBuffer.put(scissor);
            scissorBuffer.flip();

            VkPipelineViewportStateCreateInfo viewportStateCreateInfo = VkPipelineViewportStateCreateInfo.calloc(memoryStack);
            viewportStateCreateInfo
                    .sType(VK13.VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
                    .pViewports(viewportBuffer)
                    .pScissors(scissorBuffer);

            VkPipelineRasterizationStateCreateInfo rasterizationStateCreateInfo = VkPipelineRasterizationStateCreateInfo.calloc(memoryStack);
            rasterizationStateCreateInfo
                    .sType(VK13.VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
                    .depthClampEnable(false)
                    .rasterizerDiscardEnable(false)
                    .polygonMode(VK13.VK_POLYGON_MODE_FILL)
                    .lineWidth(1.0f)
                    .cullMode(VK13.VK_CULL_MODE_BACK_BIT)
                    .frontFace(VK13.VK_FRONT_FACE_CLOCKWISE)
                    .depthBiasEnable(false)
                    .depthBiasConstantFactor(0.0f)
                    .depthBiasClamp(0.0f)
                    .depthBiasSlopeFactor(0.0f);

            VkPipelineMultisampleStateCreateInfo multiSampleStateCreateInfo = VkPipelineMultisampleStateCreateInfo.calloc(memoryStack);
            multiSampleStateCreateInfo
                    .sType(VK13.VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
                    .sampleShadingEnable(false)
                    .rasterizationSamples(VK13.VK_SAMPLE_COUNT_1_BIT)
                    .minSampleShading(1.0f)
                    .pSampleMask(null)
                    .alphaToCoverageEnable(false)
                    .alphaToOneEnable(false);


            VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachmentBuffer = VkPipelineColorBlendAttachmentState.malloc(1, memoryStack);
            VkPipelineColorBlendAttachmentState colorBlendAttachment = VkPipelineColorBlendAttachmentState.calloc(memoryStack);
            colorBlendAttachment
                    .colorWriteMask(VK13.VK_COLOR_COMPONENT_R_BIT | VK13.VK_COLOR_COMPONENT_G_BIT | VK13.VK_COLOR_COMPONENT_B_BIT | VK13.VK_COLOR_COMPONENT_A_BIT)
                    .blendEnable(false)
                    .srcColorBlendFactor(VK13.VK_BLEND_FACTOR_ONE)
                    .dstAlphaBlendFactor(VK13.VK_BLEND_FACTOR_ZERO)
                    .colorBlendOp(VK13.VK_BLEND_OP_ADD)
                    .srcAlphaBlendFactor(VK13.VK_BLEND_FACTOR_ONE)
                    .dstAlphaBlendFactor(VK13.VK_BLEND_FACTOR_ZERO)
                    .alphaBlendOp(VK13.VK_BLEND_OP_ADD);
            colorBlendAttachmentBuffer.put(colorBlendAttachment);
            colorBlendAttachmentBuffer.flip();

            VkPipelineColorBlendStateCreateInfo colorBlendStateCreateInfo = VkPipelineColorBlendStateCreateInfo.calloc(memoryStack);
            colorBlendStateCreateInfo
                    .sType(VK13.VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
                    .logicOpEnable(false)
                    .logicOp(VK13.VK_LOGIC_OP_COPY)
                    .pAttachments(colorBlendAttachmentBuffer)
                    .blendConstants(0, 0.0f)
                    .blendConstants(1, 0.0f)
                    .blendConstants(2, 0.0f)
                    .blendConstants(3, 0.0f);

            LongBuffer pPipelineLayout = memoryStack.mallocLong(1);
            VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(memoryStack);
            pipelineLayoutInfo
                    .sType(VK13.VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                    .pSetLayouts(null)
                    .pPushConstantRanges(null);
            int result = VK13.vkCreatePipelineLayout(device, pipelineLayoutInfo, null, pPipelineLayout);
            if (result != VK13.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to create pipeline layout. Error code", result));
            }
            pipelineLayoutPointer = pPipelineLayout.get(0);
        }
    }

    public void free() {
        VK13.vkDestroyPipelineLayout(swapChainImages.getSwapChain().getLogicalDevice().getLogicalDeviceInformation().vkDevice(), pipelineLayoutPointer, null);
        simpleVertexShader.free();
        simpleFragmentShader.free();
    }
}
