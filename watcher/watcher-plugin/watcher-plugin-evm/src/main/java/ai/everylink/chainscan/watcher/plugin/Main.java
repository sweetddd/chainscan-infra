package ai.everylink.chainscan.watcher.plugin;

import ai.everylink.chainscan.watcher.core.util.JDBCUtils;
import ai.everylink.chainscan.watcher.core.util.OkHttpUtil;
import ai.everylink.chainscan.watcher.core.util.WatcherUtils;
import ai.everylink.chainscan.watcher.entity.Transaction;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mysql.cj.protocol.Resultset;
import okhttp3.OkHttpClient;
import org.apache.commons.compress.utils.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;

import javax.sql.rowset.serial.SerialBlob;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Main
 *
 * @author: david.zhanghui@everylink.ai
 */
public class Main {
    private static Logger logger = LoggerFactory.getLogger(Main.class);
    private static String rpcUrl = "https://rinkeby.infura.io/v3/b9769dd0ae344706b1cebca95e52e887";
    public static void main(String[] args) throws Exception {

//        OkHttpClient httpClient = OkHttpUtil.buildOkHttpClient();
//        HttpService httpService = new HttpService(rpcUrl, httpClient, false);
//        Web3j web3j = Web3j.build(httpService);
//        EthBlock block = web3j.ethGetBlockByNumber(
//                new DefaultBlockParameterNumber(1719600L), true).send();
//
//        String txHash = "0x0adde1ce5805d0a0980938ea4072a54670441db367bff705201c39ec4da1b854";
//        Optional<Transaction> tx = web3j.ethGetTransactionByHash(txHash).send().getTransaction();
//        EthGetTransactionReceipt receipt = web3j.ethGetTransactionReceipt(txHash).send();
//        System.out.println(block.getBlock());

        currentUpdate();
    }


    private static void currentUpdate() throws Exception {
        long min = 4000;
        long max = 80000000;
        long step = WatcherUtils.getWatcherFixTxBatchSize();
        long batch = max / step;

        Connection conn = null;
        PreparedStatement pst = null;
        try {
            conn = JDBCUtils.getConnection();
            String sql = "select max(tid) from tid";
            pst = conn.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                min = rs.getLong(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            // 6. 释放资源
            JDBCUtils.close(pst,conn);
        }

        for (long i = 0; i < batch; i++) {
            long start = min + i*step;
            long end = start + step;

            // use origin jdbc
            Connection connection = null;
            PreparedStatement preparedStatement = null;
            try {
                connection = JDBCUtils.getConnection();
                String sql = "update transaction set input_method='', input_params='' where id>=? and id<?";
                preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setLong(1, start);
                preparedStatement.setLong(2, end);
                int rows = preparedStatement.executeUpdate();
                System.out.println("update: " + rows);
            } catch (SQLException e) {
                e.printStackTrace();
            }finally {
                // 6. 释放资源
                JDBCUtils.close(preparedStatement,connection);
            }
        }
    }
}
