// const ethers = require("ethers");
// const zksync = require("./../../app/zksync/build");
// const bridgerCall = require("./../../app/bridge/bridge-utils");
// const GlobalConstants = require("./../../app/bridge/constants");
const { ApiPromise, WsProvider, HttpProvider } = require("@polkadot/api");
const fs = require("fs");
// const { blake2AsHex } = require('@polkadot/util-crypto');
// const { hexToU8a, isHex, stringToU8a,hexToBn,numberToHex } =  require('@polkadot/util');


const DTX_WEB3J_URL = process.env.DTX_WEB3J_URL;
//const DTX_WEB3J_URL = 'http://vmdev.infra.powx.io';
//const DTX_WEB3J_URL = 'http://vmchain-dev-node-0-sandbox.chain-sandbox.svc.cluster.local:9934';
const wsProvider = new HttpProvider(DTX_WEB3J_URL);
let context = fs.readFileSync('./config/types.json');
const typesData = JSON.parse(context);
let api = null;




module.exports = {
    schedule: {
        interval: '5s', // 1 分钟间隔
        type: 'worker', // 指定所有的 worker 都需要执行
    },
    async task(ctx) {
        vmchainWatcher(ctx)
    },


};


let next_schedule = true;

async function scanBlock(ctx,api){
    console.log('next_schedule')
    console.log(next_schedule)
    if(!next_schedule){
        return
    }
    let maxBlockNumber = await ctx.service.blocks.getMaxBlockNumber();
    if(!maxBlockNumber){
        maxBlockNumber=1;
    }
    
    console.log('maxBlockNumber')
    console.log(maxBlockNumber)
    await baseBlock(ctx,api,maxBlockNumber);
    await baseBlock(ctx,api,maxBlockNumber+1);

}

async function baseBlock(ctx,api,maxBlockNumber){
    next_schedule = false;
    console.log(maxBlockNumber)
    let newBlock =  await api.query.cposContribution.contributionBlocks(maxBlockNumber);

    if(!newBlock.toString()){
        console.log("no mobi data")
        next_schedule = true;
    }else{
        try {
            let newBlockJson = JSON.parse(newBlock.toString());
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
            
        } catch (error) {
            console.log('error')
            console.log(error.message);
            let currentMaxBlockNumber = await ctx.service.blocks.getMaxBlockNumber();
            if(!currentMaxBlockNumber){
                next_schedule = true;
                return
            }
            setTimeout(()=>{
                baseBlock(ctx,api,maxBlockNumber)
            },5000);
            
        }
        
    }

}



async function vmchainWatcher(ctx) {

    // Create our API with a default connection to the local node
    if(!api){
        api = await ApiPromise.create({
            provider: wsProvider,
            types: typesData,
        });
    }
    

// numberToHex(0x1234, 32); // => 0x00001234
    await scanBlock(ctx,api);

}
