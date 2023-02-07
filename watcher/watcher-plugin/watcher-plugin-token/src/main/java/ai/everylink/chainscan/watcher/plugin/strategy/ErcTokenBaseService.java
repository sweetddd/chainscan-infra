package ai.everylink.chainscan.watcher.plugin.strategy;

import ai.everylink.chainscan.watcher.core.util.VM30Utils;
import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public abstract class ErcTokenBaseService implements ErcTokenService {

    @Autowired
    protected VM30Utils vm30Utils;

    protected String ipfs = "ipfs://";

    protected String ipfsDomain = "https://ipfs.io/ipfs/";

    protected boolean isIpfs(String url){
        return url.toLowerCase().startsWith(ipfs);
    }

    protected String ipfsToHttps(String url){
        return ipfsDomain + url.replace(ipfs, StrUtil.EMPTY);
    }

}
