package my.game;

import org.lwjgl.PointerBuffer;
import org.lwjgl.Version;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.Set;

//TODO properly free memory when there are exceptions. Right now im just letting it leak like crazy especially in
// the places where I just throw runtime exception
public class HelloWorld {

    private static VkInstance vkInstance;

    public static void main(String[] args) {
        System.out.println("Hello LWJGL " + Version.getVersion() + "!");

        //TODO this is kind of stupid and will only work running from intellij. Think of how you want to handle shaders
        // properly.
        File fragmentShader = new File("src/main/resources/shaders/simple_shader.frag.spv");
        File vertexShader = new File("src/main/resources/shaders/simple_shader.vert.spv");

        System.out.printf("Fragment shader code size %d\n", fragmentShader.length());
        System.out.printf("Vertex shader code size %d\n", vertexShader.length());

        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!GLFW.glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure GLFW
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE); // the window will stay hidden after creation
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE); // the window will be resizable
        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_NO_API);

        // Create the window
        final long window = GLFW.glfwCreateWindow(300, 300, "Hello World!", MemoryUtil.NULL, MemoryUtil.NULL);
        if (window == MemoryUtil.NULL)
            throw new RuntimeException("Failed to create the GLFW window");


        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer extensionCount = stack.mallocInt(1); // int*
            int result  = VK13.vkEnumerateInstanceExtensionProperties((ByteBuffer) null, extensionCount, null);
            if (result == VK13.VK_SUCCESS) {
                System.out.println("vkEnumerateInstanceExtensionProperties returned success");
            } else {
                System.out.println("vkEnumerateInstanceExtensionProperties returned failure");
            }
            System.out.printf("%d extensions supported\n", extensionCount.get(0));
            VkExtensionProperties.Buffer vkExtensionPropertiesBuffer = VkExtensionProperties.malloc(extensionCount.get(0), stack);
            int result2 = VK13.vkEnumerateInstanceExtensionProperties((ByteBuffer) null, extensionCount, vkExtensionPropertiesBuffer);
            if (result2 == VK13.VK_SUCCESS) {
                System.out.println("vkEnumerateInstanceExtensionProperties returned success");
            } else {
                System.out.println("vkEnumerateInstanceExtensionProperties returned failure");
            }
            System.out.printf("%d extensions supported\n", extensionCount.get(0));
            Set<String> supportedExtensions = new HashSet<>();
            for (VkExtensionProperties x : vkExtensionPropertiesBuffer) {
                System.out.printf("%s\n", x.extensionNameString());
                supportedExtensions.add(x.extensionNameString());
            }

            PointerBuffer glfwRequiredExtensions = GLFWVulkan.glfwGetRequiredInstanceExtensions();
            if (glfwRequiredExtensions == null) {
                throw new RuntimeException("glfwGetRequiredInstanceExtensions returned null");
            }
            for (int i = 0; i < glfwRequiredExtensions.capacity(); ++i) {
                String curr = MemoryUtil.memASCII(glfwRequiredExtensions.get(i));
                if (supportedExtensions.contains(curr)) {
                    System.out.printf("GLFW required extension %s is supported\n", curr);
                } else {
                    System.out.printf("GLFW required extension %s is not supported\n", curr);
                    throw new RuntimeException(String.format("GLFW required extension: %s is not supported\n", curr));
                }
            }
            vkInstance = initVulkan(stack);
        } // the stack frame is popped automatically

        GLFW.glfwShowWindow(window);
        while (!GLFW.glfwWindowShouldClose(window)) {
            GLFW.glfwPollEvents();
        }

        //Free vulkan
        VK13.vkDestroyInstance(vkInstance, null);

        // Free the window callbacks and destroy the window
        Callbacks.glfwFreeCallbacks(window);
        GLFW.glfwDestroyWindow(window);

        // Terminate GLFW and free the error callback
        GLFW.glfwTerminate();
        GLFW.glfwSetErrorCallback(null).free();
    }

    private static VkInstance initVulkan(MemoryStack memoryStack) {
        VkApplicationInfo appInfo = VkApplicationInfo.malloc(memoryStack);
        appInfo.sType(VK13.VK_STRUCTURE_TYPE_APPLICATION_INFO);
        appInfo.pApplicationName(MemoryUtil.memASCII("Hello Triangle"));
        appInfo.applicationVersion(VK13.VK_MAKE_VERSION(1, 0, 0));
        appInfo.pEngineName(MemoryUtil.memASCII("No Engine"));
        appInfo.engineVersion(VK13.VK_MAKE_VERSION(1, 0, 0));
        appInfo.apiVersion(VK13.VK_API_VERSION_1_0);

        VkInstanceCreateInfo vkInstanceCreateInfo = VkInstanceCreateInfo.malloc(memoryStack);
        vkInstanceCreateInfo.sType(VK13.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
        vkInstanceCreateInfo.pApplicationInfo(appInfo);
        PointerBuffer pointerBuffer = memoryStack.mallocPointer(1);
        int result = VK13.vkCreateInstance(vkInstanceCreateInfo, null, pointerBuffer);
        if (result != VK13.VK_SUCCESS) {
            throw new RuntimeException("creating vulkan instance failed");
        }
        return new VkInstance(pointerBuffer.get(0), vkInstanceCreateInfo);
    }
}