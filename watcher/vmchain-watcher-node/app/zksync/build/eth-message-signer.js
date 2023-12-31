'use strict';
const __createBinding = (this && this.__createBinding) || (Object.create ? function(o, m, k, k2) {
  if (k2 === undefined) k2 = k;
  Object.defineProperty(o, k2, { enumerable: true, get() { return m[k]; } });
} : function(o, m, k, k2) {
  if (k2 === undefined) k2 = k;
  o[k2] = m[k];
});
const __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? function(o, v) {
  Object.defineProperty(o, 'default', { enumerable: true, value: v });
} : function(o, v) {
  o.default = v;
});
const __importStar = (this && this.__importStar) || function(mod) {
  if (mod && mod.__esModule) return mod;
  const result = {};
  if (mod != null) for (const k in mod) if (k !== 'default' && Object.prototype.hasOwnProperty.call(mod, k)) __createBinding(result, mod, k);
  __setModuleDefault(result, mod);
  return result;
};
const __awaiter = (this && this.__awaiter) || function(thisArg, _arguments, P, generator) {
  function adopt(value) { return value instanceof P ? value : new P(function(resolve) { resolve(value); }); }
  return new (P || (P = Promise))(function(resolve, reject) {
    function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
    function rejected(value) { try { step(generator.throw(value)); } catch (e) { reject(e); } }
    function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
    step((generator = generator.apply(thisArg, _arguments || [])).next());
  });
};
Object.defineProperty(exports, '__esModule', { value: true });
exports.EthMessageSigner = void 0;
const ethers = __importStar(require('ethers'));
const utils_1 = require('./utils');
/**
 * Wrapper around `ethers.Signer` which provides convenient methods to get and sign messages required for zkSync.
 */
class EthMessageSigner {
  constructor(ethSigner, ethSignerType) {
    this.ethSigner = ethSigner;
    this.ethSignerType = ethSignerType;
  }
  getEthMessageSignature(message) {
    return __awaiter(this, void 0, void 0, function* () {
      if (this.ethSignerType == null) {
        throw new Error('ethSignerType is unknown');
      }
      const signedBytes = utils_1.getSignedBytesFromMessage(message, !this.ethSignerType.isSignedMsgPrefixed);
      const signature = yield utils_1.signMessagePersonalAPI(this.ethSigner, signedBytes);
      console.log(this.ethSignerType.verificationMethod === 'ECDSA' ? 'EthereumSignature' : 'EIP1271Signature');
      return {
        type: this.ethSignerType.verificationMethod === 'ECDSA' ? 'EthereumSignature' : 'EIP1271Signature',
        signature,
      };
    });
  }
  getTransferEthSignMessage(transfer) {
    let humanReadableTxInfo = this.getTransferEthMessagePart(transfer);
    if (humanReadableTxInfo.length != 0) {
      humanReadableTxInfo += '\n';
    }
    humanReadableTxInfo += `Nonce: ${transfer.nonce}`;
    return humanReadableTxInfo;
  }
  ethSignTransfer(transfer) {
    return __awaiter(this, void 0, void 0, function* () {
      const message = this.getTransferEthSignMessage(transfer);
      return yield this.getEthMessageSignature(message);
    });
  }
  ethSignSwap(swap) {
    return __awaiter(this, void 0, void 0, function* () {
      const message = this.getSwapEthSignMessage(swap);
      return yield this.getEthMessageSignature(message);
    });
  }
  ethSignOrder(order) {
    return __awaiter(this, void 0, void 0, function* () {
      const message = this.getOrderEthSignMessage(order);
      return yield this.getEthMessageSignature(message);
    });
  }
  getSwapEthSignMessagePart(swap) {
    if (swap.fee != '0' && swap.fee) {
      return `Swap fee: ${swap.fee} ${swap.feeToken}`;
    }
    return '';
  }
  getSwapEthSignMessage(swap) {
    let message = this.getSwapEthSignMessagePart(swap);
    if (message != '') {
      message += '\n';
    }
    message += `Nonce: ${swap.nonce}`;
    return message;
  }
  getOrderEthSignMessage(order) {
    let message;
    if (order.amount == '0' || order.amount == null && order.type == 'limit_price') {
      message = `Limit order for ${order.tokenSell} -> ${order.tokenBuy}\n`;
    } else if (order.type == 'market_price') {
      message = `Market order for ${order.tokenSell} -> ${order.tokenBuy}\n`;
    } else {
      message = `Order for ${order.amount} ${order.tokenSell} -> ${order.tokenBuy}\n`;
    }
    if (!order.type) {
      message +=
            `Ratio: ${order.ratio[0].toString()}:${order.ratio[1].toString()}\n` +
                `Address: ${order.recipient.toLowerCase()}\n` +
                `Nonce: ${order.nonce}`;
    } else {
      const sell = order.direction == 'buy' ? order.tokenSell : order.tokenBuy;
      const buy = order.direction == 'buy' ? order.tokenBuy : order.tokenSell;
      const gasUnit = order.direction == 'buy' ? buy : sell;
      const tolerance = order.type == 'limit_price' ? '' : order.tolerance[0] / order.tolerance[1] * 100 + '%';
      message +=
            `Price: ${order.priceShow} ${sell}\n` +
            `Amount: ${order.amountShow} ${buy}\n` +
            `Total: ${order.totalShow} ${sell}\n` +
            `Transaction fee: ${order.fee}\n` +
            `Gas fee per transaction ${order.gasPrice} ${gasUnit}\n` +
            (order.type == 'limit_price' ? '' : `Price tolerance: ${tolerance}\n`) +
            `Address: ${order.recipient.toLowerCase()}\n` +
            `Nonce: ${order.nonce}`;
    }
    console.log('eth-message', message);
    return message;
  }
  ethSignForcedExit(forcedExit) {
    return __awaiter(this, void 0, void 0, function* () {
      const message = this.getForcedExitEthSignMessage(forcedExit);
      return yield this.getEthMessageSignature(message);
    });
  }
  getMintNFTEthMessagePart(mintNFT) {
    let humanReadableTxInfo = `MintNFT ${mintNFT.contentHash} for: ${mintNFT.recipient.toLowerCase()}`;
    if (mintNFT.stringFee != null) {
      humanReadableTxInfo += `\nFee: ${mintNFT.stringFee} ${mintNFT.stringFeeToken}`;
    }
    return humanReadableTxInfo;
  }
  getMintNFTEthSignMessage(mintNFT) {
    let humanReadableTxInfo = this.getMintNFTEthMessagePart(mintNFT);
    humanReadableTxInfo += `\nNonce: ${mintNFT.nonce}`;
    return humanReadableTxInfo;
  }
  getWithdrawNFTEthMessagePart(withdrawNFT) {
    let humanReadableTxInfo = `WithdrawNFT ${withdrawNFT.token} to: ${withdrawNFT.to.toLowerCase()}`;
    if (withdrawNFT.stringFee != null) {
      humanReadableTxInfo += `\nFee: ${withdrawNFT.stringFee} ${withdrawNFT.stringFeeToken}`;
    }
    return humanReadableTxInfo;
  }
  getWithdrawNFTEthSignMessage(withdrawNFT) {
    let humanReadableTxInfo = this.getWithdrawNFTEthMessagePart(withdrawNFT);
    humanReadableTxInfo += `\nNonce: ${withdrawNFT.nonce}`;
    return humanReadableTxInfo;
  }
  getWithdrawEthSignMessage(withdraw) {
    let humanReadableTxInfo = this.getWithdrawEthMessagePart(withdraw);
    if (humanReadableTxInfo.length != 0) {
      humanReadableTxInfo += '\n';
    }
    humanReadableTxInfo += `Nonce: ${withdraw.nonce}`;
    return humanReadableTxInfo;
  }
  getForcedExitEthSignMessage(forcedExit) {
    let humanReadableTxInfo = this.getForcedExitEthMessagePart(forcedExit);
    humanReadableTxInfo += `\nNonce: ${forcedExit.nonce}`;
    return humanReadableTxInfo;
  }
  getTransferEthMessagePart(tx) {
    let txType,
      to;
    if (tx.ethAddress != undefined) {
      txType = 'Withdraw';
      to = tx.ethAddress;
    } else if (tx.to != undefined) {
      txType = 'Transfer';
      to = tx.to;
    } else {
      throw new Error('Either to or ethAddress field must be present');
    }
    let message = '';
    if (tx.stringAmount != null) {
      message += `${txType} ${tx.stringAmount} ${tx.stringToken} to: ${to.toLowerCase()}`;
    }
    if (tx.stringFee != null) {
      if (message.length != 0) {
        message += '\n';
      }
      message += `Fee: ${tx.stringFee} ${tx.stringToken}`;
    }
    return message;
  }
  getWithdrawEthMessagePart(tx) {
    return this.getTransferEthMessagePart(tx);
  }
  getChangePubKeyEthMessagePart(changePubKey) {
    let message = '';
    message += `Set signing key: ${changePubKey.pubKeyHash.replace('sync:', '').toLowerCase()}`;
    if (changePubKey.stringFee != null) {
      message += `\nFee: ${changePubKey.stringFee} ${changePubKey.stringToken}`;
    }
    return message;
  }
  getForcedExitEthMessagePart(forcedExit) {
    let message = `ForcedExit ${forcedExit.stringToken} to: ${forcedExit.target.toLowerCase()}`;
    if (forcedExit.stringFee != null) {
      message += `\nFee: ${forcedExit.stringFee} ${forcedExit.stringToken}`;
    }
    return message;
  }
  ethSignMintNFT(mintNFT) {
    return __awaiter(this, void 0, void 0, function* () {
      const message = this.getMintNFTEthSignMessage(mintNFT);
      return yield this.getEthMessageSignature(message);
    });
  }
  ethSignWithdrawNFT(withdrawNFT) {
    return __awaiter(this, void 0, void 0, function* () {
      const message = this.getWithdrawNFTEthSignMessage(withdrawNFT);
      return yield this.getEthMessageSignature(message);
    });
  }
  ethSignWithdraw(withdraw) {
    return __awaiter(this, void 0, void 0, function* () {
      const message = this.getWithdrawEthSignMessage(withdraw);
      return yield this.getEthMessageSignature(message);
    });
  }
  getChangePubKeyEthSignMessage(changePubKey) {
    return utils_1.getChangePubkeyMessage(changePubKey.pubKeyHash, changePubKey.nonce, changePubKey.accountId);
  }
  ethSignChangePubKey(changePubKey) {
    return __awaiter(this, void 0, void 0, function* () {
      const message = this.getChangePubKeyEthSignMessage(changePubKey);
      return yield this.getEthMessageSignature(message);
    });
  }
  ethSignRegisterFactoryMessage(factoryAddress, accountId, accountAddress) {
    return __awaiter(this, void 0, void 0, function* () {
      const factoryAddressHex = ethers.utils.hexlify(utils_1.serializeAddress(factoryAddress)).substr(2);
      const accountAddressHex = ethers.utils.hexlify(utils_1.serializeAddress(accountAddress)).substr(2);
      const msgAccId = ethers.utils.hexlify(utils_1.serializeAccountId(accountId)).substr(2);
      const message = `\nCreator's account ID in zkSync: ${msgAccId}\n` +
                `Creator: ${accountAddressHex}\n` +
                `Factory: ${factoryAddressHex}`;
      const msgBytes = ethers.utils.toUtf8Bytes(message);
      return yield this.getEthMessageSignature(msgBytes);
    });
  }
}
exports.EthMessageSigner = EthMessageSigner;
