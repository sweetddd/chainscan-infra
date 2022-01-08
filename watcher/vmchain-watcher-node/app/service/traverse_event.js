'use strict';

let fs = require("fs");
let http = require('@polkadot/api');
let ethers = require('ethers');
let zksync = require('zksync/build');
let web3 = require('web3');

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
        console.log(events)
        console.log("=====================================================")
        // Loop through the Vec<EventRecord>

        events.forEach((record) => {
            // Extract the phase, event and the event types
            let event = record.event;
            const types = event.typeDef;
            console.log("------------------------------------------------------")
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
            // Loop through each of the parameters, displaying the type and data
            event.data.forEach(async (data, index) => {
                console.log(`\t\t\t${types[index].type}: ${data.toString()}`);
            });
        });
    });
}

main().catch((error) => {
    console.error(error);
    process.exit(-1);
});


