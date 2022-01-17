const ethers = require("ethers");
const zksync = require("./../../app/zksync/build");
const bridgerCall = require("./../../app/bridge/bridge-utils");
const GlobalConstants = require("./../../app/bridge/constants");
const { ApiPromise, WsProvider, HttpProvider } = require("@polkadot/api");
const fs = require("fs");
const { blake2AsHex } = require('@polkadot/util-crypto');
const { hexToU8a, isHex, stringToU8a,hexToBn,numberToHex } =  require('@polkadot/util');


const vmWeb3Url = process.env.vmWeb3Url;
const wsProvider = new HttpProvider(vmWeb3Url);
let context = fs.readFileSync('./config/types.json');

const typesData = JSON.parse(context);




module.exports = {
    schedule: {
        interval: '5s', // 1 分钟间隔
        type: 'all', // 指定所有的 worker 都需要执行
    },
    async task(ctx) {
        console.log(vmchainWatcher(ctx))
    },


};




async function scanBlock(ctx,api){
    let maxBlockNumber = await ctx.service.blocks.getMaxBlockNumber();
    if(!maxBlockNumber){
        maxBlockNumber = 1;
    }
    await baseBlock(ctx,api,maxBlockNumber);
    await baseBlock(ctx,api,maxBlockNumber+1);

}

async function baseBlock(ctx,api,maxBlockNumber){


    let newBlock =  await api.query.cposContribution.contributionBlocks(maxBlockNumber);
    console.log(newBlock.toString())
    if(!newBlock.toString()){
        console.log("no mobi data")
    }else{
        let newBlockJson = JSON.parse(newBlock.toString());
        let blockTime = newBlockJson.start_time.toString();
        console.log(blockTime)

        if(blockTime.length == 10){
            blockTime = blockTime +"000"
        }
        let newTime = new Date(blockTime);
        newBlockJson.start_time = newTime;
        await ctx.service.blocks.addBlock(newBlockJson);
        for(let tx in newBlockJson.transactions){
            await ctx.service.transactions.addTransactionsDetail(newBlockJson,newBlockJson.transactions[tx]);
        }
    }

}



async function vmchainWatcher(ctx) {

    // Create our API with a default connection to the local node
    const api = await ApiPromise.create({
        provider: wsProvider,
        types: typesData,
    });
// numberToHex(0x1234, 32); // => 0x00001234
    await scanBlock(ctx,api);

}
