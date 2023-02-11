package ai.everylink.chainscan.watcher.plugin.strategy;

import ai.everylink.chainscan.watcher.dao.NftAccountDao;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import cn.hutool.core.util.StrUtil;

import java.math.BigInteger;
import java.util.List;

@Slf4j
@Service
public class ErcToken1155ServiceImpl extends ErcTokenBaseService {

    @Autowired
    private NftAccountDao nftAccountDao;

    @Override
    public ErcTypeTokenEnum type() {
        return ErcTypeTokenEnum.ERC1155;
    }

    @Override
    public boolean isScan() {
        String erc1155NotScanStr = environment.getProperty("watcher.token.not.scan.erc1155");
        boolean erc1155NotScan = Boolean.parseBoolean(erc1155NotScanStr);
        log.info("erc1155NotScanStr:{}, erc1155NotScan:{}", erc1155NotScanStr, erc1155NotScan);
        return !erc1155NotScan;
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
    public Long getAmount(Web3j web3j, String contractAddress, String address, Long tokenId) {
        /*String amount = logData.substring(logData.length() - 64);
        return Long.valueOf(amount, 16);*/
        return vm30Utils.balanceOfErc1155(web3j, contractAddress, address, tokenId, 0).longValue();
    }

    @Override
    public String getNftData(Web3j web3j, String contract, BigInteger tokenId) {
        String tokenURL = vm30Utils.URI(web3j, contract, tokenId);
        log.info("ErcToken1155ServiceImpl.getNftData:{}", tokenURL);
        return tokenURL;
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

    //@Override
    /*public void updateAmount(Web3j web3j, String nftContractAddress, String address, Long accountId, Long tokenId, Long nftId) {
        BigInteger amount = vm30Utils.balanceOf(web3j, nftContractAddress, address);
        System.out.println(amount);
        int count = nftAccountDao.updateAmount(accountId, tokenId, nftId, amount);
        System.out.println(count);
    }*/

}
