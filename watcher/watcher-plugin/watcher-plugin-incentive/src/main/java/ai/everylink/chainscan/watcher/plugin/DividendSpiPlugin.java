package ai.everylink.chainscan.watcher.plugin;

import ai.everylink.chainscan.watcher.core.IEvmWatcherPlugin;
import ai.everylink.chainscan.watcher.core.WatcherExecutionException;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author jerry
 * @since 2021-12-24
 */
@Slf4j
public class DividendSpiPlugin implements IEvmWatcherPlugin {

    @Override
    public <T> boolean processBlock(T block) throws WatcherExecutionException {
        return false;
    }

}
