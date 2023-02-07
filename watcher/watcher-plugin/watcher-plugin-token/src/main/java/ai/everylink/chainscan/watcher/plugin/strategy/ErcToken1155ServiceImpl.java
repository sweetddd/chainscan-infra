package ai.everylink.chainscan.watcher.plugin.strategy;

import ai.everylink.chainscan.watcher.core.enums.ErcTypeEnum;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.protocol.Web3j;
import cn.hutool.core.util.StrUtil;

import java.math.BigInteger;
import java.util.List;

@Slf4j
@Service
public class ErcToken1155ServiceImpl extends ErcTokenBaseService {

    @Override
    public ErcTypeEnum type() {
        return ErcTypeEnum.ERC1155;
    }

    @Override
    public String getFrom(List<String> topics) {
        String topicFrom = topics.get(2);
        topicFrom = "0x"+topicFrom.substring(topicFrom.length()-40);
        return topicFrom;
    }

    @Override
    public String getTo(List<String> topics) {
        String topicTo = topics.get(3);
        topicTo = "0x"+topicTo.substring(topicTo.length()-40);
        return topicTo;
    }

    @Override
    public BigInteger getNftId(List<String> topics, String logData) {
        String nftIdHex = logData.substring(2, 66);
        return new BigInteger(nftIdHex, 16);
    }

    @Override
    public Long getAmount(String logData) {
        String amount = logData.substring(logData.length() - 64);
        return Long.valueOf(amount, 16);
    }

    @Override
    public String getNftData(Web3j web3j, String contract, BigInteger tokenId) {
        Utf8String tokenURL = vm30Utils.URI(web3j, contract, tokenId);
        log.info("ErcToken1155ServiceImpl.getNftData:{}", tokenURL);
        return tokenURL.toString();
    }

    @Override
    public String getNftName(String contractName, String nftData) {
        log.info("ErcToken1155ServiceImpl.getNftName().nftData:{}", nftData);
        if(StrUtil.isBlank(nftData)){
            return StrUtil.EMPTY;
        }
        if(super.isIpfs(nftData)){
            nftData = super.ipfsToHttps(nftData);
        }
        if(HttpUtil.isHttp(nftData) || HttpUtil.isHttps(nftData)){
            try{
                String json = HttpUtil.get(nftData);
                log.info("ErcToken1155ServiceImpl.getNftName().json:{}", json);
                JSONObject jsonObject = JSONObject.parseObject(json);
                return jsonObject.getString("name");
            } catch (Exception e){
                log.error("ErcToken1155ServiceImpl.getNftName().http error!", e);
            }
        }
        return StrUtil.EMPTY;
    }

}
