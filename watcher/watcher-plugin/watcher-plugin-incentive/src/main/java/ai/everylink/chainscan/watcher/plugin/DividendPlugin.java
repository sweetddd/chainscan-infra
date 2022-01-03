package ai.everylink.chainscan.watcher.plugin;

import ai.everylink.chainscan.watcher.core.IWatcherPlugin;
import ai.everylink.chainscan.watcher.core.WatcherExecutionException;

public class DividendPlugin implements IWatcherPlugin {

    @Override
    public <T> boolean processBlock(T block) throws WatcherExecutionException {
        return false;
    }

}
