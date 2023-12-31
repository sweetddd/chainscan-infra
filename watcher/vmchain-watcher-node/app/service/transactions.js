"use strict";

const _ = require("lodash");
const Service = require("egg").Service;
const TABLE = "transaction";
const { env } = require("process");
const web3 = require("web3");
const { hexToStr } = require("../utils");
const moment = require("moment");

class TransactionsService extends Service {
  constructor(ctx) {
    super(ctx);
    this.database = this.app.mysql;
  }

  async getTransactionByHash(hash) {
    const res = await this.database.query(
      `select  * from  ${TABLE} where  transaction_hash = ?`,
      [hash]
    );
    return res;
  }

  hexToStr(hex, encoding) {
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
    for (let i = 0; i < len; i = i + 2) {
      curCharCode = parseInt(rawStr.substr(i, 2), 16);
      resultStr.push(curCharCode);
    }
    // encoding为空时默认为utf-8
    let bytesView = new Uint8Array(resultStr);
    let str = new TextDecoder(encoding).decode(bytesView);
    return str;
  }

  async sumVolume(startHeight, endHeight) {
    const res = await this.database.query(
      `select  sum(value) from  ${TABLE} where chain_type = 'CPoS' and block_number >= ? and block_number <= ?`,
      [startHeight, endHeight]
    );
    return res;
  }

  /**
   * 增加transaction详情
   * @param transaction_hash transaction hash
   * @return
   */
  async addTransactionsDetail(block, tx) {
    // let exitTransaction =  await this.getTransactionByHash(tx.transaction_hash);
    // if(exitTransaction[0]) {
    //   return;
    // }else{
    //
    // }

    const addTxSql =
      "INSERT INTO transaction (" +
      "transaction_hash,block_hash,block_number,tx_timestamp,from_address,to_address,from_symbol,to_symbol,from_fee,to_fee,from_amount,to_amount,chain_type,chain_id" +
      ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    let time = tx.transaction_time.toString();
    if (time.length == 10) {
      time = time + "000";
    }
    time =moment.utc(Number(time)).format('YYYY-MM-DD HH:mm:ss')
    // let newTime = new Date(time);
    tx.transaction_time = time;
    tx.amount = web3.utils.hexToNumberString(tx.amount);
    const addTxSql_Params = [
      tx.transaction_hash,
      block.block_hash,
      block.block_height,
      new Date(),
      tx.buyer_address,
      tx.seller_address,
      tx.token_0 ,
      tx.token_1,
      tx.fee_0,
      tx.fee_1,
      tx.amount_0,
      tx.amount_1,
      "CPoS",
        tx.chain_id
    ];
    try {
      await this.app.mysql.query(
        addTxSql,
        addTxSql_Params,
        function (err, result) {
          if (err) {
            console.log("[INSERT SUBSCABTX ERROR] - ", err.message);
            return;
          }
          console.log("INSERT ID:", result);
        }
      );
    } catch (error) { 
        console.log("[INSERT SUBSCABTX ERROR] - ", error);
        throw new Error(error)
    }
  }
}

module.exports = TransactionsService;
