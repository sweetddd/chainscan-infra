// const ethers = require("ethers");
// const zksync = require("./../../app/zksync/build");
// const bridgerCall = require("./../../app/bridge/bridge-utils");
// const GlobalConstants = require("./../../app/bridge/constants");
const { ApiPromise, WsProvider, HttpProvider } = require("@polkadot/api");
const fs = require("fs");
const web3 = require("web3");
const Axios = require('axios');

// const { blake2AsHex } = require('@polkadot/util-crypto');
// const { hexToU8a, isHex, stringToU8a,hexToBn,numberToHex } =  require('@polkadot/util');
const BigNumber = require( 'bignumber.js');
const crypto=require('crypto');
let ethChainId = process.env.eth_chain_id;
let dtxChainId = process.env.dtx_chain_id;

const apiUrlList = new Map();
apiUrlList.set(ethChainId,process.env.eth_l2_api_url);
apiUrlList.set(dtxChainId,process.env.dtx_l2_api_url);

const DTX_WEB3J_URL = process.env.DTX_WEB3J_URL;
//const DTX_WEB3J_URL = 'http://vmdev.infra.powx.io';
//const DTX_WEB3J_URL = 'http://vmchain-dev-node-0-sandbox.chain-sandbox.svc.cluster.local:9934';
const wsProvider = new HttpProvider(DTX_WEB3J_URL);
let context = fs.readFileSync('./config/types.json');
const typesData = JSON.parse(context);
let api = null;
let ethTokens = null;
let dtxTokens = null;


let max_transaction = 1000;

module.exports = {
    schedule: {
        interval: '10s', // 1 分钟间隔
        type: 'worker', // 指定所有的 worker 都需要执行
    },
    async task(ctx) {
        vmchainWatcher(ctx)
    }

};


let next_schedule = true;

async function scanBlock(ctx,api){

    let maxBlockNumber = await ctx.service.blocks.getMaxBlockNumber();
    if(!maxBlockNumber){
        maxBlockNumber=0;
    }
    maxBlockNumber++;

    await baseBlock(ctx,api,maxBlockNumber);
    // if(next_schedule){
    //     await baseBlock(ctx,api,maxBlockNumber+1);
    // }


}

async function baseBlock(ctx,api,maxBlockNumber){
    console.log(maxBlockNumber)
    next_schedule = false;
    let newBlock =  await api.query.cposContribution.cPoSBlocks(maxBlockNumber);

    if(!newBlock.toString()){
        console.log("no mobi data")
        next_schedule = true;
    }else{

        try {
            let newBlockJson = await blockFromData(api,newBlock.toString());
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

            console.log(error.message)

            const IS_DUP_ENTRY = error.message.indexOf('ER_DUP_ENTRY')!==-1;
            if(IS_DUP_ENTRY){
                next_schedule = true;
                return
            }

            next_schedule = false;
            setTimeout(()=>{
                baseBlock(ctx,api,maxBlockNumber)
            },10000);

        }
        
    }

}


async function blockFromData(api,data){
    data = data.substring(2,data.length);

    let version = parseInt(data.substring(0,2),16);
    let block_height =parseInt(data.substring(2,18),16); //16
    let difficulty =parseInt(data.substring(18,50),16);  //32
    let blocked_fee =parseInt(data.substring(50,82),16); // 32
    let create_time =parseInt(data.substring(82,98),16); //16
    let block_hash = data.substring(98,162);  //
    let rewards = parseInt(data.substring(162,194),16); //16
    let transaction_count = parseInt(Number(web3.utils.hexToNumberString('0x'+data.substring(194,210))));  //

    let tx_index = transaction_count/max_transaction;

    let tx_list = await  getTransactions(api,block_height,tx_index,create_time);



    let block = {
        version : version,
        block_height : block_height,
        difficulty : difficulty,
        blocked_fee : blocked_fee,
        create_time : create_time,
        block_hash : "0x"+block_hash,
        rewards : rewards,
        transaction_count:transaction_count,
        transactions:tx_list
    };

    return block;
}


async function getTransactions(api,blockNumber,transactionIndex,create_time){

    let tx_list = [];
    for(var j = 0;j < transactionIndex;j++){

        let txs_data = await api.query.cposContribution.blockTransactions(blockNumber,j);

        txs_data =  txs_data.toString().substring(2);
        if (txs_data.length % 74 != 0){
            console.log("Error : Transaction list data is not public data "+txs_data);
        }


        let transaction_count = txs_data.length/148;

        for(var i = 0; i <= transaction_count; i++){

            let tx_data = txs_data.substring(i*148,i*148+148);
            if(tx_data.length > 0){
                let tx = transactionFromData(tx_data,blockNumber,i,create_time);
                tx_list.push(tx);
            }
        }

    }

    return tx_list;


}


function transactionFromData(data,block_height,index,create_time){


    let chain_id = parseInt(data.substring(2,4),16);  //


    let token_0 =parseInt(data.substring(4,12),16); //16
    let token_1 =parseInt(data.substring(12,20),16); //16


    let buyer_address = data.substring(20,60);  //
    let seller_address = data.substring(60,100);  //


    let fee_0_number =unpack(parseInt(data.substring(100,110),16)); //16
    let fee_1_number =unpack(parseInt(data.substring(110,120),16)); //16
    let fee_token =parseInt(data.substring(120,128),16); //16
    let amount_0 = unpack(parseInt(data.substring(128,138),16));  //
    let amount_1 = unpack(parseInt(data.substring(138,148),16));  //
    var obj=crypto.createHash('sha256');
    obj.update(new Date()+""+block_height +""+index);
    var str=obj.digest('hex');//hex是十六进制

    console.log("token_0 = "+token_0+",token_1 = "+token_1+"");

    let token_0_symbol = "";
    let token_1_symbol = "";
    let fee_token_symbol = "";
    if(chain_id == dtxChainId){
         token_0_symbol = dtxTokens.get(token_0).symbol;
         token_1_symbol = dtxTokens.get(token_1).symbol;
        fee_token_symbol = dtxTokens.get(fee_token).symbol;
    }else{
        token_0_symbol = ethTokens.get(token_0).symbol;
         token_1_symbol = ethTokens.get(token_1).symbol;
        fee_token_symbol = ethTokens.get(fee_token).symbol;
    }

    console.log("token_0_symbol = "+token_0_symbol+",token_1_symbol = "+token_1_symbol+"");

    let tx = {
        token_0:token_0_symbol,
        token_1:token_1_symbol,
        buyer_address:"0x"+buyer_address,
        seller_address:"0x"+seller_address,
        amount_0:amount_0,
        amount_1:amount_1,
        fee_0:fee_0_number,
        fee_1:fee_1_number,
        chain_id:chain_id,
        fee_token : fee_token_symbol,
        transaction_time:create_time,
        transaction_hash : "0x"+str
    }

    return tx;
}



function  unpack(number){
    let binary =  number.toString(2);
    let binary_data = "";
    if(binary.length < 40 ){
        let length = 40 - binary.length;
        for(var i = 0; i < length; i++){
            binary_data += "0";
        }
    }
    binary_data += binary;

    let mantissa_binary = binary_data.substring(0,35);
    let exponent_pow_binary = binary_data.substring(35,40);

    let mantissa = parseInt(mantissa_binary,2);
    let exponent_pow = parseInt(exponent_pow_binary,2);

    let res = mantissa * Math.pow(10, exponent_pow)
    const bigNumber = new BigNumber(res);
    const numberStr = bigNumber.toString(10);//转成10进制字符串

    return numberStr;
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

    if(!dtxTokens){
        //初始化tokens
        console.log("初始化");
        ethTokens = await initTokens(process.env.eth_l2_api_url);
        dtxTokens = await initTokens(process.env.dtx_l2_api_url);
        if(ethTokens.size == 0 || dtxTokens.size == 0){
            return;
        }

    }
    

// numberToHex(0x1234, 32); // => 0x00001234
    await scanBlock(ctx,api);

}

async function initTokens(url){
    let map = new Map();
    await Axios({
        method: 'get',
        url: url+'/v0.2/tokens',
        params: { //相当于url里的querystring
            from: 'latest',
            limit: '100',
            direction:'older'
        }
    }).then(function (response) {
        let list = response.data.result.list;


        for(let index =0; index < list.length; index++ ){
            let token = list[index];
            map.set(token.id,token);
        }

    })
        .catch(function (error) {
            console.log(error);
            return map;
        });;
    return map;
}

