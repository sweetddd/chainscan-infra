package ai.everylink.chainscan.watcher.plugin.strategy;

import ai.everylink.chainscan.watcher.core.config.DataSourceEnum;
import ai.everylink.chainscan.watcher.core.config.TargetDataSource;
import ai.everylink.chainscan.watcher.core.util.VM30Utils;
import ai.everylink.chainscan.watcher.dao.AccountInfoDao;
import ai.everylink.chainscan.watcher.dao.NftAccountDao;
import ai.everylink.chainscan.watcher.dao.NftAuctionDao;
import ai.everylink.chainscan.watcher.dao.TokenInfoDao;
import ai.everylink.chainscan.watcher.entity.NftAuction;
import ai.everylink.chainscan.watcher.plugin.service.NFTAuctionService;
import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public abstract class ErcNftBaseService implements ErcNftService {

    @Autowired
    protected VM30Utils vm30Utils;
    @Autowired
    protected Environment environment;
    @Autowired
    protected NftAuctionDao nftAuctionDao;
    @Autowired
    protected NFTAuctionService nftAuctionService;
    @Autowired
    protected NftAccountDao nftAccountDao;
    @Autowired
    protected TokenInfoDao tokenInfoDao;
    @Autowired
    protected AccountInfoDao accountInfoDao;

    protected String ipfs = "ipfs://";

    protected String ipfsDomain = "https://ipfs.io/ipfs/";

    protected boolean isIpfs(String url){
        return url.toLowerCase().startsWith(ipfs);
    }

    protected String ipfsToHttps(String url){
        return ipfsDomain + url.replace(ipfs, StrUtil.EMPTY);
    }

    @TargetDataSource(value = DataSourceEnum.marketplace)
    @Override
    public NftAuction getByTxHash(String txHash){
        return nftAuctionDao.getByTxHash(txHash);
    }

}
