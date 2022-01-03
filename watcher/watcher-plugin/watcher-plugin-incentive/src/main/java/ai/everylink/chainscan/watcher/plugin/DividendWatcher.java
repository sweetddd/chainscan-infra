package ai.everylink.chainscan.watcher.plugin;

import ai.everylink.chainscan.watcher.core.IWatcher;
import ai.everylink.chainscan.watcher.core.IWatcherPlugin;
import ai.everylink.chainscan.watcher.core.WatcherExecutionException;
import ai.everylink.chainscan.watcher.core.util.SpringApplicationUtils;
import ai.everylink.chainscan.watcher.entity.IncentiveBlock;

import ai.everylink.chainscan.watcher.plugin.service.IncentiveService;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DividendWatcher implements IWatcher {

    private static final int BLOCK_SIZE = 144;
    private static final Integer pageSize = 287;
    private static final String SYMBOL = "MOBI";
    private static final String NETWORK = "VM30";
    private final Logger logger = LoggerFactory.getLogger(DividendWatcher.class);
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
    public String getCron() {
        return "0/4 * * * * ? ";
    }

    @Override
    public List<IWatcherPlugin> getOrderedPluginList() {
        return Lists.newArrayList(new DividendPlugin());
    }

    @Override
    public List<IncentiveBlock> scanBlock() throws WatcherExecutionException {
//        init();
//        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@");
//        System.out.println("########: " + incentiveService.incentiveLastBlockScan());
//        List<IncentiveBlock> incentiveBlocks = incentiveService.incentiveBlocksScan(pageSize);
//
//        incentiveBlocks = incentiveBlocks.stream().sorted(Comparator.comparing(IncentiveBlock::getBlockHeight)).collect(Collectors.toList());
//        if (!incentiveBlocks.isEmpty()) {
//            incentiveBlocks.remove(incentiveBlocks.size() - 1);
//            IncentiveBlock firstBlock = incentiveBlocks.get(0);
//            Long firstBlockHeight = firstBlock.getBlockHeight();
//            Long startIndex = compureStartBlockIndex(firstBlockHeight);
//            Long endIndex = startIndex + BLOCK_SIZE;
//            incentiveBlocks = incentiveBlocks.stream().filter(i-> startIndex <= i.getBlockHeight() &&  i.getBlockHeight() <= endIndex).collect(Collectors.toList());
//            if (incentiveBlocks.size() == BLOCK_SIZE) {
//                String miningDetails = startIndex + "-" + endIndex;
//                BigDecimal earnings = BigDecimal.ZERO;
//                BigDecimal volume = BigDecimal.ZERO;
//                Long transactionTotal = 0L;
//                BigDecimal miningEarnings = BigDecimal.ZERO;
//                BigDecimal rigidPrice = BigDecimal.ZERO;
//                for (IncentiveBlock incentiveBlock : incentiveBlocks) {
//                    volume = volume.add(incentiveBlock.getBlockedFee());
////                    earnings = earnings
//                    transactionTotal += incentiveBlock.getExtrinsics().size();
//
//                }
//            }
//        }
//        List<IncentiveBlock> result = new ArrayList<>(incentiveBlocks);
        return new ArrayList<>();
    }

    @Override
    public void finalizedBlockStatus() {

    }

    public static void main(String[] args) {
        int test = 1;
        boolean res = test % 144 == 1;
        System.out.println(res);
    }

    public Long compureStartBlockIndex (Long firstIndex) {
        Long mod = (long) (firstIndex / BLOCK_SIZE);
        return (mod + 1) * BLOCK_SIZE + 1;
    }
}
