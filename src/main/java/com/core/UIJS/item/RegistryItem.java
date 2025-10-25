package com.core.UIJS.item;

import com.core.UIJS.UIJS;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class RegistryItem extends Item {
    public RegistryItem(Properties properties) {
        super(properties);
    }

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, UIJS.MODID);


    public static final RegistryObject<Item> PHONE = ITEMS.register(
        "phone", 
        () -> new RegistryItem(new Item.Properties())
    );
}
