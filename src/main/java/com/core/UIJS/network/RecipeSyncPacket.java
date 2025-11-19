package com.core.UIJS.network;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class RecipeSyncPacket {
    private final String group;
    private final int currentTick;
    private final int totalTicks;
    private final boolean isProcessing;
    
    public RecipeSyncPacket(String group, int currentTick, int totalTicks, boolean isProcessing) {
        this.group = group;
        this.currentTick = currentTick;
        this.totalTicks = totalTicks;
        this.isProcessing = isProcessing;
    }
    
    public static void encode(RecipeSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.group);
        buffer.writeInt(packet.currentTick);
        buffer.writeInt(packet.totalTicks);
        buffer.writeBoolean(packet.isProcessing);
    }
    
    public static RecipeSyncPacket decode(FriendlyByteBuf buffer) {
        return new RecipeSyncPacket(
            buffer.readUtf(),
            buffer.readInt(),
            buffer.readInt(),
            buffer.readBoolean()
        );
    }
    
    public static void handle(RecipeSyncPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {

        });
        context.get().setPacketHandled(true);
    }
}
