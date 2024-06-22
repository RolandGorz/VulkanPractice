package my.game.shaders;

import org.lwjgl.util.shaderc.Shaderc;

import java.io.File;

public class ShaderLoader {
    public void loadShaders() {
        //TODO this is kind of stupid and will only work running from intellij. Think of how you want to handle shaders
        // properly.
        File fragmentShader = new File("src/main/resources/shaders/simple_shader.frag.spv");
        File vertexShader = new File("src/main/resources/shaders/simple_shader.vert.spv");

        System.out.printf("Fragment shader code size %d\n", fragmentShader.length());
        System.out.printf("Vertex shader code size %d\n", vertexShader.length());
    }
}
