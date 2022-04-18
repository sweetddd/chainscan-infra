package ai.everylink.chainscan.watcher.plugin;

import ai.everylink.chainscan.watcher.core.util.JDBCUtils;
import ai.everylink.chainscan.watcher.core.util.JDBCUtils2;
import ai.everylink.chainscan.watcher.core.util.OkHttpUtil;
import ai.everylink.chainscan.watcher.core.util.WatcherUtils;
import ai.everylink.chainscan.watcher.entity.Transaction;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mysql.cj.protocol.Resultset;
import okhttp3.OkHttpClient;
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
import java.math.BigInteger;
import java.sql.*;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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

        transactionFix();
    }

    /**
     * rinkeby transaction表数据修复
     */
    private static AtomicBoolean init = new AtomicBoolean(false);
    private static void transactionFix() {
        long max = 68600000;
        long step = WatcherUtils.getWatcherFixTxBatchSize();
        while (true) {
            long start = getMaxTid();
            if (start <= 0) {
                logger.error("[watcher_fix]incorrect start:{}", start);
                break;
            }
            long end = start + 100;
            if (end > max) {
                logger.error("[watcher_fix]done.end:{}", end);
                break;
            }

            logger.info("[watcher_fix]begin to fix. step:{},start:{},end:{}", step, start, end);


            List<ai.everylink.chainscan.watcher.entity.Transaction> txList = Lists.newArrayList();

            Connection connection = null;
            PreparedStatement preparedStatement = null;
            try {
                connection = JDBCUtils.getConnection();
                String sql = "select * from transaction where id>=? and id<?";
                preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setLong(1, start);
                preparedStatement.setLong(2, end);
                ResultSet rs = preparedStatement.executeQuery();
                while (rs.next()) {
                    ai.everylink.chainscan.watcher.entity.Transaction tx = new ai.everylink.chainscan.watcher.entity.Transaction();
                    tx.setTransactionHash(rs.getString("transaction_hash"));
                    tx.setTransactionIndex(getResultSetInt(rs.getObject("transaction_index")));
                    tx.setBlockHash(rs.getString("block_hash"));
                    tx.setBlockNumber(rs.getLong("block_number"));
                    tx.setChainId(rs.getInt("chain_id"));
                    tx.setTxTimestamp(rs.getDate("tx_timestamp"));
                    tx.setFromAddr(rs.getString("from_addr"));
                    tx.setToAddr(rs.getString("to_addr"));
                    tx.setContractAddress(rs.getString("contract_address"));
                    tx.setValue(rs.getString("value"));
                    tx.setTxFee(rs.getString("tx_fee"));
                    tx.setGasLimit(getBigInteger(rs.getObject("gas_limit")));
                    tx.setGasUsed(getBigInteger(rs.getObject("gas_used")));
                    tx.setGasPrice(rs.getString("gas_price"));
                    tx.setNonce(rs.getString("nonce"));
                    tx.setInput(rs.getString("input"));
                    tx.setTxType(getResultSetInt(rs.getString("tx_type")));
                    txList.add(tx);
                }
                updateTid(end);
            } catch (SQLException e) {
                e.printStackTrace();
            }finally {
                // 6. 释放资源
                JDBCUtils.close(preparedStatement,connection);
            }

            connection = null;
            preparedStatement = null;
            try {
                connection = JDBCUtils2.getConnection();
                String sql = "INSERT INTO transaction (transaction_hash, transaction_index, block_hash, block_number, chain_id, status, fail_msg, tx_timestamp, " +
                        "from_addr, to_addr, contract_address, value, tx_fee, gas_limit, gas_used, gas_price, nonce, input,tx_type, " +
                        "create_time, chain_type, token_tag) " +
                        " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
                preparedStatement = connection.prepareStatement(sql);
                for (ai.everylink.chainscan.watcher.entity.Transaction b : txList) {
                    preparedStatement.setObject(1, b.getTransactionHash());
                    preparedStatement.setObject(2, b.getTransactionIndex());
                    preparedStatement.setObject(3, b.getBlockHash());
                    preparedStatement.setObject(4, b.getBlockNumber());
                    preparedStatement.setObject(5, b.getChainId());
                    preparedStatement.setObject(6, b.getStatus());
                    preparedStatement.setObject(7, b.getFailMsg());
                    preparedStatement.setObject(8, b.getTxTimestamp());
                    preparedStatement.setObject(9, b.getFromAddr());
                    preparedStatement.setObject(10, b.getToAddr());
                    preparedStatement.setObject(11, b.getContractAddress());
                    preparedStatement.setObject(12, b.getValue());
                    preparedStatement.setObject(13, b.getTxFee());
                    preparedStatement.setObject(14, b.getGasLimit());
                    preparedStatement.setObject(15, b.getGasUsed());
                    preparedStatement.setObject(16, b.getGasPrice());
                    preparedStatement.setObject(17, b.getNonce());
                    preparedStatement.setObject(18, b.getInput());
                    preparedStatement.setObject(19, b.getTxType());
                    preparedStatement.setObject(20, new Date());
                    preparedStatement.setObject(21, "EVM_PoW");
                    preparedStatement.setObject(22, 0);

                    preparedStatement.addBatch();
                }
                preparedStatement.executeBatch();
            } catch (SQLException e) {
                e.printStackTrace();
            }finally {
                // 6. 释放资源
                JDBCUtils2.close(preparedStatement,connection);
            }
        }
    }

    private static Integer getResultSetInt(Object val) {
        if (val == null) {
            return null;
        }

        try {
            return Integer.parseInt(val.toString());
        } catch (Exception e){
        }

        return null;
    }

    private static Long getResultSetLong(Object val) {
        if (val == null) {
            return null;
        }

        try {
            return Long.parseLong(val.toString());
        } catch (Exception e){
        }

        return null;
    }

    private static BigInteger getBigInteger(Object val) {
        if (val == null) {
            return null;
        }

        try {
            return BigInteger.valueOf(Long.parseLong(val.toString()));
        } catch (Exception e){
        }

        return null;
    }

    private static String getResultSetString(Object val) {
        if (val == null) {
            return null;
        }

        try {
            return val.toString();
        } catch (Exception e){
        }

        return null;
    }

    private static long getMaxTid() {
        Connection conn = null;
        PreparedStatement pst = null;
        try {
            conn = JDBCUtils.getConnection();
            String sql = "select max(tid) from tid";
            pst = conn.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            // 6. 释放资源
            JDBCUtils.close(pst,conn);
        }

        return 0;
    }

    private static void updateTid(long tid) {
        Connection conn = null;
        PreparedStatement pst = null;
        try {
            conn = JDBCUtils.getConnection();
            String sql = "insert into tid(tid) values(?)";
            pst = conn.prepareStatement(sql);
            pst.setLong(1, tid);
            int rows = pst.executeUpdate();
            logger.info("[watcher_fix]updateTid.tid={},rows={}", tid, rows);
        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            // 6. 释放资源
            JDBCUtils.close(pst,conn);
        }
    }
}
