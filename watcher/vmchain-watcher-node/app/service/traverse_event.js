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


async function main () {
    // Create our API with a default connection to the local node
    const api = await ApiPromise.create({
        provider: wsProvider,
        types:typesData
    });
    // Subscribe to system events via storage
    api.query.system.events((events) => {
        console.log(`\nReceived ${events.length} events: ` );
        // Loop through the Vec<EventRecord>

        events.forEach((record) => {
            // Extract the phase, event and the event types
            let event = record.event;
            const types = event.typeDef;
            console.log(event.data);

            // http://xapi.powx.io/l2-server-jsrpc/jsrpc      rinkeby
            // http://xapi.powx.io/l2-server3-jsrpc/jsrpc     vm
            let address_array = ["http://xapi.powx.io/l2-server-jsrpc/jsrpc", "http://xapi.powx.io/l2-server3-jsrpc/jsrpc"]
            address_array.forEach(async (address) => {
                let web3Wallet = new web3("http://vmtest.infra.powx.io/v1/72f3a83ea86b41b191264bd16cbac2bf");
                const ethWallet = new ethers.providers.Web3Provider(
                    web3Wallet.eth.currentProvider
                ).getSigner();
                const syncProvider = await zksync.Provider.newHttpProvider(
                    "http://xapi.powx.io/l2-server3-jsrpc/jsrpc", 1000
                );
                let signer = await zksync.Signer.fromPrivateKey("0f9390c5b10cb10befbedf8cf451bf16e4c1e70c80ec12051f5c65454bdb3707");
                let signType = { verificationMethod: 'ECDSA', isSignedMsgPrefixed: false};
                const syncWallet = await zksync.Wallet.fromEthSigner(ethWallet, syncProvider, signer, 1, signType);
                const withdrawTransaction = await syncWallet.withdrawFromSyncToEthereum({
                    ethAddress: "",
                    token: "USDT",
                    amount: ethers.utils.parseEther("1.0"),
                    fee: ethers.utils.parseEther("0.001")
                });
                const transactionReceipt = await withdrawTransaction.awaitVerifyReceipt();
                console.log(transactionReceipt)
            })
        });
    });
}

const bridge=async function(){
    let wallet = new web3("http://rinkeby.infra.powx.io/v1/72f3a83ea86b41b191264bd16cbac2bf");
    let ethPrivateKey = "d2302498102cb336f9291c9913031b11008a49d025b2e7b03f237371372a28e3";
    let amount = "1000000";
    let token = "USDT";
    let decimals = 6;
    let provider = new ethers.providers.Web3Provider(
        wallet.eth.currentProvider
    );

    const ethWallet = new ethers.Wallet(ethPrivateKey,provider);

    let contract= await bridgerCall.Call.createContractInstance(GlobalConstants.GlobalConstants.Contracts[token].from,GlobalConstants.GlobalConstants.ContractABIs.Erc20,wallet);
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


