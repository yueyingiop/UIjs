package com.core.UIJS;

import com.core.UIJS.kubejs.ArmorSetBuilder;
import com.core.UIJS.kubejs.ArmorSetRegistry;

import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.latvian.mods.kubejs.script.ScriptType;

public class UIJSPlugin extends KubeJSPlugin {

    @Override
    public void registerEvents() {
        ArmorSetRegistry.EVENT_GROUP.register();
    }

    @Override
    public void registerBindings(BindingsEvent event) {
        if (event.getType() == ScriptType.SERVER) {
            event.add("ArmorSetBuilder", ArmorSetBuilder.class);
        }
    }

    @Override
    public void onServerReload() {
        ArmorSetRegistry.clearCache();
    }
}
