'use strict';

const { Controller } = require('egg');
const { useSuccess } = require('../../utils/index.js');

var fs = require("fs");
var context = fs.readFileSync("./config/types.json");
var typesData= JSON.parse(context);

// substrate
var http = require('@polkadot/api');
var Keyring = http.Keyring
const keyring = new Keyring({ type: 'sr25519' });

var WsProvider = http.WsProvider
var ApiPromise = http.ApiPromise
const wsProvider = new WsProvider('ws://10.233.75.89:9944');
//const wsProvider = new WsProvider('ws://vmchain-node-2-sandbox.chain-sandbox.svc.cluster.local:9944');

class subscanJob extends Controller {
  async index() {
    const api = await ApiPromise.create({
      provider: wsProvider,
      types:typesData
    });
    const { ctx } = this;
    const blockHashs = await ctx.service.blocks.getSubScanBlockHash();
    console.log(blockHashs.length);
    // console.log(blockHashs[0]['block_number']);
    // console.log(blockHashs[0]['block_hash']);
    // console.log(blockHashs[0]['block_timestamp']);

    ctx.service.blocks.updateSubScanBlockState(blockHashs[0]['block_number']);

    blockHashs.forEach(item => {
      // {
      //   block_number: 4735,
      //       block_hash: '0x4dcc60704bdb13c909c4531d61cc55a268ce3c9528d3c5df37da1a262c1f8e58',
      //     block_timestamp: 2021-12-28T03:35:24.000Z
      // }
      // try {
      //   let block = api.rpc.chain.getBlock(item['block_hash']);
      //   signedBlock.block.extrinsics.forEach((ex, index) => {
      //     console.log(index, ex.hash.toHex());
      //     let txHash = ex.hash.toHex();
      //     let block_hash = item['block_hash'];
      //     let block_number = item['block_number'];
      //     let status = '0x1';
      //
      //     let tx_timestamp = '0x1';
      //     let from_addr = '0x1';
      //     let to_addr = '0x1';
      //     let value = '0x1';
      //     let nonce = '0x1';
      //     let input_method = '0x1';
      //   });
      //   console.log(block);
      // } catch (err) {
      //   console.log(item['block_hash']);
      // }

    });

    const blockHash = "0xda2bcddaf9d017cd981588c214a204bf2c934098ad319da88ade2f911da88a7d";

    const signedBlock = await api.rpc.chain.getBlock(blockHash);

    if (signedBlock.block.extrinsics.length > 1){
      for(var i = 0; i  < signedBlock.block.extrinsics.length; i ++) {
        console.log(signedBlock.block.extrinsics[i].hash.toHex());
        console.log(signedBlock.block.extrinsics[i].signature.nonce);
        console.log(signedBlock.block.extrinsics[i].signature.tip);
        console.log(signedBlock.block.extrinsics[i].signature.era);
        console.log(signedBlock.block.extrinsics[i].signature.signature);
        // console.log(signedBlock.block.extrinsics[i].signature.signer.);
      }
    }

    // const { meta, method, section }   = api.registry.findMetaCall( signedBlock.block.extrinsics[1].method.callIndex);

    useSuccess(ctx, signedBlock.block.extrinsics[1]);
    // try {
    //   const { ctx } = this;
    //  // useSuccess(ctx, callInfo);
    //   useSuccess(ctx, signedBlock.block.extrinsics[0]);
    // } catch (err) {
    //   console.log(err);
    // }
  }
}

module.exports = subscanJob;
