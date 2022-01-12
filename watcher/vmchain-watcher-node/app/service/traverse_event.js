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
// let context = fs.readFileSync("E:\\project\\IdeaProject\\chainscan-infra\\watcher\\vmchain-watcher-node\\app\\service\\types.json");
let context = fs.readFileSync("../../config/types.json");
let typesData= JSON.parse(context);

const L2Address = {
    "rinkeby": "http://xapi.powx.io/l2-server-jsrpc/jsrpc",
    "vm": "http://xapi.powx.io/l2-server3-jsrpc/jsrpc"
}
let token = "USDT";
const type =  'Withdraw';
//手续费账号
let ethPrivateKey = "0xa53578fe8f9a1678be99f58dbe3e189743f5cb2149ba77d004c6819d0dd25104";


async function main () {
    // Create our API with a default connection to the local node
    const api = await ApiPromise.create({
        provider: wsProvider,
        types:typesData
    });
    // Subscribe to system events via storage
    await api.query.system.events(async (events) => {
        // Loop through the Vec<EventRecord>
        await events.forEach(async (record) => {
            // Extract the event
            let event = record.event;
            if (event.section.toString() === "cposContribution" && event.method.toString() === "Dividend") {
                const block_height = event.data[0].toString()
                const reverse = parseInt(event.data[1].toString())
                const timestamp = event.data[2].toString()
                console.log(`Received dividend events, block height: ${block_height}, reverse: ${reverse}, timestamp: ${timestamp}`)
                // await rinkebyWithdraw("rinkeby", reverse)
                // await vmWithdraw("vm", reverse)
                Object.keys(L2Address).map(async name => {
                    if (name.trim() === "rinkeby"){
                        await rinkebyWithdraw(name, reverse)
                    }
                    else {
                        await vmWithdraw(name, reverse)
                    }
                })
            }
        });
    });
}

const rinkebyWithdraw = async function(name, reverse){
    try {
        // let ethPrivateKey = "0xa53578fe8f9a1678be99f58dbe3e189743f5cb2149ba77d004c6819d0dd25104";
        let web3Wallet = await new web3("http://rinkeby.infra.powx.io/v1/72f3a83ea86b41b191264bd16cbac2bf");
        let provider = await new ethers.providers.Web3Provider(web3Wallet.eth.currentProvider);
        const syncProvider = await zksync.Provider.newHttpProvider(L2Address[name], 1000);
        const ethWallet = await new ethers.Wallet(ethPrivateKey,provider);
        const zkWallet =  await zksync.Wallet.fromEthSigner(ethWallet, syncProvider);
        // 查看 zkSync 账户余额
        const state = await zkWallet.getAccountState();
        console.log(state)
        const committedBalances = state.committed.balances;
        const balance = parseInt(committedBalances["USDT"]);
        const volume = reverse < balance ? reverse : balance;
        console.log(`L2 name: ${name}, reverse：${reverse}, USDT balance: ${balance}, withdraw to L1 volume: ${volume}`)
        const fee = await zkWallet.provider.getTransactionFee(type, zkWallet.address(), token);
        const withdrawTransaction = await zkWallet.withdrawFromSyncToEthereum({
            ethAddress: zkWallet.address(),
            token: token,
            amount: volume,
            fee: fee.totalFee
        });
        await withdrawTransaction.awaitVerifyReceipt();
        await bridge(volume)
    } catch (err) {
        console.log(`Exception: -> ${err.message}`)
    }
}

const vmWithdraw = async function(name, reverse){
    try {
        // let ethPrivateKey = "0xa53578fe8f9a1678be99f58dbe3e189743f5cb2149ba77d004c6819d0dd25104";
        let web3Wallet = new web3("http://vmtest.infra.powx.io/v1/72f3a83ea86b41b191264bd16cbac2bf");
        let provider = new ethers.providers.Web3Provider(web3Wallet.eth.currentProvider);
        const syncProvider = await zksync.Provider.newHttpProvider(L2Address[name], 1000);
        const ethWallet = new ethers.Wallet(ethPrivateKey,provider);
        const zkWallet =  await zksync.Wallet.fromEthSigner(ethWallet, syncProvider);
        // 查看 zkSync 账户余额
        const state = await zkWallet.getAccountState();
        console.log(state)
        const committedBalances = state.committed.balances;
        const balance = committedBalances["USDT"];
        const volume = reverse < balance ? reverse : balance;
        console.log(`L2 name: ${name}, reverse：${reverse}, USDT balance: ${balance}, withdraw to L1 volume: ${volume}`)

        const fee = await zkWallet.provider.getTransactionFee(type, zkWallet.address(), token);
        const withdrawTransaction = await zkWallet.withdrawFromSyncToEthereum({
            ethAddress:zkWallet.address(),
            token: token,
            amount: volume,
            fee:fee.totalFee
        });
        await withdrawTransaction.awaitVerifyReceipt();
    } catch (err) {
        console.log(`Exception: -> ${err.message}`)
    }
}

const bridge = async function(amount){
    // 一层rinkeby
    let decimals = 6;
    let wallet = new web3("http://rinkeby.infra.powx.io/v1/72f3a83ea86b41b191264bd16cbac2bf");
    let provider = new ethers.providers.Web3Provider(
        wallet.eth.currentProvider
    );

    const ethWallet = new ethers.Wallet(ethPrivateKey, provider);
    let tx= await bridgerCall.Call.erc20Aprrove(GlobalConstants.GlobalConstants.Contracts[token].from,GlobalConstants.GlobalConstants.ETH_ERC20_HANDLER_ADDRESS,'150000','6',ethWallet);
    console.log(`${tx} 完成`);
    let txs =await bridgerCall.Call.erc20_despoit(
        GlobalConstants.GlobalConstants.ETH_BRIDGE_ADDRESS,
        token,
        amount,
        GlobalConstants.GlobalConstants.VMCHAIN_ID,
        GlobalConstants.GlobalConstants.Contracts[token].resourceId,
        bridgerCall.Call.getCallData(amount, decimals, ethWallet.address),
        ethWallet,
        decimals)
    console.log(`${txs} 完成`);
}

main().catch((error) => {
    console.error(error);
    process.exit(-1);
});


