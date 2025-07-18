If you want this to run you need to compile in maven first to generate the shadersIndex.txt in the build output
and then you can build from within intellij to benefit from incremental builds. (Maven has incremental builds
but as of the time of writing this readme I have yet to look into that). Also if you need to change ShaderIndexer
make sure to run install step of maven lifecycle since this package will reference the jar in the maven repository 
rather than the one in the build output directory of ShaderIndex.

When running the jar file created by maven package make sure to set the working directory to target so that 
both the jar run and maven run can share the same directory consistently

For Mac to be able to run you must include -XstartOnFirstThread since
mac requires your window to be running on the main thread. Also when debugging
on mac with vulkan validation layers you must specify -Dorg.lwjgl.vulkan.libname=&lt;VulkanSDK location>/&lt;Version>/macOS/lib/libvulkan.dylib
Both of these are required as parameters to the java command when running the compiled jar