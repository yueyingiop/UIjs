package com.core.UIJS;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    public static final ForgeConfigSpec SPEC;
    public static final Config INSTANCE;
    
    static {
        Pair<Config, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Config::new);
        SPEC = specPair.getRight();
        INSTANCE = specPair.getLeft();
    }
    
    public final ForgeConfigSpec.ConfigValue<String> mcmlDirectory;
    
    public Config(ForgeConfigSpec.Builder builder) {
        builder.comment("UIJS Configuration").push("uijs");
        
        mcmlDirectory = builder
            .comment("Directory where MCML files are stored")
            .define("mcmlDirectory", "kubejs/mcml");
        
        builder.pop();
    }
}
