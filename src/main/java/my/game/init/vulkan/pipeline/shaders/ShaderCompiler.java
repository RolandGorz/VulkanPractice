package my.game.init.vulkan.pipeline.shaders;

import com.google.common.collect.ImmutableList;
import org.lwjgl.util.shaderc.Shaderc;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

//When running from intellij make sure to set the working directory to be where the jar
// is located to properly reflect how it will run on other machines

// IMPORTANT!!! we must call this at runtime since the shaders are compiled to be understood by the user's cpu.
// If I compiled the shader on my machine and then shipped the compiled shaders to be used by other people it
// would fail spectacularly to be understood by their gpu.
public class ShaderCompiler {

    private final List<String> glslShaderFiles;
    public static final Path COMPILED_PATH = Paths.get(System.getProperty("user.dir"), "shaders", "compiled").toAbsolutePath();

    public ShaderCompiler() {
        InputStream shaderIndex = ShaderCompiler.class.getClassLoader().getResourceAsStream("shadersIndex.txt");
        if (shaderIndex == null) {
            throw new IllegalStateException("Shader index not found. Giving up");
        }
        InputStreamReader inputStreamReader = new InputStreamReader(shaderIndex);
        try (BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            String line = bufferedReader.readLine();
            ImmutableList.Builder<String> shaderFiles = ImmutableList.builder();
            while (line != null) {
                shaderFiles.add(line);
                line = bufferedReader.readLine();
            }
            glslShaderFiles = shaderFiles.build();
            if (glslShaderFiles.isEmpty()) {
                throw new IllegalStateException("Shaders index is empty");
            }
        } catch (IOException e) {
            System.out.println("Something went wrong with closing the input stream for shaders index");
            throw new RuntimeException(e);
        }
        File compiledPathDirectory = COMPILED_PATH.toFile();
        if (!compiledPathDirectory.exists()) {
            if (!compiledPathDirectory.mkdirs()) {
                System.out.println("Failed to create compiled shaders directory.");
                throw new IllegalStateException("Failed to create compiled shaders directory.");
            }
        }
    }

    public void compileShaders() {
        for (String s : glslShaderFiles) {
            if (s.endsWith(".frag")) {
                compileShaderIfChanged(s, Shaderc.shaderc_fragment_shader);
            } else if (s.endsWith(".vert")) {
                compileShaderIfChanged(s, Shaderc.shaderc_vertex_shader);
            }
        }
    }

    private void compileShaderIfChanged(String glslShaderFileName, int shaderType) {
        byte[] compiledShader;
        URL shader = ShaderCompiler.class.getClassLoader().getResource(glslShaderFileName);
        if (shader == null) {
            throw new RuntimeException(String.format("Failed to find shader resource %s", glslShaderFileName));
        }
        File shaderFile = new File(shader.getFile());
        try (InputStream shaderStream = shader.openStream()) {
            if (shaderStream == null) {
                throw new RuntimeException(String.format("Failed to open input stream for %s", glslShaderFileName));
            }
            Path dest = COMPILED_PATH.resolve(shaderFile.getName() + ".spv");
            File destFile = dest.toFile();
            if (!destFile.exists() || shaderFile.lastModified() > destFile.lastModified()) {
                System.out.printf("Compiling [%s] to [%s]\n", shader.getFile(), destFile.getPath());
                String shaderCode = new String(shaderStream.readAllBytes());
                compiledShader = compileShader(shaderCode, shaderType);
                Files.write(destFile.toPath(), compiledShader);
            } else {
                System.out.printf("Shader [%s] already compiled. Loading compiled version: [%s]\n", shaderFile.getPath(), destFile.getPath());
            }
        } catch (IOException e) {
            System.out.printf("Something went wrong with closing the input stream for shader %s\n", glslShaderFileName);
            throw new RuntimeException(e);
        }
    }

    //TODO shaderc should be initialized once and only released when all shaders are compiled
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
