package com.core.UIJS;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.core.UIJS.item.RegistryItem;
import com.core.UIJS.kubejs.ArmorSetRegistry;

@Mod(UIJS.MODID)
public class UIJS 
{

    public static final String MODID = "ui_js";
    public static final Logger LOGGER = LogManager.getLogger(UIJS.MODID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final RegistryObject<CreativeModeTab> UI_JS_TAB = CREATIVE_MODE_TABS.register("ui_js_tab", () -> CreativeModeTab.builder()
        .withTabsBefore(CreativeModeTabs.COMBAT)
        .title(Component.translatable("itemGroup.ui_js_tab"))
        .icon(() -> RegistryItem.PHONE.get().getDefaultInstance())
        .displayItems((parameters, output) -> {
            output.accept(RegistryItem.PHONE.get());
        })
        .build()
    );


    public UIJS(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();
        RegistryItem.ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(this);
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        ArmorSetRegistry.REGISTER.post(new ArmorSetRegistry.RegisterEvent());
    }


}
