package com.corectdelay;

import net.fabricmc.api.ClientModInitializer;

public class CorectDelayClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CorectDelayConfig.load();
    }
}
