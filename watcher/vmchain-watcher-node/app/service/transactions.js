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
      "transaction_hash,block_hash,block_number,tx_timestamp,from_addr,to_addr,value,coin_symbol,price,buyer_fee,seller_fee,amount,chain_type" +
      ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)";
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
      tx.transaction_time,
      tx.buyer_address,
      tx.seller_address,
      tx.transaction_volume,
      tx.buy_symbol +"-"+ tx.sell_symbol,
      tx.price,
      tx.buyer_fee,
      tx.seller_fee,
      tx.amount,
      "CPoS",
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
    } catch (error) {}
  }
}

module.exports = TransactionsService;
