package com.core.UIJS;

import com.core.UIJS.kubejs.ArmorSetBuilder;
import com.core.UIJS.kubejs.ArmorSetRegistry;
import com.core.UIJS.kubejs.UIJSBindings;
import com.core.UIJS.mcml.MCMLFileScanner;

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
        if (event.getType() == ScriptType.STARTUP) {
            event.add("UIJS", new UIJSBindings());
        }
    }

    @Override
    public void onServerReload() {
        ArmorSetRegistry.clearCache();
        ArmorSetRegistry.REGISTER.post(new ArmorSetRegistry.RegisterEvent());
    }

    @Override
    public void afterInit() {
        MCMLFileScanner.scanAndRegisterMCMLFiles();
    }
}
