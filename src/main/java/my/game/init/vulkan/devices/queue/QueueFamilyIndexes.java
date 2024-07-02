package my.game.init.vulkan.devices.queue;

import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
public interface QueueFamilyIndexes {
    Optional<Integer> presentationQueueFamilyIndex();
    Optional<Integer> graphicsQueueFamilyIndex();
}
