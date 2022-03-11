// This file is created by egg-ts-helper@1.29.1
// Do not modify this file!!!!!!!!!

import 'egg';
type AnyClass = new (...args: any[]) => any;
type AnyFunc<T = any> = (...args: any[]) => T;
type CanExportFunc = AnyFunc<Promise<any>> | AnyFunc<IterableIterator<any>>;
type AutoInstanceType<T, U = T extends CanExportFunc ? T : T extends AnyFunc ? ReturnType<T> : T> = U extends AnyClass ? InstanceType<U> : U;
import ExportBlocks = require('../../../app/service/blocks');
import ExportDividendRecord = require('../../../app/service/dividendRecord');
import ExportTransactions = require('../../../app/service/transactions');
import ExportUserRewards = require('../../../app/service/userRewards');

declare module 'egg' {
  interface IService {
    blocks: AutoInstanceType<typeof ExportBlocks>;
    dividendRecord: AutoInstanceType<typeof ExportDividendRecord>;
    transactions: AutoInstanceType<typeof ExportTransactions>;
    userRewards: AutoInstanceType<typeof ExportUserRewards>;
  }
}
