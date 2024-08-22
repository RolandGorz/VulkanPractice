package my.game.init.vulkan.pipeline;

import com.google.common.collect.ImmutableList;
import my.game.init.vulkan.drawing.transformation.DescriptorSetLayout;
import my.game.init.vulkan.pipeline.shaders.LoadedShader;
import my.game.init.vulkan.pipeline.shaders.ShaderModule;
import my.game.init.vulkan.struct.Vertex;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineDynamicStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineViewportStateCreateInfo;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.List;

public class GraphicsPipeline {
    private final VkDevice device;
    private final Long pipelineLayoutPointer;
    private final Long graphicsPipelinePointer;

    List<Integer> dynamicStates = ImmutableList.of(
            VK10.VK_DYNAMIC_STATE_VIEWPORT,
            VK10.VK_DYNAMIC_STATE_SCISSOR
    );

    public GraphicsPipeline(final VkDevice device, final RenderPass renderPass, final DescriptorSetLayout descriptorSetLayout) {
        this.device = device;
        LoadedShader loadedVertex = new LoadedShader("shaders/compiled/basic.vert.spv");
        ShaderModule vertexShader = new ShaderModule(device, loadedVertex);
        loadedVertex.free();
        LoadedShader loadedFragment = new LoadedShader("shaders/compiled/basic.frag.spv");
        ShaderModule fragmentShader = new ShaderModule(device, loadedFragment);
        loadedFragment.free();
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            VkPipelineShaderStageCreateInfo.Buffer shaderStageCreateInfoBuffer = VkPipelineShaderStageCreateInfo.malloc(2, memoryStack);
            VkPipelineShaderStageCreateInfo vertexShaderStageInfo = VkPipelineShaderStageCreateInfo.calloc(memoryStack);
            vertexShaderStageInfo
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                    .stage(VK10.VK_SHADER_STAGE_VERTEX_BIT)
                    .module(vertexShader.getShaderModulePointer())
                    .pName(memoryStack.UTF8("main"));
            shaderStageCreateInfoBuffer.put(vertexShaderStageInfo);
            VkPipelineShaderStageCreateInfo fragmentShaderStageInfo = VkPipelineShaderStageCreateInfo.calloc(memoryStack);
            fragmentShaderStageInfo
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                    .stage(VK10.VK_SHADER_STAGE_FRAGMENT_BIT)
                    .module(fragmentShader.getShaderModulePointer())
                    .pName(memoryStack.UTF8("main"));
            shaderStageCreateInfoBuffer.put(fragmentShaderStageInfo);
            shaderStageCreateInfoBuffer.flip();

            IntBuffer pDynamicStates = memoryStack.mallocInt(dynamicStates.size());
            for (Integer x : dynamicStates) {
                pDynamicStates.put(x);
            }
            pDynamicStates.flip();
            VkPipelineDynamicStateCreateInfo dynamicStateCreateInfo = VkPipelineDynamicStateCreateInfo.calloc(memoryStack);
            dynamicStateCreateInfo
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
                    .pDynamicStates(pDynamicStates);

            //TODO refactor graphics pipeline. Too much in one place. Also attribute and binding description rewrite immediately

            VkVertexInputBindingDescription.Buffer bindingDescriptions = VkVertexInputBindingDescription.calloc(1, memoryStack);
            bindingDescriptions.get(0)
                    .binding(0)
                    .stride(Vertex.SIZE)
                    .inputRate(VK10.VK_VERTEX_INPUT_RATE_VERTEX);

            VkVertexInputAttributeDescription.Buffer attributeDescriptions = VkVertexInputAttributeDescription.calloc(2, memoryStack);
            attributeDescriptions.get(0)
                    .binding(0)
                    .location(0)
                    .format(VK10.VK_FORMAT_R32G32_SFLOAT)
                    .offset(Vertex.POSITION_OFFSET);
            attributeDescriptions.get(1)
                    .binding(0)
                    .location(1)
                    .format(VK10.VK_FORMAT_R32G32B32_SFLOAT)
                    .offset(Vertex.COLOR_OFFSET);

            VkPipelineVertexInputStateCreateInfo vertexInputStateCreateInfo = VkPipelineVertexInputStateCreateInfo.calloc(memoryStack);
            vertexInputStateCreateInfo
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
                    .pVertexBindingDescriptions(bindingDescriptions)
                    .pVertexAttributeDescriptions(attributeDescriptions);

            VkPipelineInputAssemblyStateCreateInfo inputAssemblyStateCreateInfo = VkPipelineInputAssemblyStateCreateInfo.calloc(memoryStack);
            inputAssemblyStateCreateInfo
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
                    .topology(VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
                    .primitiveRestartEnable(false);

            VkPipelineViewportStateCreateInfo viewportStateCreateInfo = VkPipelineViewportStateCreateInfo.calloc(memoryStack);
            viewportStateCreateInfo
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
                    .viewportCount(1)
                    .scissorCount(1);

            VkPipelineRasterizationStateCreateInfo rasterizationStateCreateInfo = VkPipelineRasterizationStateCreateInfo.calloc(memoryStack);
            rasterizationStateCreateInfo
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
                    .depthClampEnable(false)
                    .rasterizerDiscardEnable(false)
                    .polygonMode(VK10.VK_POLYGON_MODE_FILL)
                    .lineWidth(1.0f)
                    .cullMode(VK10.VK_CULL_MODE_BACK_BIT)
                    .frontFace(VK10.VK_FRONT_FACE_CLOCKWISE)
                    .depthBiasEnable(false)
                    .depthBiasConstantFactor(0.0f)
                    .depthBiasClamp(0.0f)
                    .depthBiasSlopeFactor(0.0f);

            VkPipelineMultisampleStateCreateInfo multiSampleStateCreateInfo = VkPipelineMultisampleStateCreateInfo.calloc(memoryStack);
            multiSampleStateCreateInfo
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
                    .sampleShadingEnable(false)
                    .rasterizationSamples(VK10.VK_SAMPLE_COUNT_1_BIT)
                    .minSampleShading(1.0f)
                    .pSampleMask(null)
                    .alphaToCoverageEnable(false)
                    .alphaToOneEnable(false);


            VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachmentBuffer = VkPipelineColorBlendAttachmentState.malloc(1, memoryStack);
            VkPipelineColorBlendAttachmentState colorBlendAttachment = VkPipelineColorBlendAttachmentState.calloc(memoryStack);
            colorBlendAttachment
                    .colorWriteMask(VK10.VK_COLOR_COMPONENT_R_BIT | VK10.VK_COLOR_COMPONENT_G_BIT | VK10.VK_COLOR_COMPONENT_B_BIT | VK10.VK_COLOR_COMPONENT_A_BIT)
                    .blendEnable(false);
            colorBlendAttachmentBuffer.put(colorBlendAttachment);
            colorBlendAttachmentBuffer.flip();

            VkPipelineColorBlendStateCreateInfo colorBlendStateCreateInfo = VkPipelineColorBlendStateCreateInfo.calloc(memoryStack);
            colorBlendStateCreateInfo
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
                    .logicOpEnable(false)
                    .logicOp(VK10.VK_LOGIC_OP_COPY)
                    .pAttachments(colorBlendAttachmentBuffer)
                    .blendConstants(0, 0.0f)
                    .blendConstants(1, 0.0f)
                    .blendConstants(2, 0.0f)
                    .blendConstants(3, 0.0f);

            LongBuffer pPipelineLayout = memoryStack.mallocLong(1);
            VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(memoryStack);
            LongBuffer layouts = memoryStack.mallocLong(1);
            layouts.put(descriptorSetLayout.getHandle());
            layouts.flip();
            pipelineLayoutInfo
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                    .pSetLayouts(layouts)
                    .setLayoutCount(layouts.capacity())
                    .pPushConstantRanges(null);
            int result = VK10.vkCreatePipelineLayout(device, pipelineLayoutInfo, null, pPipelineLayout);
            if (result != VK10.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to create pipeline layout. Error code: %d", result));
            }
            pipelineLayoutPointer = pPipelineLayout.get(0);
            VkGraphicsPipelineCreateInfo.Buffer graphicsPipelineCreateInfoBuffer = VkGraphicsPipelineCreateInfo.malloc(1, memoryStack);
            VkGraphicsPipelineCreateInfo graphicsPipelineCreateInfo = VkGraphicsPipelineCreateInfo.calloc(memoryStack);
            graphicsPipelineCreateInfo
                    .sType(VK10.VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
                    .pStages(shaderStageCreateInfoBuffer)
                    .pVertexInputState(vertexInputStateCreateInfo)
                    .pInputAssemblyState(inputAssemblyStateCreateInfo)
                    .pViewportState(viewportStateCreateInfo)
                    .pRasterizationState(rasterizationStateCreateInfo)
                    .pMultisampleState(multiSampleStateCreateInfo)
                    .pDepthStencilState(null)
                    .pColorBlendState(colorBlendStateCreateInfo)
                    .pDynamicState(dynamicStateCreateInfo)
                    .layout(pipelineLayoutPointer)
                    .renderPass(renderPass.getRenderPassPointer())
                    .subpass(0)
                    .basePipelineHandle(VK10.VK_NULL_HANDLE)
                    .basePipelineIndex(-1);
            graphicsPipelineCreateInfoBuffer.put(graphicsPipelineCreateInfo);
            graphicsPipelineCreateInfoBuffer.flip();
            LongBuffer graphicsPipelinePointerBuffer = memoryStack.mallocLong(1);
            int graphicsPipelineResult = VK10.vkCreateGraphicsPipelines(device, VK10.VK_NULL_HANDLE, graphicsPipelineCreateInfoBuffer, null, graphicsPipelinePointerBuffer);
            if (graphicsPipelineResult != VK10.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to create graphics pipeline. Error code %d", graphicsPipelineResult));
            }
            graphicsPipelinePointer = graphicsPipelinePointerBuffer.get(0);
        } finally {
            vertexShader.free();
            fragmentShader.free();
        }
    }

    public Long getGraphicsPipelinePointer() {
        return graphicsPipelinePointer;
    }

    public Long getPipelineLayoutPointer() {
        return pipelineLayoutPointer;
    }

    public void free() {
        VK10.vkDestroyPipeline(device, graphicsPipelinePointer, null);
        VK10.vkDestroyPipelineLayout(device, pipelineLayoutPointer, null);
    }
}
