package ai.everylink.chainscan.watcher.core.util;

import com.alibaba.druid.pool.DruidDataSourceFactory;
import com.google.common.collect.Maps;
import org.apache.commons.io.IOUtils;

import javax.sql.DataSource;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Map;

/**
 * Druid连接池的工具类
 */

public class JDBCUtils {

    // 1. 定义一个成员变量 DataSource
    private static DataSource dataSource;

    static {
        String url = WatcherUtils.dbUrl();
        String un = WatcherUtils.dbUserName();
        String pw = WatcherUtils.dbPassword();
//        url = "jdbc:mysql://localhost:3306/chainscan_rinkeby?characterEncoding=utf-8&serverTimezone=UTC&useSSL=false";
//        un = "root";
//        pw = "";
        System.out.println("userName:" + un + "; pwd:" + pw + "; url:" + url);
        Map<String, String> map = Maps.newConcurrentMap();
        map.put("driverClassName", "com.mysql.cj.jdbc.Driver");
        map.put("url", url);
        map.put("username", un);
        map.put("password", pw);
        map.put("initialSize", "10");
        map.put("minIdle", "10");
        map.put("maxActive", "200");
        map.put("maxWait", "60000");

        // 2. 获取DataSource
        try {
            dataSource = DruidDataSourceFactory.createDataSource(map);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取连接的方法
     */
    public static Connection getConnection() throws SQLException {
        // 从连接池中取一个连接对象
        return dataSource.getConnection();
    }


    /**
     * 释放资源
     * 执行DML语句的时候需要关闭 statement 和 connection
     *
     * @param statement
     * @param connection
     */
    public static void close(Statement statement, Connection connection) {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }

        if (connection != null) {
            try {
                connection.close();      // 归还到连接池中
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }

    }

    /**
     * 获取连接池的方法
     */
    public static DataSource getDataSource() {
        return dataSource;
    }

    /**
     * 使用新的工具类
     */
    public static void main(String[] args) throws Exception {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            String str = "0x60e060405260396080818152906200297160a03980516200002991600c91602090910190620001fa565b503480156200003757600080fd5b50604051620029aa380380620029aa8339810160408190526200005a916200034b565b82518390839062000073906001906020850190620001fa565b50805162000089906002906020840190620001fa565b5050600b805460ff1916905550620000ac6000620000a66200011e565b62000122565b620000db7f9f2df0fed2c77648de5860a4cc508cd0818c85b8b8a1ab4ceeef8d981c8956a6620000a66200011e565b6200010a7f65d7a28e3265b37a6474929f336521b332c1681b933f6cb9f3376673440d862a620000a66200011e565b620001158162000132565b5050506200045d565b3390565b6200012e828262000147565b5050565b80516200012e90600c906020840190620001fa565b620001538282620001d1565b6200012e576000828152602081815260408083206001600160a01b03851684529091529020805460ff191660011790556200018d6200011e565b6001600160a01b0316816001600160a01b0316837f2f8788117e7eff1d82e926ec794901d17c78024a50270940304540a733656f0d60405160405180910390a45050565b6000918252602082815260408084206001600160a01b0393909316845291905290205460ff1690565b8280546200020890620003d8565b90600052602060002090601f0160209004810192826200022c576000855562000277565b82601f106200024757805160ff191683800117855562000277565b8280016001018555821562000277579182015b82811115620002775782518255916020019190600101906200025a565b506200028592915062000289565b5090565b5b808211156200028557600081556001016200028a565b600082601f830112620002b1578081fd5b81516001600160401b0380821115620002ce57620002ce6200042e565b6040516020601f8401601f1916820181018381118382101715620002f657620002f66200042e565b60405283825285840181018710156200030d578485fd5b8492505b8383101562000330578583018101518284018201529182019162000311565b838311156200034157848185840101525b5095945050505050565b60008060006060848603121562000360578283fd5b83516001600160401b038082111562000377578485fd5b6200038587838801620002a0565b945060208601519150808211156200039b578384fd5b620003a987838801620002a0565b93506040860151915080821115620003bf578283fd5b50620003ce86828701620002a0565b9150509250925092565b600281046001821680620003ed57607f821691505b6020821081141562000428577f4e487b7100000000000000000000000000000000000000000000000000000000600052602260045260246000fd5b50919050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052604160045260246000fd5b612504806200046d6000396000f3fe608060405234801561001057600080fd5b50600436106101cf5760003560e01c80635c975abb11610104578063a22cb465116100a2578063d539139311610071578063d5391393146103a1578063d547741f146103a9578063e63ab1e9146103bc578063e985e9c5146103c4576101cf565b8063a22cb46514610355578063b88d4fde14610368578063c87b56dd1461037b578063d3fc98641461038e576101cf565b80638456cb59116100de5780638456cb591461032a57806391d148541461033257806395d89b4114610345578063a217fddf1461034d576101cf565b80635c975abb146102fc5780636352211e1461030457806370a0823114610317576101cf565b80632f2ff15d116101715780633f4ba83a1161014b5780633f4ba83a146102bb57806342842e0e146102c357806342966c68146102d65780634f6ccce7146102e9576101cf565b80632f2ff15d146102825780632f745c591461029557806336568abe146102a8576101cf565b8063095ea7b3116101ad578063095ea7b31461023257806318160ddd1461024757806323b872dd1461025c578063248a9ca31461026f576101cf565b806301ffc9a7146101d457806306fdde03146101fd578063081812fc14610212575b600080fd5b6101e76101e2366004611b13565b6103d7565b6040516101f49190611c3d565b60405180910390f35b6102056103ea565b6040516101f49190611c51565b610225610220366004611ad9565b61047c565b6040516101f49190611bec565b610245610240366004611a48565b6104c8565b005b61024f610560565b6040516101f49190611c48565b61024561026a36600461195a565b610566565b61024f61027d366004611ad9565b61059e565b610245610290366004611af1565b6105b3565b61024f6102a3366004611a48565b6105d7565b6102456102b6366004611af1565b610629565b61024561066f565b6102456102d136600461195a565b6106c1565b6102456102e4366004611ad9565b6106dc565b61024f6102f7366004611ad9565b61070f565b6101e761076a565b610225610312366004611ad9565b610773565b61024f61032536600461190e565b6107a8565b6102456107ec565b6101e7610340366004611af1565b61083c565b610205610865565b61024f610874565b610245610363366004611a0e565b610879565b610245610376366004611995565b610947565b610205610389366004611ad9565b610986565b61024561039c366004611a71565b610a4b565b61024f610aa7565b6102456103b7366004611af1565b610acb565b61024f610aea565b6101e76103d2366004611928565b610b0e565b60006103e282610b3c565b90505b919050565b6060600180546103f990612451565b80601f016020809104026020016040519081016040528092919081815260200182805461042590612451565b80156104725780601f1061044757610100808354040283529160200191610472565b820191906000526020600020905b81548152906001019060200180831161045557829003601f168201915b5050505050905090565b600061048782610b61565b6104ac5760405162461bcd60e51b81526004016104a3906120b6565b60405180910390fd5b506000908152600560205260409020546001600160a01b031690565b60006104d382610773565b9050806001600160a01b0316836001600160a01b031614156105075760405162461bcd60e51b81526004016104a3906121e6565b806001600160a01b0316610519610b7e565b6001600160a01b031614806105355750610535816103d2610b7e565b6105515760405162461bcd60e51b81526004016104a390611f91565b61055b8383610b82565b505050565b60095490565b610577610571610b7e565b82610bf0565b6105935760405162461bcd60e51b81526004016104a390612227565b61055b838383610c75565b60009081526020819052604090206001015490565b6105bc8261059e565b6105cd816105c8610b7e565b610da2565b61055b8383610e06565b60006105e2836107a8565b82106106005760405162461bcd60e51b81526004016104a390611d12565b506001600160a01b03919091166000908152600760209081526040808320938352929052205490565b610631610b7e565b6001600160a01b0316816001600160a01b0316146106615760405162461bcd60e51b81526004016104a390612314565b61066b8282610e8b565b5050565b61069b7f65d7a28e3265b37a6474929f336521b332c1681b933f6cb9f3376673440d862a610340610b7e565b6106b75760405162461bcd60e51b81526004016104a390611f0a565b6106bf610f0e565b565b61055b83838360405180602001604052806000815250610947565b6106e7610571610b7e565b6107035760405162461bcd60e51b81526004016104a3906122c4565b61070c81610f7c565b50565b6000610719610560565b82106107375760405162461bcd60e51b81526004016104a390612278565b6009828154811061075857634e487b7160e01b600052603260045260246000fd5b90600052602060002001549050919050565b600b5460ff1690565b6000818152600360205260408120546001600160a01b0316806103e25760405162461bcd60e51b81526004016104a390612038565b60006001600160a01b0382166107d05760405162461bcd60e51b81526004016104a390611fee565b506001600160a01b031660009081526004602052604090205490565b6108187f65d7a28e3265b37a6474929f336521b332c1681b933f6cb9f3376673440d862a610340610b7e565b6108345760405162461bcd60e51b81526004016104a390611ead565b6106bf611023565b6000918252602082815260408084206001600160a01b0393909316845291905290205460ff1690565b6060600280546103f990612451565b600081565b610881610b7e565b6001600160a01b0316826001600160a01b031614156108b25760405162461bcd60e51b81526004016104a390611e2a565b80600660006108bf610b7e565b6001600160a01b03908116825260208083019390935260409182016000908120918716808252919093529120805460ff191692151592909217909155610903610b7e565b6001600160a01b03167f17307eab39ab6107e8899845ad3d59bd9653f200f220920489ca2b5937696c318360405161093b9190611c3d565b60405180910390a35050565b610958610952610b7e565b83610bf0565b6109745760405162461bcd60e51b81526004016104a390612227565b6109808484848461107e565b50505050565b606061099182610b61565b6109ad5760405162461bcd60e51b81526004016104a390612197565b6000828152600d6020526040902080546109c690612451565b80601f01602080910402602001604051908101604052809291908181526020018280546109f290612451565b8015610a3f5780601f10610a1457610100808354040283529160200191610a3f565b820191906000526020600020905b815481529060010190602001808311610a2257829003601f168201915b50505050509050919050565b610a777f9f2df0fed2c77648de5860a4cc508cd0818c85b8b8a1ab4ceeef8d981c8956a6610340610b7e565b610a935760405162461bcd60e51b81526004016104a390612363565b610a9d83836110b1565b61055b8282611190565b7f9f2df0fed2c77648de5860a4cc508cd0818c85b8b8a1ab4ceeef8d981c8956a681565b610ad48261059e565b610ae0816105c8610b7e565b61055b8383610e8b565b7f65d7a28e3265b37a6474929f336521b332c1681b933f6cb9f3376673440d862a81565b6001600160a01b03918216600090815260066020908152604080832093909416825291909152205460ff1690565b60006001600160e01b0319821663780e9d6360e01b14806103e257506103e2826111d4565b6000908152600360205260409020546001600160a01b0316151590565b3390565b600081815260056020526040902080546001600160a01b0319166001600160a01b0384169081179091558190610bb782610773565b6001600160a01b03167f8c5be1e5ebec7d5bd14f71427d1e84f3dd0314c0f7b2291e5b200ac8c7c3b92560405160405180910390a45050565b6000610bfb82610b61565b610c175760405162461bcd60e51b81526004016104a390611e61565b6000610c2283610773565b9050806001600160a01b0316846001600160a01b03161480610c5d5750836001600160a01b0316610c528461047c565b6001600160a01b0316145b80610c6d5750610c6d8185610b0e565b949350505050565b826001600160a01b0316610c8882610773565b6001600160a01b031614610cae5760405162461bcd60e51b81526004016104a39061214e565b6001600160a01b038216610cd45760405162461bcd60e51b81526004016104a390611de6565b610cdf838383611214565b610cea600082610b82565b6001600160a01b0383166000908152600460205260408120805460019290610d139084906123f7565b90915550506001600160a01b0382166000908152600460205260408120805460019290610d419084906123c0565b909155505060008181526003602052604080822080546001600160a01b0319166001600160a01b0386811691821790925591518493918716917fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef91a4505050565b610dac828261083c565b61066b57610dc4816001600160a01b0316601461121f565b610dcf83602061121f565b604051602001610de0929190611b77565b60408051601f198184030181529082905262461bcd60e51b82526104a391600401611c51565b610e10828261083c565b61066b576000828152602081815260408083206001600160a01b03851684529091529020805460ff19166001179055610e47610b7e565b6001600160a01b0316816001600160a01b0316837f2f8788117e7eff1d82e926ec794901d17c78024a50270940304540a733656f0d60405160405180910390a45050565b610e95828261083c565b1561066b576000828152602081815260408083206001600160a01b03851684529091529020805460ff19169055610eca610b7e565b6001600160a01b0316816001600160a01b0316837ff6391f5c32d9c69d2a47ea670b442974b53935d1edc7fd64eb21e047a839171b60405160405180910390a45050565b610f1661076a565b610f325760405162461bcd60e51b81526004016104a390611ce4565b600b805460ff191690557f5db9ee0a495bf2e6ff9c91a7834c1ba4fdd244a5e8aa4e537bd38aeae4b073aa610f65610b7e565b604051610f729190611bec565b60405180910390a1565b6000610f8782610773565b9050610f9581600084611214565b610fa0600083610b82565b6001600160a01b0381166000908152600460205260408120805460019290610fc99084906123f7565b909155505060008281526003602052604080822080546001600160a01b0319169055518391906001600160a01b038416907fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef908390a45050565b61102b61076a565b156110485760405162461bcd60e51b81526004016104a390611f67565b600b805460ff191660011790557f62e78cea01bee320cd4e420270b5ea74000d11b0c9f74754ebdbfc544b05a258610f65610b7e565b611089848484610c75565b611095848484846113d8565b6109805760405162461bcd60e51b81526004016104a390611d5d565b6001600160a01b0382166110d75760405162461bcd60e51b81526004016104a390612081565b6110e081610b61565b156110fd5760405162461bcd60e51b81526004016104a390611daf565b61110960008383611214565b6001600160a01b03821660009081526004602052604081208054600192906111329084906123c0565b909155505060008181526003602052604080822080546001600160a01b0319166001600160a01b03861690811790915590518392907fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef908290a45050565b61119982610b61565b6111b55760405162461bcd60e51b81526004016104a390612102565b6000828152600d60209081526040909120825161055b928401906117ee565b60006001600160e01b031982166380ac58cd60e01b148061120557506001600160e01b03198216635b5e139f60e01b145b806103e257506103e2826114f3565b61055b838383611518565b6060600061122e8360026123d8565b6112399060026123c0565b67ffffffffffffffff81111561125f57634e487b7160e01b600052604160045260246000fd5b6040519080825280601f01601f191660200182016040528015611289576020820181803683370190505b509050600360fc1b816000815181106112b257634e487b7160e01b600052603260045260246000fd5b60200101906001600160f81b031916908160001a905350600f60fb1b816001815181106112ef57634e487b7160e01b600052603260045260246000fd5b60200101906001600160f81b031916908160001a90535060006113138460026123d8565b61131e9060016123c0565b90505b60018111156113b2576f181899199a1a9b1b9c1cb0b131b232b360811b85600f166010811061136057634e487b7160e01b600052603260045260246000fd5b1a60f81b82828151811061138457634e487b7160e01b600052603260045260246000fd5b60200101906001600160f81b031916908160001a90535060049490941c936113ab8161243a565b9050611321565b5083156113d15760405162461bcd60e51b81526004016104a390611c64565b9392505050565b60006113ec846001600160a01b0316611548565b156114e857836001600160a01b031663150b7a02611408610b7e565b8786866040518563ffffffff1660e01b815260040161142a9493929190611c00565b602060405180830381600087803b15801561144457600080fd5b505af1925050508015611474575060408051601f3d908101601f1916820190925261147191810190611b2f565b60015b6114ce573d8080156114a2576040519150601f19603f3d011682016040523d82523d6000602084013e6114a7565b606091505b5080516114c65760405162461bcd60e51b81526004016104a390611d5d565b805181602001fd5b6001600160e01b031916630a85bd0160e11b149050610c6d565b506001949350505050565b60006001600160e01b03198216637965db0b60e01b14806103e257506103e28261154e565b611523838383611567565b61152b61076a565b1561055b5760405162461bcd60e51b81526004016104a390611c99565b3b151590565b6001600160e01b031981166301ffc9a760e01b14919050565b61157283838361055b565b6001600160a01b03831661158e57611589816115f0565b6115b1565b816001600160a01b0316836001600160a01b0316146115b1576115b18382611634565b6001600160a01b0382166115cd576115c8816116d1565b61055b565b826001600160a01b0316826001600160a01b03161461055b5761055b82826117aa565b600980546000838152600a60205260408120829055600182018355919091527f6e1540171b6c0c960b71a7020d9f60077f6af931a8bbf590da0223dacf75c7af0155565b60006001611641846107a8565b61164b91906123f7565b60008381526008602052604090205490915080821461169e576001600160a01b03841660009081526007602090815260408083208584528252808320548484528184208190558352600890915290208190555b5060009182526008602090815260408084208490556001600160a01b039094168352600781528383209183525290812055565b6009546000906116e3906001906123f7565b6000838152600a60205260408120546009805493945090928490811061171957634e487b7160e01b600052603260045260246000fd5b90600052602060002001549050806009838154811061174857634e487b7160e01b600052603260045260246000fd5b6000918252602080832090910192909255828152600a9091526040808220849055858252812055600980548061178e57634e487b7160e01b600052603160045260246000fd5b6001900381819060005260206000200160009055905550505050565b60006117b5836107a8565b6001600160a01b039093166000908152600760209081526040808320868452825280832085905593825260089052919091209190915550565b8280546117fa90612451565b90600052602060002090601f01602090048101928261181c5760008555611862565b82601f1061183557805160ff1916838001178555611862565b82800160010185558215611862579182015b82811115611862578251825591602001919060010190611847565b5061186e929150611872565b5090565b5b8082111561186e5760008155600101611873565b600067ffffffffffffffff808411156118a2576118a26124a2565b604051601f8501601f1916810160200182811182821017156118c6576118c66124a2565b6040528481529150818385018610156118de57600080fd5b8484602083013760006020868301015250509392505050565b80356001600160a01b03811681146103e557600080fd5b60006020828403121561191f578081fd5b6113d1826118f7565b6000806040838503121561193a578081fd5b611943836118f7565b9150611951602084016118f7565b90509250929050565b60008060006060848603121561196e578081fd5b611977846118f7565b9250611985602085016118f7565b9150604084013590509250925092565b600080600080608085870312156119aa578081fd5b6119b3856118f7565b93506119c1602086016118f7565b925060408501359150606085013567ffffffffffffffff8111156119e3578182fd5b8501601f810187136119f3578182fd5b611a0287823560208401611887565b91505092959194509250565b60008060408385031215611a20578182fd5b611a29836118f7565b915060208301358015158114611a3d578182fd5b809150509250929050565b60008060408385031215611a5a578182fd5b611a63836118f7565b946020939093013593505050565b600080600060608486031215611a85578283fd5b611a8e846118f7565b925060208401359150604084013567ffffffffffffffff811115611ab0578182fd5b8401601f81018613611ac0578182fd5b611acf86823560208401611887565b9150509250925092565b600060208284031215611aea578081fd5b5035919050565b60008060408385031215611b03578182fd5b82359150611951602084016118f7565b600060208284031215611b24578081fd5b81356113d1816124b8565b600060208284031215611b40578081fd5b81516113d1816124b8565b60008151808452611b6381602086016020860161240e565b601f01601f19169290920160200192915050565b60007f416363657373436f6e74726f6c3a206163636f756e742000000000000000000082528351611baf81601785016020880161240e565b7001034b99036b4b9b9b4b733903937b6329607d1b6017918401918201528351611be081602884016020880161240e565b01602801949350505050565b6001600160a01b0391909116815260200190565b6001600160a01b0385811682528416602082015260408101839052608060608201819052600090611c3390830184611b4b565b9695505050505050565b901515815260200190565b90815260200190565b6000602082526113d16020830184611b4b565b6020808252818101527f537472696e67733a20686578206c656e67746820696e73756666696369656e74604082015260600190565b6020808252602b908201527f4552433732315061757361626c653a20746f6b656e207472616e73666572207760408201526a1a1a5b19481c185d5cd95960aa1b606082015260800190565b60208082526014908201527314185d5cd8589b194e881b9bdd081c185d5cd95960621b604082015260600190565b6020808252602b908201527f455243373231456e756d657261626c653a206f776e657220696e646578206f7560408201526a74206f6620626f756e647360a81b606082015260800190565b60208082526032908201527f4552433732313a207472616e7366657220746f206e6f6e20455243373231526560408201527131b2b4bb32b91034b6b83632b6b2b73a32b960711b606082015260800190565b6020808252601c908201527f4552433732313a20746f6b656e20616c7265616479206d696e74656400000000604082015260600190565b60208082526024908201527f4552433732313a207472616e7366657220746f20746865207a65726f206164646040820152637265737360e01b606082015260800190565b60208082526019908201527f4552433732313a20617070726f766520746f2063616c6c657200000000000000604082015260600190565b6020808252602c908201527f4552433732313a206f70657261746f7220717565727920666f72206e6f6e657860408201526b34b9ba32b73a103a37b5b2b760a11b606082015260800190565b60208082526038908201527f4552433732314d696e7465724275726e65725061757365723a206d757374206860408201527f6176652070617573657220726f6c6520746f2070617573650000000000000000606082015260800190565b6020808252603a908201527f4552433732314d696e7465724275726e65725061757365723a206d757374206860408201527f6176652070617573657220726f6c6520746f20756e7061757365000000000000606082015260800190565b60208082526010908201526f14185d5cd8589b194e881c185d5cd95960821b604082015260600190565b60208082526038908201527f4552433732313a20617070726f76652063616c6c6572206973206e6f74206f7760408201527f6e6572206e6f7220617070726f76656420666f7220616c6c0000000000000000606082015260800190565b6020808252602a908201527f4552433732313a2062616c616e636520717565727920666f7220746865207a65604082015269726f206164647265737360b01b606082015260800190565b60208082526029908201527f4552433732313a206f776e657220717565727920666f72206e6f6e657869737460408201526832b73a103a37b5b2b760b91b606082015260800190565b6020808252818101527f4552433732313a206d696e7420746f20746865207a65726f2061646472657373604082015260600190565b6020808252602c908201527f4552433732313a20617070726f76656420717565727920666f72206e6f6e657860408201526b34b9ba32b73a103a37b5b2b760a11b606082015260800190565b6020808252602c908201527f4552433732314d657461646174613a2055524920736574206f66206e6f6e657860408201526b34b9ba32b73a103a37b5b2b760a11b606082015260800190565b60208082526029908201527f4552433732313a207472616e73666572206f6620746f6b656e2074686174206960408201526839903737ba1037bbb760b91b606082015260800190565b6020808252602f908201527f4552433732314d657461646174613a2055524920717565727920666f72206e6f60408201526e3732bc34b9ba32b73a103a37b5b2b760891b606082015260800190565b60208082526021908201527f4552433732313a20617070726f76616c20746f2063757272656e74206f776e656040820152603960f91b606082015260800190565b60208082526031908201527f4552433732313a207472616e736665722063616c6c6572206973206e6f74206f6040820152701ddb995c881b9bdc88185c1c1c9bdd9959607a1b606082015260800190565b6020808252602c908201527f455243373231456e756d657261626c653a20676c6f62616c20696e646578206f60408201526b7574206f6620626f756e647360a01b606082015260800190565b60208082526030908201527f4552433732314275726e61626c653a2063616c6c6572206973206e6f74206f7760408201526f1b995c881b9bdc88185c1c1c9bdd995960821b606082015260800190565b6020808252602f908201527f416363657373436f6e74726f6c3a2063616e206f6e6c792072656e6f756e636560408201526e103937b632b9903337b91039b2b63360891b606082015260800190565b60208082526037908201527f4552433732314d696e7465724275726e65725061757365723a206d757374206860408201527f617665206d696e74657220726f6c6520746f206d696e74000000000000000000606082015260800190565b600082198211156123d3576123d361248c565b500190565b60008160001904831182151516156123f2576123f261248c565b500290565b6000828210156124095761240961248c565b500390565b60005b83811015612429578181015183820152602001612411565b838111156109805750506000910152565b6000816124495761244961248c565b506000190190565b60028104600182168061246557607f821691505b6020821081141561248657634e487b7160e01b600052602260045260246000fd5b50919050565b634e487b7160e01b600052601160045260246000fd5b634e487b7160e01b600052604160045260246000fd5b6001600160e01b03198116811461070c57600080fdfea2646970667358221220df55aa956e0e6b61da02c8a85eea4497590c5195c6670d4d95b7ca11795683fa64736f6c6343000800003368747470733a2f2f6f70656e7365612d6372656174757265732d6170692e6865726f6b756170702e636f6d2f6170692f63726561747572652f000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000000000000000000000000000a000000000000000000000000000000000000000000000000000000000000000e000000000000000000000000000000000000000000000000000000000000000077374616b696e670000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000077374616b696e67000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010687474703a2f2f62616964752e636f6d00000000000000000000000000000000";
            System.out.println(str.length() / 1024);
            InputStream targetStream = IOUtils.toInputStream(str, StandardCharsets.UTF_8.name());

            connection = JDBCUtils.getConnection();
            String sql = "insert into tt(content) values(compress(?))";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setBlob(1, targetStream);
            int rows = preparedStatement.executeUpdate();
            System.out.println(rows);
        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            JDBCUtils.close(preparedStatement,connection);
        }


        try {
            connection = JDBCUtils.getConnection();
            String sql = "select uncompress(content) from tt where id=?";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setObject(1, 1);
            ResultSet  rs = preparedStatement.executeQuery();
            while (rs.next()) {
                Blob b = rs.getBlob(1);
                String content = IOUtils.toString(b.getBinaryStream(), StandardCharsets.UTF_8.name());
                System.out.println(content);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            // 6. 释放资源
            JDBCUtils.close(preparedStatement,connection);
        }
    }

}