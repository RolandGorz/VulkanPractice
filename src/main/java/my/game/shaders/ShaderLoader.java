package my.game.shaders;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

public class ShaderLoader {
    public void loadShaders() {
        Enumeration<URL> shaderResources;
        try {
            shaderResources = this.getClass().getClassLoader().getResources("shaders/compiled");
        } catch (IOException e) {
            System.out.printf("Error occurred trying to load compiled shaders. %s%n", e);
            throw new RuntimeException(e);
        }
        if (!shaderResources.hasMoreElements()) {
            throw new IllegalStateException("No shaders directory found. Giving up");
        }
        URL shadersDirectoryURL = shaderResources.nextElement();
        if (shaderResources.hasMoreElements()) {
            throw new IllegalStateException("More than one shaders directory found. Giving up");
        }
        File shadersDirectory = new File(shadersDirectoryURL.getFile());
        File[] files = shadersDirectory.listFiles();
        if (files == null) {
            throw new IllegalStateException(String.format("Shaders directory is empty at %s", shadersDirectory.getName()));
        }
        for (File f : files) {
            System.out.printf("%s shader code size %d\n", f.getName(), f.length());
        }
    }
}
