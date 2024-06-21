package my.game;

import org.lwjgl.util.shaderc.Shaderc;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;

//TODO This should probably be its own module that compiles shaders in the actual main project module. This would be fine
// then to just reference the paths explicitly but maybe a smarter way is better. Also decide on if spv files (compiled shaders)
// should be included on git or not. I vote no since they are not human readable so anyone checking out should run ShaderCompiler
// I could make ShaderCompiler an ant task so when maven compiles this will always run and then also set up intellij to delegate
// compilation to maven.
public class ShaderCompiler {

    private static final String shadersDirectory = "src/main/resources/shaders/";

    public static void main(String[] args) {
        ShaderCompiler shaderCompiler = new ShaderCompiler();
        File file = new File(shadersDirectory);
        File[] files = file.listFiles();
        if (files == null) {
            throw new IllegalStateException(String.format("Shaders directory is empty at %s", shadersDirectory));
        }
        for (File f : files) {
            if (f.getName().endsWith(".frag")) {
                shaderCompiler.compileShaderIfChanged(f, Shaderc.shaderc_fragment_shader);
            } else if (f.getName().endsWith(".vert")) {
                shaderCompiler.compileShaderIfChanged(f, Shaderc.shaderc_vertex_shader);
            }
        }
    }

    private void compileShaderIfChanged(File glsShaderFile, int shaderType) {
        byte[] compiledShader;
        try {
            File spvFile = new File(glsShaderFile.getPath() + ".spv");
            if (!spvFile.exists() || glsShaderFile.lastModified() > spvFile.lastModified()) {
                System.out.printf("Compiling [%s] to [%s]\n", glsShaderFile.getPath(), spvFile.getPath());
                String shaderCode = new String(Files.readAllBytes(glsShaderFile.toPath()));

                compiledShader = compileShader(shaderCode, shaderType);
                Files.write(spvFile.toPath(), compiledShader);
            } else {
                System.out.printf("Shader [%s] already compiled. Loading compiled version: [%s]\n", glsShaderFile.getPath(), spvFile.getPath());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] compileShader(String shaderCode, int shaderType) {
        long compiler = 0;
        long options = 0;
        byte[] compiledShader;

        try {
            compiler = Shaderc.shaderc_compiler_initialize();
            options = Shaderc.shaderc_compile_options_initialize();

            long result = Shaderc.shaderc_compile_into_spv(
                    compiler,
                    shaderCode,
                    shaderType,
                    "shader.glsl",
                    "main",
                    options
            );

            if (Shaderc.shaderc_result_get_compilation_status(result) != Shaderc.shaderc_compilation_status_success) {
                throw new RuntimeException("Shader compilation failed: " + Shaderc.shaderc_result_get_error_message(result));
            }

            ByteBuffer buffer = Shaderc.shaderc_result_get_bytes(result);
            compiledShader = new byte[buffer.remaining()];
            buffer.get(compiledShader);
        } finally {
            Shaderc.shaderc_compile_options_release(options);
            Shaderc.shaderc_compiler_release(compiler);
        }

        return compiledShader;
    }
}
