package my.game.init.vulkan.pipeline;

import my.game.init.vulkan.pipeline.shaders.LoadedShader;
import my.game.init.vulkan.pipeline.shaders.ShaderModule;
import org.lwjgl.vulkan.VkDevice;

public class GraphicsPipeline {
    final ShaderModule simpleVertexShader;
    final ShaderModule simpleFragmentShader;

    public GraphicsPipeline(VkDevice device) {
        LoadedShader loadedVertex = new LoadedShader("shaders/compiled/simple_shader.vert.spv");
        simpleVertexShader = new ShaderModule(device, loadedVertex);
        loadedVertex.free();
        LoadedShader loadedFragment = new LoadedShader("shaders/compiled/simple_shader.frag.spv");
        simpleFragmentShader = new ShaderModule(device, loadedFragment);
        loadedFragment.free();
    }

    public void free() {
        simpleVertexShader.free();
        simpleFragmentShader.free();
    }
}
