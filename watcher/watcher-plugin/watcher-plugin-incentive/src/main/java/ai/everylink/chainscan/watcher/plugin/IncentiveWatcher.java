package ai.everylink.chainscan.watcher.plugin;

import ai.everylink.chainscan.watcher.core.IWatcher;
import ai.everylink.chainscan.watcher.core.IWatcherPlugin;
import ai.everylink.chainscan.watcher.core.util.SpringApplicationUtils;
import ai.everylink.chainscan.watcher.entity.Block;
import ai.everylink.chainscan.watcher.entity.IncentiveBlock;
import ai.everylink.chainscan.watcher.plugin.service.IncentiveService;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class IncentiveWatcher implements IWatcher {

    private static final Integer pageSize = 500;

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
        List<IncentiveBlock> result = new ArrayList<>();
        init();
        List<IncentiveBlock> incentiveBlocks = incentiveService.incentiveBlocksScan(pageSize);
        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@: " + incentiveBlocks);
        incentiveBlocks = incentiveBlocks.stream().sorted(Comparator.comparing(IncentiveBlock::getBlockHeight)).collect(Collectors.toList());
        if (!incentiveBlocks.isEmpty()) {
            incentiveBlocks.remove(incentiveBlocks.size() - 1);
            for (IncentiveBlock incentiveBlock : incentiveBlocks) {
                String blockHash  = incentiveBlock.getBlockHash();
                Block  existBlock = incentiveService.selectBlockByBlockHash(blockHash);
                if (existBlock == null){
                    result.add(incentiveBlock);
                }
            }
        }
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
        return "0 0 * * * ?";
        //return "0/4 * * * * ? ";
    }

}
