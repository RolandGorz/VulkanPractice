package my.game.init.vulkan.devices.physical;

import my.game.init.vulkan.devices.physical.util.QueueFamilyIndexSet;
import java.util.Set;

public class QueueFamilyIndexes {

    private final int presentationQueueFamilyIndex;
    private final int graphicsQueueFamilyIndex;
    private final boolean isComplete;
    QueueFamilyIndexSet uniqueIndexes = new QueueFamilyIndexSet();

    public static Builder builder() {
        return new Builder();
    }

    private QueueFamilyIndexes(Builder builder) {
        presentationQueueFamilyIndex = builder.presentationQueueFamilyIndexBuilder;
        uniqueIndexes.add(presentationQueueFamilyIndex);
        graphicsQueueFamilyIndex = builder.graphicsQueueFamilyIndexBuilder;
        uniqueIndexes.add(graphicsQueueFamilyIndex);
        isComplete = builder.isComplete();
    }

    public int presentationQueueFamilyIndex() {
        return presentationQueueFamilyIndex;
    }

    public int graphicsQueueFamilyIndex() {
        return graphicsQueueFamilyIndex;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public Set<Integer> uniqueIndexes() {
        return uniqueIndexes;
    }

    public static class Builder {

        private int graphicsQueueFamilyIndexBuilder = -1;
        private int presentationQueueFamilyIndexBuilder = -1;

        private Builder() {}

        public Builder setGraphicsQueueFamilyIndex(final int graphicsQueueFamilyIndex) {
            graphicsQueueFamilyIndexBuilder = graphicsQueueFamilyIndex;
            return this;
        }

        public Builder setPresentationQueueFamilyIndex(final int presentationQueueFamilyIndex) {
            presentationQueueFamilyIndexBuilder = presentationQueueFamilyIndex;
            return this;
        }

        public boolean isComplete() {
            return graphicsQueueFamilyIndexBuilder != -1 && presentationQueueFamilyIndexBuilder != -1;
        }

        public QueueFamilyIndexes build() {
            return new QueueFamilyIndexes(this);
        }
    }
}
