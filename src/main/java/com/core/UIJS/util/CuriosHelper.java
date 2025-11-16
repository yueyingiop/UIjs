package com.core.UIJS.util;

import java.util.Map;
import java.util.Optional;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public class CuriosHelper {
    // 判断是否拥有指定Curios物品
    public static boolean getCuriosItem(LivingEntity entity, String curiosSlot, String itemId) {
        Optional<ICuriosItemHandler> curiosHandlerOptional = CuriosApi.getCuriosInventory(entity).resolve();
        if (curiosHandlerOptional.isPresent()) {
            ICuriosItemHandler curiosHandler = curiosHandlerOptional.get();
            Map<String, ICurioStacksHandler> curios = curiosHandler.getCurios();
            ICurioStacksHandler stackHandler = curios.get(curiosSlot);

            if (stackHandler != null) {
                IDynamicStackHandler stacks = stackHandler.getStacks();
                int slots = stacks.getSlots();
                for (int i = 0; i < slots; i++) {
                    ItemStack stack = stacks.getStackInSlot(i);
                    
                    if (!stack.isEmpty()) {
                        if (ForgeRegistries.ITEMS.getKey(stack.getItem()).toString().equals(itemId)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    // 获取指定插槽饰品
    public static ItemStack getCurio(Player player, String curiosSlot, int index) { 
        Optional<ICuriosItemHandler> curiosHandler = CuriosApi.getCuriosInventory(player).resolve();
        if (curiosHandler.isPresent()) {
            Map<String, ICurioStacksHandler> stacksHandlers = curiosHandler.get().getCurios();
            ICurioStacksHandler haloHandler = stacksHandlers.get(curiosSlot);
            if (haloHandler != null) {
                IDynamicStackHandler stacks = haloHandler.getStacks();
                return stacks.getStackInSlot(index);
            }
        }
        return ItemStack.EMPTY;
    }
}
