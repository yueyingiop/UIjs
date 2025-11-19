package com.core.UIJS.network;

import java.util.function.Supplier;

import com.core.UIJS.ui.UIRegistry;
import com.core.UIJS.ui.UIRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class OpenUIPacket {
    private final ResourceLocation uiId;
    private final BlockPos blockPos;
    private final ResourceLocation dimension;

    // 普通UI构造函数
    public OpenUIPacket(ResourceLocation uiId) {
        this.uiId = uiId;
        this.blockPos = null;
        this.dimension = null;
    }

    // 方块绑定UI构造函数
    public OpenUIPacket(ResourceLocation uiId, BlockPos blockPos, ResourceLocation dimension) {
        this.uiId = uiId;
        this.blockPos = blockPos;
        this.dimension = dimension;
    }

    public OpenUIPacket(FriendlyByteBuf buf) {
        this.uiId = buf.readResourceLocation();
        
        // 读取是否有方块绑定信息
        boolean hasBlockBinding = buf.readBoolean();
        if (hasBlockBinding) {
            this.blockPos = buf.readBlockPos();
            this.dimension = buf.readResourceLocation();
        } else {
            this.blockPos = null;
            this.dimension = null;
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(uiId);
        
        // 写入是否有方块绑定信息
        boolean hasBlockBinding = (blockPos != null && dimension != null);
        buf.writeBoolean(hasBlockBinding);
        
        if (hasBlockBinding) {
            buf.writeBlockPos(blockPos);
            buf.writeResourceLocation(dimension);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            // 在客户端打开UI
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                if (Minecraft.getInstance().level != null) {
                    // 判断UI是否为方块绑定
                    boolean isBlockBound = UIRegistry.isBlockBoundUI(uiId);
                    if (isBlockBound && blockPos != null && dimension != null) {
                        // 使用方块绑定的构造函数
                        UIRenderer.openUI(uiId, blockPos, dimension);
                    } else {
                        // 使用普通构造函数
                        UIRenderer.openUI(uiId);
                    }
                }
            });
        });
        context.get().setPacketHandled(true);
    }

    public ResourceLocation getuiId() {
        return uiId;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public ResourceLocation getDimension() {
        return dimension;
    }
}
