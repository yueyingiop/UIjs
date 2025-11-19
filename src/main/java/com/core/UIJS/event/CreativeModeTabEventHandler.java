package com.core.UIJS.event;

import com.core.UIJS.UIJS;
import com.core.UIJS.item.RegistryItem;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = UIJS.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CreativeModeTabEventHandler {
    @SubscribeEvent
    public static void addTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(RegistryItem.PHONE.get());
        }
    }
}
