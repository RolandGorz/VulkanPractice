package my.game.init.vulkan.devices.logical;

import my.game.init.vulkan.devices.physical.PhysicalDeviceInformation;
import org.lwjgl.vulkan.VkDevice;

public record LogicalDeviceInformation(PhysicalDeviceInformation physicalDeviceInformation, VkDevice vkDevice) {}
