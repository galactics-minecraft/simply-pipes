package com.omarlawatey.compat.jade;

import com.omarlawatey.blocks.pipe.Pipe;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class JadePlugin implements IWailaPlugin {
  @Override
  public void register(IWailaCommonRegistration registration) {
    // Register server data provider for syncing block entity data
    registration.registerBlockDataProvider(JadeComponentProvider.INSTANCE, Pipe.class);
  }

  @Override
  public void registerClient(IWailaClientRegistration registration) {
    // Register component provider for Pipe block
    registration.registerBlockComponent(JadeComponentProvider.INSTANCE, Pipe.class);
  }
}
