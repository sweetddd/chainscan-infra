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

  /**
     * 增加transaction详情
     * @param transaction_hash transaction hash
     * @return
     */
  async addTransactionsDetail({ id: transaction_hash }) {
    const addTxSql = 'INSERT INTO ${TABLE} (' +
        'transaction_hash,block_hash,block_number,status,tx_timestamp,from_addr,to_addr,value,nonce,input_method' +
        ') VALUES(?,?,?,?,?,?,?,?,?,?)';
    const addTxSql_Params = [ 'Wilson', 55 ];
    await this.app.mysql.query(addTxSql, addTxSql_Params, function(err, result) {
      if (err) {
        console.log('[INSERT SUBSCABTX ERROR] - ', err.message);
        return;
      }
      console.log('-------INSERT SUBSCABTX----------');
      console.log('INSERT ID:', result);
      console.log('#######################');
    });
  }
}

module.exports = TransactionsService;
