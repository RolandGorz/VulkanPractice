package my.game.init.vulkan.pipeline.shaders;

import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;

public class LoadedShader {

    private final ByteBuffer shaderCode;
    private final String fileName;

    public LoadedShader(final String fileName) {
        this.fileName = fileName;
        final URL shaderResource = this.getClass().getClassLoader().getResource(fileName);
        if (shaderResource == null) {
            throw new IllegalStateException(String.format("Could not find shader resource %s", fileName));
        }
        File f = new File(shaderResource.getFile());
        System.out.printf("%s shader code size %d\n", f.getName(), f.length());
        try (FileInputStream fileInputStream = new FileInputStream(f)) {
            byte[] code = fileInputStream.readAllBytes();
            ByteBuffer byteBufferCode = MemoryUtil.memAlloc(code.length);
            byteBufferCode.put(code);
            shaderCode = byteBufferCode.flip();
        } catch (IOException e) {
            throw new RuntimeException("This should never happen.", e);
        }
    }

    public ByteBuffer getShaderCode() {
        return shaderCode;
    }

    public String getFileName() {
        return fileName;
    }

    public void free() {
        MemoryUtil.memFree(shaderCode);
    }
}
