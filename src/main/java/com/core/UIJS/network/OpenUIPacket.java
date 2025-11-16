package com.core.UIJS.network;

import java.util.function.Supplier;

import com.core.UIJS.ui.UIRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class OpenUIPacket {
    private final ResourceLocation uiId;

    public OpenUIPacket(ResourceLocation uiId) {
        this.uiId = uiId;
    }

    public OpenUIPacket(FriendlyByteBuf buf) {
        this.uiId = buf.readResourceLocation();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(uiId);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            // 在客户端打开UI
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                if (Minecraft.getInstance().level != null) {
                    UIRenderer.openUI(uiId);
                }
            });
        });
        context.get().setPacketHandled(true);
    }

    public ResourceLocation getuiId() {
        return uiId;
    }
}
