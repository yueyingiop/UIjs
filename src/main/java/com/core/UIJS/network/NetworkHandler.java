package com.core.UIJS.network;

import com.core.UIJS.UIJS;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        ResourceLocation.fromNamespaceAndPath(UIJS.MODID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        INSTANCE.registerMessage(
            packetId++, SlotSyncPacket.class,
            SlotSyncPacket::encode, 
            SlotSyncPacket::decode, 
            SlotSyncPacket::handle
        );
        INSTANCE.registerMessage(
            packetId++, OpenUIPacket.class,
            OpenUIPacket::encode, 
            OpenUIPacket::new, 
            OpenUIPacket::handle
        );
        INSTANCE.registerMessage(
            packetId++, LoginSyncPacket.class,
            LoginSyncPacket::encode, 
            LoginSyncPacket::decode, 
            LoginSyncPacket::handle
        );
        INSTANCE.registerMessage(
            packetId++, InventorySyncPacket.class,
            InventorySyncPacket::encode, 
            InventorySyncPacket::decode, 
            InventorySyncPacket::handle
        );
        INSTANCE.registerMessage(
            packetId++, RecipeSyncPacket.class,
            RecipeSyncPacket::encode, 
            RecipeSyncPacket::decode, 
            RecipeSyncPacket::handle
        );
    }

    public static void sendToServer(Object packet) {
        INSTANCE.sendToServer(packet);
    }
}
