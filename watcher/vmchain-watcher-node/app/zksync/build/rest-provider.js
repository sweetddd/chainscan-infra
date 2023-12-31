'use strict';
const __awaiter = (this && this.__awaiter) || function(thisArg, _arguments, P, generator) {
  function adopt(value) { return value instanceof P ? value : new P(function(resolve) { resolve(value); }); }
  return new (P || (P = Promise))(function(resolve, reject) {
    function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
    function rejected(value) { try { step(generator.throw(value)); } catch (e) { reject(e); } }
    function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
    step((generator = generator.apply(thisArg, _arguments || [])).next());
  });
};
const __importDefault = (this && this.__importDefault) || function(mod) {
  return (mod && mod.__esModule) ? mod : { default: mod };
};
Object.defineProperty(exports, '__esModule', { value: true });
exports.RestProvider = exports.RESTError = exports.getDefaultRestProvider = void 0;
const axios_1 = __importDefault(require('axios'));
const ethers_1 = require('ethers');
const provider_interface_1 = require('./provider-interface');
const utils_1 = require('./utils');
function getDefaultRestProvider(network) {
  return __awaiter(this, void 0, void 0, function* () {
    if (network === 'localhost') {
      return yield RestProvider.newProvider('http://127.0.0.1:3001/api/v0.2');
    } else if (network === 'ropsten') {
      return yield RestProvider.newProvider('https://ropsten-api.zksync.io/api/v0.2');
    } else if (network === 'rinkeby') {
      return yield RestProvider.newProvider('https://rinkeby-api.zksync.io/api/v0.2');
    } else if (network === 'ropsten-beta') {
      return yield RestProvider.newProvider('https://ropsten-beta-api.zksync.io/api/v0.2');
    } else if (network === 'rinkeby-beta') {
      return yield RestProvider.newProvider('https://rinkeby-beta-api.zksync.io/api/v0.2');
    } else if (network === 'mainnet') {
      return yield RestProvider.newProvider('https://api.zksync.io/api/v0.2');
    }

    throw new Error(`Ethereum network ${network} is not supported`);

  });
}
exports.getDefaultRestProvider = getDefaultRestProvider;
class RESTError extends Error {
  constructor(message, restError) {
    super(message);
    this.restError = restError;
  }
}
exports.RESTError = RESTError;
class RestProvider extends provider_interface_1.SyncProvider {
  constructor(address) {
    super();
    this.address = address;
    this.providerType = 'Rest';
  }
  static newProvider(address = 'http://127.0.0.1:3001/api/v0.2', pollIntervalMilliSecs) {
    return __awaiter(this, void 0, void 0, function* () {
      const provider = new RestProvider(address);
      if (pollIntervalMilliSecs) {
        provider.pollIntervalMilliSecs = pollIntervalMilliSecs;
      }
      provider.contractAddress = yield provider.getContractAddress();
      provider.tokenSet = new utils_1.TokenSet(yield provider.getTokens());
      return provider;
    });
  }
  parseResponse(response) {
    if (response.status === 'success') {
      return response.result;
    }

    throw new RESTError(`zkSync API response error: errorType: ${response.error.errorType};` +
                ` code ${response.error.code}; message: ${response.error.message}`, response.error);

  }
  get(url) {
    return __awaiter(this, void 0, void 0, function* () {
      return yield axios_1.default.get(url).then(resp => {
        return resp.data;
      });
    });
  }
  post(url, body) {
    return __awaiter(this, void 0, void 0, function* () {
      return yield axios_1.default.post(url, body).then(resp => {
        return resp.data;
      });
    });
  }
  accountInfoDetailed(idOrAddress, infoType) {
    return __awaiter(this, void 0, void 0, function* () {
      return yield this.get(`${this.address}/accounts/${idOrAddress}/${infoType}`);
    });
  }
  accountInfo(idOrAddress, infoType) {
    return __awaiter(this, void 0, void 0, function* () {
      return this.parseResponse(yield this.accountInfoDetailed(idOrAddress, infoType));
    });
  }
  accountFullInfoDetailed(idOrAddress) {
    return __awaiter(this, void 0, void 0, function* () {
      return yield this.get(`${this.address}/accounts/${idOrAddress}`);
    });
  }
  accountFullInfo(idOrAddress) {
    return __awaiter(this, void 0, void 0, function* () {
      return this.parseResponse(yield this.accountFullInfoDetailed(idOrAddress));
    });
  }
  accountTxsDetailed(idOrAddress, paginationQuery) {
    return __awaiter(this, void 0, void 0, function* () {
      return yield this.get(`${this.address}/accounts/${idOrAddress}/transactions?from=${paginationQuery.from}` +
                `&limit=${paginationQuery.limit}&direction=${paginationQuery.direction}`);
    });
  }
  accountTxs(idOrAddress, paginationQuery) {
    return __awaiter(this, void 0, void 0, function* () {
      return this.parseResponse(yield this.accountTxsDetailed(idOrAddress, paginationQuery));
    });
  }
  accountPendingTxsDetailed(idOrAddress, paginationQuery) {
    return __awaiter(this, void 0, void 0, function* () {
      return yield this.get(`${this.address}/accounts/${idOrAddress}/transactions/pending?from=${paginationQuery.from}` +
                `&limit=${paginationQuery.limit}&direction=${paginationQuery.direction}`);
    });
  }
  accountPendingTxs(idOrAddress, paginationQuery) {
    return __awaiter(this, void 0, void 0, function* () {
      return this.parseResponse(yield this.accountPendingTxsDetailed(idOrAddress, paginationQuery));
    });
  }
  blockPaginationDetailed(paginationQuery) {
    return __awaiter(this, void 0, void 0, function* () {
      return yield this.get(`${this.address}/blocks?from=${paginationQuery.from}&limit=${paginationQuery.limit}` +
                `&direction=${paginationQuery.direction}`);
    });
  }
  blockPagination(paginationQuery) {
    return __awaiter(this, void 0, void 0, function* () {
      return this.parseResponse(yield this.blockPaginationDetailed(paginationQuery));
    });
  }
  blockByPositionDetailed(blockPosition) {
    return __awaiter(this, void 0, void 0, function* () {
      return yield this.get(`${this.address}/blocks/${blockPosition}`);
    });
  }
  blockByPosition(blockPosition) {
    return __awaiter(this, void 0, void 0, function* () {
      return this.parseResponse(yield this.blockByPositionDetailed(blockPosition));
    });
  }
  blockTransactionsDetailed(blockPosition, paginationQuery) {
    return __awaiter(this, void 0, void 0, function* () {
      return yield this.get(`${this.address}/blocks/${blockPosition}/transactions?from=${paginationQuery.from}` +
                `&limit=${paginationQuery.limit}&direction=${paginationQuery.direction}`);
    });
  }
  blockTransactions(blockPosition, paginationQuery) {
    return __awaiter(this, void 0, void 0, function* () {
      return this.parseResponse(yield this.blockTransactionsDetailed(blockPosition, paginationQuery));
    });
  }
  configDetailed() {
    return __awaiter(this, void 0, void 0, function* () {
      return yield this.get(`${this.address}/config`);
    });
  }
  config() {
    return __awaiter(this, void 0, void 0, function* () {
      return this.parseResponse(yield this.configDetailed());
    });
  }
  getTransactionFeeDetailed(txType, address, tokenLike) {
    return __awaiter(this, void 0, void 0, function* () {
      const rawFee = yield this.post(`${this.address}/fee`, {
        txType,
        address,
        tokenLike,
      });
      let fee;
      if (rawFee.status === 'success') {
        fee = {
          request: rawFee.request,
          status: rawFee.status,
          error: null,
          result: {
            gasFee: ethers_1.BigNumber.from(rawFee.result.gasFee),
            zkpFee: ethers_1.BigNumber.from(rawFee.result.zkpFee),
            totalFee: ethers_1.BigNumber.from(rawFee.result.totalFee),
          },
        };
      } else {
        fee = {
          request: rawFee.request,
          status: rawFee.status,
          error: rawFee.error,
          result: null,
        };
      }
      return fee;
    });
  }
  getTransactionFee(txType, address, tokenLike) {
    return __awaiter(this, void 0, void 0, function* () {
      return this.parseResponse(yield this.getTransactionFeeDetailed(txType, address, tokenLike));
    });
  }
  getBatchFullFeeDetailed(transactions, tokenLike) {
    return __awaiter(this, void 0, void 0, function* () {
      const rawFee = yield this.post(`${this.address}/fee/batch`, { transactions, tokenLike });
      let fee;
      if (rawFee.status === 'success') {
        fee = {
          request: rawFee.request,
          status: rawFee.status,
          error: null,
          result: {
            gasFee: ethers_1.BigNumber.from(rawFee.result.gasFee),
            zkpFee: ethers_1.BigNumber.from(rawFee.result.zkpFee),
            totalFee: ethers_1.BigNumber.from(rawFee.result.totalFee),
          },
        };
      } else {
        fee = {
          request: rawFee.request,
          status: rawFee.status,
          error: rawFee.error,
          result: null,
        };
      }
      return fee;
    });
  }
  getBatchFullFee(transactions, tokenLike) {
    return __awaiter(this, void 0, void 0, function* () {
      return this.parseResponse(yield this.getBatchFullFeeDetailed(transactions, tokenLike));
    });
  }
  networkStatusDetailed() {
    return __awaiter(this, void 0, void 0, function* () {
      return yield this.get(`${this.address}/networkStatus`);
    });
  }
  networkStatus() {
    return __awaiter(this, void 0, void 0, function* () {
      return this.parseResponse(yield this.networkStatusDetailed());
    });
  }
  tokenPaginationDetailed(paginationQuery) {
    return __awaiter(this, void 0, void 0, function* () {
      return yield this.get(`${this.address}/tokens?from=${paginationQuery.from}&limit=${paginationQuery.limit}` +
                `&direction=${paginationQuery.direction}`);
    });
  }
  tokenPagination(paginationQuery) {
    return __awaiter(this, void 0, void 0, function* () {
      return this.parseResponse(yield this.tokenPaginationDetailed(paginationQuery));
    });
  }
  tokenByIdOrAddressDetailed(idOrAddress) {
    return __awaiter(this, void 0, void 0, function* () {
      return yield this.get(`${this.address}/tokens/${idOrAddress}`);
    });
  }
  tokenByIdOrAddress(idOrAddress) {
    return __awaiter(this, void 0, void 0, function* () {
      return this.parseResponse(yield this.tokenByIdOrAddressDetailed(idOrAddress));
    });
  }
  tokenPriceInfoDetailed(idOrAddress, tokenIdOrUsd) {
    return __awaiter(this, void 0, void 0, function* () {
      return yield this.get(`${this.address}/tokens/${idOrAddress}/priceIn/${tokenIdOrUsd}`);
    });
  }
  tokenPriceInfo(idOrAddress, tokenIdOrUsd) {
    return __awaiter(this, void 0, void 0, function* () {
      return this.parseResponse(yield this.tokenPriceInfoDetailed(idOrAddress, tokenIdOrUsd));
    });
  }
  submitTxNewDetailed(tx, signature) {
    return __awaiter(this, void 0, void 0, function* () {
      return yield this.post(`${this.address}/transactions`, { tx, signature });
    });
  }
  submitTxNew(tx, signature) {
    return __awaiter(this, void 0, void 0, function* () {
      return this.parseResponse(yield this.submitTxNewDetailed(tx, signature));
    });
  }
  /**
     * @deprecated Use submitTxNew method instead
     */
  submitTx(tx, signature, fastProcessing) {
    return __awaiter(this, void 0, void 0, function* () {
      if (fastProcessing) {
        tx.fastProcessing = fastProcessing;
      }
      const txHash = yield this.submitTxNew(tx, signature);
      txHash.replace('0x', 'sync-tx:');
      return txHash;
    });
  }
  txStatusDetailed(txHash) {
    return __awaiter(this, void 0, void 0, function* () {
      return yield this.get(`${this.address}/transactions/${txHash}`);
    });
  }
  txStatus(txHash) {
    return __awaiter(this, void 0, void 0, function* () {
      return this.parseResponse(yield this.txStatusDetailed(txHash));
    });
  }
  txDataDetailed(txHash) {
    return __awaiter(this, void 0, void 0, function* () {
      return yield this.get(`${this.address}/transactions/${txHash}/data`);
    });
  }
  txData(txHash) {
    return __awaiter(this, void 0, void 0, function* () {
      return this.parseResponse(yield this.txDataDetailed(txHash));
    });
  }
  submitTxsBatchNewDetailed(txs, signature) {
    return __awaiter(this, void 0, void 0, function* () {
      return yield this.post(`${this.address}/transactions/batches`, { txs, signature });
    });
  }
  submitTxsBatchNew(txs, signature) {
    return __awaiter(this, void 0, void 0, function* () {
      return this.parseResponse(yield this.submitTxsBatchNewDetailed(txs, signature));
    });
  }
  /**
     * @deprecated Use submitTxsBatchNew method instead.
     */
  submitTxsBatch(transactions, ethSignatures) {
    return __awaiter(this, void 0, void 0, function* () {
      return (yield this.submitTxsBatchNew(transactions, ethSignatures)).transactionHashes;
    });
  }
  getBatchDetailed(batchHash) {
    return __awaiter(this, void 0, void 0, function* () {
      return yield this.get(`${this.address}/transactions/batches/${batchHash}`);
    });
  }
  getBatch(batchHash) {
    return __awaiter(this, void 0, void 0, function* () {
      return this.parseResponse(yield this.getBatchDetailed(batchHash));
    });
  }
  getNFTDetailed(id) {
    return __awaiter(this, void 0, void 0, function* () {
      return yield this.get(`${this.address}/tokens/nft/${id}`);
    });
  }
  getNFT(id) {
    return __awaiter(this, void 0, void 0, function* () {
      const nft = this.parseResponse(yield this.getNFTDetailed(id));
      // If the NFT does not exist, throw an exception
      if (nft == null) {
        throw new Error('Requested NFT doesn\'t exist or the corresponding mintNFT operation is not verified yet');
      }
      return nft;
    });
  }
  notifyAnyTransaction(hash, action) {
    return __awaiter(this, void 0, void 0, function* () {
      while (true) {
        const transactionStatus = yield this.txStatus(hash);
        let notifyDone;
        if (action === 'COMMIT') {
          notifyDone = transactionStatus && transactionStatus.rollupBlock;
        } else {
          if (transactionStatus && transactionStatus.rollupBlock) {
            if (transactionStatus.status === 'rejected') {
              // If the transaction status is rejected
              // it cannot be known if transaction is queued, committed or finalized.
              // That is why there is separate `blockByPosition` query.
              const blockStatus = yield this.blockByPosition(transactionStatus.rollupBlock);
              notifyDone = blockStatus && blockStatus.status === 'finalized';
            } else {
              notifyDone = transactionStatus.status === 'finalized';
            }
          }
        }
        if (notifyDone) {
          // Transaction status needs to be recalculated because it can
          // be updated between `txStatus` and `blockByPosition` calls.
          return yield this.txStatus(hash);
        }

        yield utils_1.sleep(this.pollIntervalMilliSecs);

      }
    });
  }
  notifyTransaction(hash, action) {
    return __awaiter(this, void 0, void 0, function* () {
      yield this.notifyAnyTransaction(hash, action);
      return yield this.getTxReceipt(hash);
    });
  }
  notifyPriorityOp(hash, action) {
    return __awaiter(this, void 0, void 0, function* () {
      yield this.notifyAnyTransaction(hash, action);
      return yield this.getPriorityOpStatus(hash);
    });
  }
  getContractAddress() {
    return __awaiter(this, void 0, void 0, function* () {
      const config = yield this.config();
      return {
        mainContract: config.contract,
        govContract: config.govContract,
      };
    });
  }
  getTokens(limit) {
    return __awaiter(this, void 0, void 0, function* () {
      const tokens = {};
      let tmpId = 0;
      limit = limit ? limit : RestProvider.MAX_LIMIT;
      let tokenPage;
      do {
        tokenPage = yield this.tokenPagination({
          from: tmpId,
          limit,
          direction: 'newer',
        });
        for (const token of tokenPage.list) {
          tokens[token.symbol] = {
            address: token.address,
            id: token.id,
            symbol: token.symbol,
            decimals: token.decimals,
          };
        }
        tmpId += limit;
      } while (tokenPage.list.length == limit);
      return tokens;
    });
  }
  getState(address) {
    return __awaiter(this, void 0, void 0, function* () {
      const fullInfo = yield this.accountFullInfo(address);
      const defaultInfo = {
        balances: {},
        nonce: 0,
        pubKeyHash: 'sync:0000000000000000000000000000000000000000',
        nfts: {},
        mintedNfts: {},
      };
      if (fullInfo.finalized) {
        return {
          address,
          id: fullInfo.committed.accountId,
          accountType: fullInfo.committed.accountType,
          committed: {
            balances: fullInfo.committed.balances,
            nonce: fullInfo.committed.nonce,
            pubKeyHash: fullInfo.committed.pubKeyHash,
            nfts: fullInfo.committed.nfts,
            mintedNfts: fullInfo.committed.mintedNfts,
          },
          verified: {
            balances: fullInfo.finalized.balances,
            nonce: fullInfo.finalized.nonce,
            pubKeyHash: fullInfo.finalized.pubKeyHash,
            nfts: fullInfo.finalized.nfts,
            mintedNfts: fullInfo.finalized.mintedNfts,
          },
        };
      } else if (fullInfo.committed) {
        return {
          address,
          id: fullInfo.committed.accountId,
          accountType: fullInfo.committed.accountType,
          committed: {
            balances: fullInfo.committed.balances,
            nonce: fullInfo.committed.nonce,
            pubKeyHash: fullInfo.committed.pubKeyHash,
            nfts: fullInfo.committed.nfts,
            mintedNfts: fullInfo.committed.mintedNfts,
          },
          verified: defaultInfo,
        };
      }

      return {
        address,
        committed: defaultInfo,
        verified: defaultInfo,
      };

    });
  }
  getConfirmationsForEthOpAmount() {
    return __awaiter(this, void 0, void 0, function* () {
      const config = yield this.config();
      return config.depositConfirmations;
    });
  }
  getTransactionsBatchFee(txTypes, addresses, tokenLike) {
    return __awaiter(this, void 0, void 0, function* () {
      const transactions = [];
      for (let i = 0; i < txTypes.length; ++i) {
        transactions.push({ txType: txTypes[i], address: addresses[i] });
      }
      const fee = yield this.getBatchFullFee(transactions, tokenLike);
      return fee.totalFee;
    });
  }
  getTokenPrice(tokenLike) {
    return __awaiter(this, void 0, void 0, function* () {
      const price = yield this.tokenPriceInfo(tokenLike, 'usd');
      return price.price.toNumber();
    });
  }
  getTxReceipt(txHash) {
    return __awaiter(this, void 0, void 0, function* () {
      const receipt = yield this.txStatus(txHash);
      if (!receipt || !receipt.rollupBlock) {
        return {
          executed: false,
        };
      }

      if (receipt.status === 'rejected') {
        const blockFullInfo = yield this.blockByPosition(receipt.rollupBlock);
        const blockInfo = {
          blockNumber: receipt.rollupBlock,
          committed: !!blockFullInfo,
          verified: !!(blockFullInfo && blockFullInfo.status === 'finalized'),
        };
        return {
          executed: true,
          success: false,
          failReason: receipt.failReason,
          block: blockInfo,
        };
      }

      return {
        executed: true,
        success: true,
        block: {
          blockNumber: receipt.rollupBlock,
          committed: true,
          verified: receipt.status === 'finalized',
        },
      };


    });
  }
  getPriorityOpStatus(hash) {
    return __awaiter(this, void 0, void 0, function* () {
      const receipt = yield this.txStatus(hash);
      if (!receipt || !receipt.rollupBlock) {
        return {
          executed: false,
        };
      }

      return {
        executed: true,
        block: {
          blockNumber: receipt.rollupBlock,
          committed: true,
          verified: receipt.status === 'finalized',
        },
      };

    });
  }
  getEthTxForWithdrawal(withdrawalHash) {
    return __awaiter(this, void 0, void 0, function* () {
      const txData = yield this.txData(withdrawalHash);
      if (txData.tx.op.type === 'Withdraw' ||
                txData.tx.op.type === 'ForcedExit' ||
                txData.tx.op.type === 'WithdrawNFT') {
        return txData.tx.op.ethTxHash;
      }

      return null;

    });
  }
}
exports.RestProvider = RestProvider;
RestProvider.MAX_LIMIT = 100;
