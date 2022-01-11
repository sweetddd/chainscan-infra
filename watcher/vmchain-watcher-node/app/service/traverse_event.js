'use strict';

let fs = require("fs");
let http = require('@polkadot/api');
let web3 = require('web3');
let ethers = require('ethers');
let zksync = require('../zksync/build');
let bridgerCall = require('./../bridge/bridge-utils');
let GlobalConstants = require('./../bridge/constants');

let WsProvider = http.WsProvider
let ApiPromise = http.ApiPromise

const wsProvider = new WsProvider("ws://10.233.65.230:9900");
let context = fs.readFileSync("E:\\project\\IdeaProject\\chainscan-infra\\watcher\\vmchain-watcher-node\\app\\service\\types.json");
// let context = fs.readFileSync("../../config/types.json");
let typesData= JSON.parse(context);

// http://xapi.powx.io/l2-server-jsrpc/jsrpc      rinkeby
// http://xapi.powx.io/l2-server3-jsrpc/jsrpc     vm
const L2Address = {
    "rinkeby": "http://xapi.powx.io/l2-server-jsrpc/jsrpc",
    "vm": "http://xapi.powx.io/l2-server3-jsrpc/jsrpc"
}


async function main () {
    // Create our API with a default connection to the local node
    const api = await ApiPromise.create({
        provider: wsProvider,
        types:typesData
    });
    // Subscribe to system events via storage
    api.query.system.events((events) => {
        // Loop through the Vec<EventRecord>
        events.forEach(async (record) => {
            // Extract the event
            let event = record.event;
            if (event.section.toString() === "cposContribution" && event.method.toString() === "Dividend") {
                const block_height = event.data[0].toString()
                const reverse = parseInt(event.data[1].toString())
                const timestamp = event.data[2].toString()
                console.log(`Received dividend events, block height: ${block_height}, reverse: ${reverse}, timestamp: ${timestamp}`)

                let token = "USDT";
                const type =  'Withdraw';

                Object.keys(L2Address).map(async name => {
                    if (name.trim() === "rinkeby"){
                        // let amount = "50000000";
                        let ethPrivateKey = "9d054bd9d4f13c37fea4daa1f2f96968ce272e5a8b455b0d516c09acbe2f2803";
                        let web3Wallet = new web3("http://rinkeby.infra.powx.io/v1/72f3a83ea86b41b191264bd16cbac2bf");
                        let provider = new ethers.providers.Web3Provider(
                            web3Wallet.eth.currentProvider
                        );
                        const syncProvider = await zksync.Provider.newHttpProvider(
                            L2Address[name], 1000
                        );
                        const ethWallet = new ethers.Wallet(ethPrivateKey,provider);
                        const zkWallet =  await zksync.Wallet.fromEthSigner(ethWallet,syncProvider);

                        // 查看 zkSync 账户余额
                        let nonce = await zkWallet.getNonce()
                        const state = await zkWallet.getAccountState();
                        const committedBalances = state.committed.balances;
                        const balance = committedBalances["USDT"];
                        console.log(`current L2 name: ${name}, USDT balance: ${balance}`)

                        const fee = await zkWallet.provider.getTransactionFee(type, zkWallet.address(), token);
                        const withdrawTransaction = await zkWallet.withdrawFromSyncToEthereum({
                            ethAddress:zkWallet.address(),
                            token: token,
                            amount: balance,
                            fee:fee.totalFee,
                            nonce:nonce
                        });
                        const transactionReceipt = await withdrawTransaction.awaitVerifyReceipt();
                        await bridge(ethPrivateKey, balance)
                    }
                    else {
                        // let amount = "500";
                        let ethPrivateKey = "0f9390c5b10cb10befbedf8cf451bf16e4c1e70c80ec12051f5c65454bdb3707";
                        let web3Wallet = new web3("http://vmtest.infra.powx.io/v1/72f3a83ea86b41b191264bd16cbac2bf");
                        let provider = new ethers.providers.Web3Provider(
                            web3Wallet.eth.currentProvider
                        );
                        const syncProvider = await zksync.Provider.newHttpProvider(
                            L2Address[name], 1000
                        );
                        const ethWallet = new ethers.Wallet(ethPrivateKey,provider);
                        const zkWallet =  await zksync.Wallet.fromEthSigner(ethWallet,syncProvider);

                        // 查看 zkSync 账户余额
                        const state = await zkWallet.getAccountState();
                        const committedBalances = state.committed.balances;
                        const balance = committedBalances["USDT"];
                        console.log(`current L2 name: ${name}, USDT balance: ${balance}`)

                        const fee = await zkWallet.provider.getTransactionFee(type, zkWallet.address(), token);
                        const withdrawTransaction = await zkWallet.withdrawFromSyncToEthereum({
                            ethAddress:zkWallet.address(),
                            token: token,
                            amount: balance,
                            fee:fee.totalFee
                        });
                        const transactionReceipt = await withdrawTransaction.awaitVerifyReceipt();
                    }
                })
            }
        });
    });
}

const bridge = async function(){
    // let ethPrivateKey = "9d054bd9d4f13c37fea4daa1f2f96968ce272e5a8b455b0d516c09acbe2f2803";
    // let amount = "1000000";
    let wallet = new web3("http://rinkeby.infra.powx.io/v1/72f3a83ea86b41b191264bd16cbac2bf");
    let token = "USDT";
    let decimals = 6;
    let provider = new ethers.providers.Web3Provider(
        wallet.eth.currentProvider
    );

    const ethWallet = new ethers.Wallet(ethPrivateKey,provider);
    let tx= await bridgerCall.Call.erc20Aprrove(GlobalConstants.GlobalConstants.Contracts[token].from,GlobalConstants.GlobalConstants.ETH_ERC20_HANDLER_ADDRESS,'150000','6',ethWallet);
    console.log(`${tx} 完成`);
    let txs =await bridgerCall.Call.erc20_despoit(
        GlobalConstants.GlobalConstants.ETH_BRIDGE_ADDRESS,
        token,
        amount,
        GlobalConstants.GlobalConstants.VMCHAIN_ID,
        GlobalConstants.GlobalConstants.Contracts[token].resourceId,
        bridgerCall.Call.getCallData(amount,decimals,ethWallet.address),
        ethWallet,
        decimals)
    console.log(`${txs} 完成`);

}

main().catch((error) => {
    console.error(error);
    process.exit(-1);
});


