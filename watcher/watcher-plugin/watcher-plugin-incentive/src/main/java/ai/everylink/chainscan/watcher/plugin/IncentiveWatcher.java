package ai.everylink.chainscan.watcher.plugin;

import ai.everylink.chainscan.watcher.core.IWatcher;
import ai.everylink.chainscan.watcher.core.IWatcherPlugin;
import ai.everylink.chainscan.watcher.core.util.SpringApplicationUtils;
import ai.everylink.chainscan.watcher.entity.IncentiveBlock;
import ai.everylink.chainscan.watcher.plugin.service.IncentiveService;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

public class IncentiveWatcher implements IWatcher {

    private IncentiveService incentiveService;

    private void init() {
        initService();
    }

    private void initService() {
        if (incentiveService == null) {
            incentiveService = SpringApplicationUtils.getBean(IncentiveService.class);
        }
    }

    @Override
    public List<IncentiveBlock> scanBlock() {
        init();
        List<IncentiveBlock> result = new ArrayList<>();
        IncentiveBlock incentiveBlocks = incentiveService.incentiveLastBlockScan();
        result.add(incentiveBlocks);
        return result;
    }


    @Override
    public List<IWatcherPlugin> getOrderedPluginList() {
        return Lists.newArrayList(new IncentivePlugin());
    }

    @Override
    public void finalizedBlockStatus() {

    }

    @Override
    public String getCron() {
//        return "0 0 * * * ?";
        return "0/4 * * * * ? ";
    }

}
