package my.game.init.vulkan.drawing.memory.buffer;

import my.game.init.vulkan.command.CommandPool;
import my.game.init.vulkan.devices.logical.LogicalDevice;
import my.game.init.vulkan.math.Vector2fWithSize;
import my.game.init.vulkan.math.Vector3fWithSize;
import my.game.init.vulkan.struct.Vertex;
import org.lwjgl.vulkan.VK10;

import java.util.List;

import static my.game.init.vulkan.struct.Vertex.COLOR_OFFSET;
import static my.game.init.vulkan.struct.Vertex.POSITION_OFFSET;

public class VertexBuffer extends StagingBufferUser {

    public VertexBuffer(LogicalDevice logicalDevice, List<Vertex> vertices, CommandPool commandPool) {
        super(vertices, logicalDevice, VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                commandPool,
                (stagingDataByteBuffer) -> {
                    for (int i = 0; i < vertices.size(); ++i) {
                        Vertex curr = vertices.get(i);
                        Vector2fWithSize currPos = curr.pos();
                        Vector3fWithSize currColor = curr.color();
                        currPos.get(i * curr.getSize() + POSITION_OFFSET, stagingDataByteBuffer);
                        currColor.get(i * curr.getSize() + COLOR_OFFSET, stagingDataByteBuffer);
                    }
                });
    }
}
