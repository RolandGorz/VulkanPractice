If you want this to run you need to compile in maven first to generate the shadersIndex.txt in the build output
and then you can build from within intellij to benefit from incremental builds. (Maven has incremental builds
but as of the time of writing this readme I have yet to look into that). Also if you need to change ShaderIndexer
make sure to run install step of maven lifecycle since this package will reference the jar in the maven repository 
rather than the one in the build output directory of ShaderIndex.