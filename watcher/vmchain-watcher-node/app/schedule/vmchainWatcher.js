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
    
    await baseBlock(ctx,api,maxBlockNumber);
    await baseBlock(ctx,api,maxBlockNumber+1);

}

async function baseBlock(ctx,api,maxBlockNumber){
    console.log('maxBlockNumber')
    console.log(maxBlockNumber)
    next_schedule = false;
    let newBlock =  await api.query.cposContribution.cPoSBlocks(maxBlockNumber);

    if(!newBlock.toString()){
        console.log("no mobi data")
        next_schedule = true;
    }else{

        try {
            let newBlockJson = blockFromData(newBlock.toString());
            let blockTime = newBlockJson.create_time.toString();


            if(blockTime.length == 10){
                blockTime = blockTime +"000"
            }
            let newTime = new Date(parseInt(blockTime));
            if (blockTime == 0){
                newTime = new Date();
            }

            newBlockJson.create_time = newTime;

            await ctx.service.blocks.addBlock(newBlockJson,ctx);
            for(let tx in newBlockJson.transactions){
                await ctx.service.transactions.addTransactionsDetail(newBlockJson,newBlockJson.transactions[tx]);
            }
            next_schedule = true;

        } catch (error) {
            console.log('error')
            console.log(error.message);
            // let currentMaxBlockNumber = await ctx.service.blocks.getMaxBlockNumber();
            // if(!currentMaxBlockNumber){
            //     next_schedule = true;
            //     return
            // }
            next_schedule = true;
            // setTimeout(()=>{
            //     baseBlock(ctx,api,maxBlockNumber)
            // },5000);

        }
        
    }

}


function blockFromData(data){
    data = data.substring(2,data.length);

    let version = parseInt(data.substring(0,2),16);
    let block_height =parseInt(data.substring(2,18),16); //16
    let difficulty =parseInt(data.substring(18,50),16);  //32
    let blocked_fee =parseInt(data.substring(50,82),16); // 32
    let create_time =parseInt(data.substring(82,98),16); //16
    let block_hash = data.substring(98,162);  //
    let transaction_count = parseInt(data.substring(162,178));  //

    let txs_data = data.substring(178,data.length);
    console.log(txs_data.length)


    if (txs_data.length % 177 != 0){
        console.log("Error : Transaction list data is not public data "+txs_data);
    }

    let tx_list = [];
    for(var i = 0; i < transaction_count; i++){

        let tx_data = txs_data.substring(i*354,i*354+354);
        let tx = transactionFromData(tx_data);
        tx_list.push(tx);
    }


    let block = {
        version : version,
        block_height : block_height,
        difficulty : difficulty,
        blocked_fee : blocked_fee,
        create_time : create_time,
        block_hash : "0x"+block_hash,
        transaction_count:transaction_count,
        transactions:tx_list
    };
    return block;
}


function transactionFromData(data){


    let buy_symbol =hexToStr(data.substring(2,18)); //16
    let sell_symbol =hexToStr(data.substring(18,34)); //16
    let buyer_address = data.substring(34,74);  //
    let seller_address = data.substring(74,114);  //


    let amount =parseInt(data.substring(114,146),16);  //32
    let price =parseInt(data.substring(146,178),16); // 32
    let buyer_fee =parseInt(data.substring(178,210),16); //16
    let seller_fee =parseInt(data.substring(210,242),16); //16
    let transaction_hash = data.substring(242,306);  //
    let transaction_time = parseInt(data.substring(306,322),16);  //
    let transaction_volume = parseInt(data.substring(322,354),16);  //
     console.log("transaction_time"+ transaction_time)
    let tx = {
        buy_symbol:buy_symbol,
        sell_symbol:sell_symbol,
        buyer_address:buyer_address,
        seller_address:seller_address,
        amount:amount,
        price:price,
        buyer_fee:buyer_fee,
        seller_fee:seller_fee,
        transaction_hash:"0x"+transaction_hash,
        transaction_time:transaction_time,
        transaction_volume:transaction_volume,
    }

  //  console.log(tx);
    return tx;
}


function hexToStr(hex, encoding) {
    let trimedStr = hex.trim();
    let rawStr =
      trimedStr.substr(0, 2).toLowerCase() === "0x"
        ? trimedStr.substr(2)
        : trimedStr;
    let len = rawStr.length;
    if (len % 2 !== 0) {
      console.log("Illegal Format ASCII Code!");
      return "";
    }
    let curCharCode;
    let resultStr = [];

    let flag = false;
    for (let i = 0; i < len; i = i + 2) {
      curCharCode = parseInt(rawStr.substr(i, 2), 16);
        if(!flag){
            if(curCharCode != 0){
            flag = true;
            }
        }
      if(flag){
            resultStr.push(curCharCode);

      }
    }
    // encoding为空时默认为utf-8
    let bytesView = new Uint8Array(resultStr);
    let str = new TextDecoder(encoding).decode(bytesView);
    return str;
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
