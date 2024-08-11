package my.game.init.vulkan.struct;

public record Index(short value) implements Struct {

    @Override
    public int getSize() {
        return Short.SIZE;
    }
}
