package my.game.init.vulkan.drawing.memory.buffer;

import my.game.init.vulkan.command.CommandPool;
import my.game.init.vulkan.devices.logical.LogicalDevice;
import my.game.init.vulkan.struct.Index;
import org.lwjgl.vulkan.VK10;

import java.util.List;

public class IndexBuffer extends StagingBufferUser {

    public IndexBuffer(LogicalDevice logicalDevice, List<Index> indexList, CommandPool commandPool) {
        super(indexList, logicalDevice, VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK10.VK_BUFFER_USAGE_INDEX_BUFFER_BIT,
                commandPool,
                (stagingDataByteBuffer) -> {
                    for (Index curr : indexList) {
                        stagingDataByteBuffer.putShort(curr.value());
                    }
                    stagingDataByteBuffer.flip();
                });
    }
}
