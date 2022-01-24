'use strict';

const _ = require('lodash');
const Service = require('egg').Service;
const TABLE = 'dividend_record';
const { env } = require('process');

class DividendRecordService extends Service {
    constructor(ctx) {
        super(ctx);
        this.database = this.app.mysql;
    }




    async addRecord(dividendRecord) {


        //ividendRecord.time = newTime;
        const addTxSql = 'INSERT INTO dividend_record (' +
            'mining_details, earnings,  volume,  transactions,  mining_earnings,   time, create_time,burnt' +
            ') VALUES(?,?,?,?,?,?,?,?)';
        const addTxSql_Params = [dividendRecord.mining_details,dividendRecord.earnings ,dividendRecord.volume   ,dividendRecord.transactions   ,dividendRecord.mining_earnings, dividendRecord.time, new Date(),dividendRecord.burnt];
        await this.app.mysql.query(addTxSql, addTxSql_Params, function(err, result) {
            if (err) {
                console.log('[INSERT SUBSCABTX ERROR] - ', err.message);
                return;
            }
            console.log('INSERT ID:', result);
            console.log('#######################');

        });

    }




}

module.exports = DividendRecordService;
