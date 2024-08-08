package my.game.init.vulkan.drawing;

import my.game.init.vulkan.math.Vector2fWithSize;
import my.game.init.vulkan.math.Vector3fWithSize;

public record Vertex(Vector2fWithSize pos, Vector3fWithSize color) {
    public static final int SIZE = Vector2fWithSize.SIZE + Vector3fWithSize.SIZE;
    public static final int POSITION_OFFSET = 0;
    public static final int COLOR_OFFSET = Vector2fWithSize.SIZE;

    public int getSize() {
        return SIZE;
    }
}
