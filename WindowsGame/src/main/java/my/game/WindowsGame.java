package my.game;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.windows.User32;

public class WindowsGame {
    public static void main(String[] args) {
        long desktop = User32.GetDC(MemoryUtil.NULL);
        System.out.println(desktop);
    }
}
