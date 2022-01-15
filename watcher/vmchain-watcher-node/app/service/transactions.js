'use strict';

const _ = require('lodash');
const Service = require('egg').Service;
const TABLE = 'transaction';
const { env } = require('process');


class TransactionsService extends Service {
  constructor(ctx) {
    super(ctx);
    this.database = this.app.mysql;
  }


  async getTransactionByHash(hash) {
    const res = await this.database.query(`select  * from  ${TABLE} where  transaction_hash = ?`,[hash]);
    return res;
  }


  base64(txt){

    let monyer = new Array();
    for(let i=0;i<txt.length;i++){
      const s=txt.charCodeAt(i).toString(16);
       monyer = monyer + new Array(5-String(s).length).join("0")+s;
    }
    return monyer;
  }


  /**
     * 增加transaction详情
     * @param transaction_hash transaction hash
     * @return
     */
  async addTransactionsDetail(block,tx) {


    let exitTransaction =  await this.getTransactionByHash(tx.transaction_hash);
    if(exitTransaction[0]) {
      return;
    }else{
      const addTxSql = 'INSERT INTO transaction (' +
          'transaction_hash,block_hash,block_number,tx_timestamp,from_addr,to_addr,value,coin_symbol,price,buyer_fee,seller_fee,amount,chain_type' +
          ') VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)';
      let time = tx.transaction_time.toString();
      if(time.length == 10){
        time = time + "000";
      }
      let newTime = new Date(time);
      tx.transaction_time = newTime;
      const addTxSql_Params = [ tx.transaction_hash,block.block_hash, block.block_height,tx.transaction_time,tx.buyer_address,tx.seller_address,tx.amount,tx.coin_symbol,tx.price,tx.buyer_fee,tx.seller_fee,tx.amount,'CPoS'];
      await this.app.mysql.query(addTxSql, addTxSql_Params, function(err, result) {
        if (err) {
          console.log('[INSERT SUBSCABTX ERROR] - ', err.message);
          return;
        }
        console.log('INSERT ID:', result);
      });
    }


  }
}

module.exports = TransactionsService;
