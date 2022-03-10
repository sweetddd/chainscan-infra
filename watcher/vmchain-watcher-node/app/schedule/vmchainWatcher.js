const ethers = require("ethers");
const zksync = require("./../../app/zksync/build");
const bridgerCall = require("./../../app/bridge/bridge-utils");
const GlobalConstants = require("./../../app/bridge/constants");
const { ApiPromise, WsProvider, HttpProvider } = require("@polkadot/api");
const fs = require("fs");
const { blake2AsHex } = require('@polkadot/util-crypto');
const { hexToU8a, isHex, stringToU8a,hexToBn,numberToHex } =  require('@polkadot/util');


//const vmWeb3Url = process.env.vmWeb3Url;
const vmWeb3Url = 'http://vmdev.infra.powx.io';
const wsProvider = new HttpProvider(vmWeb3Url);
let context = fs.readFileSync('./config/types.json');

const typesData = JSON.parse(context);

let next_schedule = true;


module.exports = {
    schedule: {
        interval: '5s', // 1 分钟间隔
        type: 'worker', // 指定所有的 worker 都需要执行
        immediate: true,//是否立即执行一次
    },
    async task(ctx) {
        vmchainWatcher(ctx)
    },


};




async function scanBlock(ctx,api){
    if(!next_schedule){
        return;
    }
    let maxBlockNumber = await ctx.service.blocks.getMaxBlockNumber();
    if(maxBlockNumber){
        await baseBlock(ctx,api,maxBlockNumber+1);
        return
    }
    await baseBlock(ctx,api,1);

}

async function baseBlock(ctx,api,maxBlockNumber){

    let newBlock;
    try {
        newBlock =  await api.query.cposContribution.contributionBlocks(maxBlockNumber);
    } catch (error) {
        baseBlock(ctx,api,maxBlockNumber)
        return
    }

    console.log('baseBlockbaseBlockbaseBlockbaseBlock')
    console.log('baseBlockbaseBlockbaseBlockbaseBlock')
    console.log('baseBlockbaseBlockbaseBlockbaseBlock')
    console.log('baseBlockbaseBlockbaseBlockbaseBlock')
    console.log('baseBlockbaseBlockbaseBlockbaseBlock')
    console.log(maxBlockNumber)

    let str;
    if(newBlock&&newBlock.toString){
        str = newBlock.toString()
    }

    if(!str){
        next_schedule = false;
        baseBlock(ctx,api,maxBlockNumber+1);
        return
    }else{
        let newBlockJson = JSON.parse(str);
        let blockTime = newBlockJson.start_time.toString();


        if(blockTime.length == 10){
            blockTime = blockTime +"000"
        }
        let newTime = new Date(parseInt(blockTime));
        if (blockTime == 0){
            newTime = new Date();
        }

        newBlockJson.start_time = newTime;

        await ctx.service.blocks.addBlock(newBlockJson,ctx);
        for(let tx in newBlockJson.transactions){
            await ctx.service.transactions.addTransactionsDetail(newBlockJson,newBlockJson.transactions[tx]);
        }
        next_schedule = true;

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
