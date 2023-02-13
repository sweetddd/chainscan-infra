package ai.everylink.chainscan.watcher.plugin.strategy;

import ai.everylink.chainscan.watcher.core.util.VM30Utils;
import ai.everylink.chainscan.watcher.dao.NftAccountDao;
import ai.everylink.chainscan.watcher.dao.NftAuctionDao;
import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public abstract class ErcTokenBaseService implements ErcTokenService {

    @Autowired
    protected VM30Utils vm30Utils;
    @Autowired
    protected Environment environment;
    @Autowired
    protected NftAccountDao nftAccountDao;
    @Autowired
    protected NftAuctionDao nftAuctionDao;

    protected String ipfs = "ipfs://";

    protected String ipfsDomain = "https://ipfs.io/ipfs/";

    protected boolean isIpfs(String url){
        return url.toLowerCase().startsWith(ipfs);
    }

    protected String ipfsToHttps(String url){
        return ipfsDomain + url.replace(ipfs, StrUtil.EMPTY);
    }

}
