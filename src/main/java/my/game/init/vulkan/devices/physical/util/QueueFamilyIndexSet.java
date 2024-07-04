package my.game.init.vulkan.devices.physical.util;

import java.util.HashSet;

public class QueueFamilyIndexSet extends HashSet<Integer> {
    @Override
    public boolean add(Integer x) {
        if (x == -1) {
            return false;
        }
        return super.add(x);
    }
}
