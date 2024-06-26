package my.game.shaders;

import org.lwjgl.util.shaderc.Shaderc;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Enumeration;

// IMPORTANT!!! we must call this at runtime since the shaders are compiled to be understood by the user's cpu.
// If i compiled the shader on my machine and then shipped the compiled shaders to be used by other people it
// would fail spectacularly to be understood by their gpu.
public class ShaderCompiler {

    private final File[] glslShaderFiles;

    public ShaderCompiler() {
        Enumeration<URL> shadersDirectory;
        try {
            shadersDirectory = ShaderCompiler.class.getClassLoader().getResources("shaders/source");
        } catch (IOException e) {
            System.out.printf("Error occurred trying to load source shaders. %s%n", e);
            throw new RuntimeException(e);
        }
        if (!shadersDirectory.hasMoreElements()) {
            throw new IllegalStateException("No shaders directory found. Giving up");
        }
        URL shaders = shadersDirectory.nextElement();
        if (shadersDirectory.hasMoreElements()) {
            throw new IllegalStateException("More than one shaders directory found. Giving up");
        }
        File file = new File(shaders.getFile());
        glslShaderFiles = file.listFiles();
        if (glslShaderFiles == null) {
            throw new IllegalStateException(String.format("Shaders directory is empty at %s", file.getName()));
        }
    }

    public void compileShaders() {
        for (File f : glslShaderFiles) {
            if (f.getName().endsWith(".frag")) {
                compileShaderIfChanged(f, Shaderc.shaderc_fragment_shader);
            } else if (f.getName().endsWith(".vert")) {
                compileShaderIfChanged(f, Shaderc.shaderc_vertex_shader);
            }
        }
    }

    private void compileShaderIfChanged(File glslShaderFile, int shaderType) {
        byte[] compiledShader;
        try {
            String glslShaderFileName = glslShaderFile.getName();
            File spvFile = new File(glslShaderFile.getParentFile().getParentFile() + "/compiled/" + glslShaderFileName + ".spv");
            File compiledDirectory = spvFile.getParentFile();
            if (!compiledDirectory.exists()) {
                if (!spvFile.getParentFile().mkdir()) {
                    System.out.println("Failed to create compiled shaders directory.");
                    throw new IllegalStateException("Failed to create compiled shaders directory.");
                }
            }
            if (!spvFile.exists() || glslShaderFile.lastModified() > spvFile.lastModified()) {
                System.out.printf("Compiling [%s] to [%s]\n", glslShaderFile.getPath(), spvFile.getPath());
                String shaderCode = new String(Files.readAllBytes(glslShaderFile.toPath()));

                compiledShader = compileShader(shaderCode, shaderType);
                Files.write(spvFile.toPath(), compiledShader);
            } else {
                System.out.printf("Shader [%s] already compiled. Loading compiled version: [%s]\n", glslShaderFile.getPath(), spvFile.getPath());
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
