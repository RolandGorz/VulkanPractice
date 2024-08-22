package my.game.init.vulkan.pipeline.shaders;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import java.nio.LongBuffer;

public class ShaderModule {
    private final Long shaderModulePointer;
    private final VkDevice device;

    public ShaderModule(final VkDevice device, final LoadedShader loadedShader) {
        this.device = device;
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            VkShaderModuleCreateInfo shaderModuleCreateInfo = VkShaderModuleCreateInfo.calloc(memoryStack);
            shaderModuleCreateInfo
                    .sType(VK10.VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                    .pCode(loadedShader.getShaderCode());
            LongBuffer shaderModulePointerBuffer = memoryStack.mallocLong(1);
            int result = VK10.vkCreateShaderModule(
                    device,
                    shaderModuleCreateInfo,
                    null,
                    shaderModulePointerBuffer);
            if (result != VK10.VK_SUCCESS) {
                throw new IllegalStateException(String.format("Failed to create shader for %s. Error code %d", loadedShader.getFileName(), result));
            }
            shaderModulePointer = shaderModulePointerBuffer.get(0);
        }
    }

    public Long getShaderModulePointer() {
        return shaderModulePointer;
    }

    public void free() {
        VK10.vkDestroyShaderModule(device, shaderModulePointer, null);
    }
}
