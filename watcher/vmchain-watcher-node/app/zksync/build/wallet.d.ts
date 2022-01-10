import { BigNumber, BigNumberish, Contract, ContractTransaction, ethers } from 'ethers';
import { EthMessageSigner } from './eth-message-signer';
import { SyncProvider } from './provider-interface';
import { Signer } from './signer';
import { BatchBuilder } from './batch-builder';
import { AccountState, Address, ChangePubKey, ChangePubKeyCREATE2, ChangePubKeyECDSA, ChangePubKeyOnchain, ChangePubkeyTypes, Create2Data, EthSignerType, ForcedExit, MintNFT, NFT, Nonce, Order, PriorityOperationReceipt, PubKeyHash, SignedTransaction, Swap, TokenLike, TransactionReceipt, Transfer, TxEthSignature, Withdraw, WithdrawNFT, TokenRatio, WeiRatio } from './types';
export declare class ZKSyncTxError extends Error {
    value: PriorityOperationReceipt | TransactionReceipt;
    constructor(message: string, value: PriorityOperationReceipt | TransactionReceipt);
}
export declare class Wallet {
    ethSigner: ethers.Signer;
    ethMessageSigner: EthMessageSigner;
    cachedAddress: Address;
    signer?: Signer;
    accountId?: number;
    ethSignerType?: EthSignerType;
    provider: SyncProvider;
    private constructor();
    connect(provider: SyncProvider): this;
    static fromEthSigner(ethWallet: ethers.Signer, provider: SyncProvider, signer?: Signer, accountId?: number, ethSignerType?: EthSignerType): Promise<Wallet>;
    static fromCreate2Data(syncSigner: Signer, provider: SyncProvider, create2Data: Create2Data, accountId?: number): Promise<Wallet>;
    static fromEthSignerNoKeys(ethWallet: ethers.Signer, provider: SyncProvider, accountId?: number, ethSignerType?: EthSignerType): Promise<Wallet>;
    getEthMessageSignature(message: ethers.utils.BytesLike): Promise<TxEthSignature>;
    batchBuilder(nonce?: Nonce): BatchBuilder;
    getTransfer(transfer: {
        to: Address;
        token: TokenLike;
        amount: BigNumberish;
        fee: BigNumberish;
        nonce: number;
        validFrom: number;
        validUntil: number;
    }): Promise<Transfer>;
    signSyncTransfer(transfer: {
        to: Address;
        token: TokenLike;
        amount: BigNumberish;
        fee: BigNumberish;
        nonce: number;
        validFrom?: number;
        validUntil?: number;
    }): Promise<SignedTransaction>;
    signRegisterFactory(factoryAddress: Address): Promise<{
        signature: TxEthSignature;
        accountId: number;
        accountAddress: Address;
    }>;
    getForcedExit(forcedExit: {
        target: Address;
        token: TokenLike;
        fee: BigNumberish;
        nonce: number;
        validFrom?: number;
        validUntil?: number;
    }): Promise<ForcedExit>;
    signSyncForcedExit(forcedExit: {
        target: Address;
        token: TokenLike;
        fee: BigNumberish;
        nonce: number;
        validFrom?: number;
        validUntil?: number;
    }): Promise<SignedTransaction>;
    syncForcedExit(forcedExit: {
        target: Address;
        token: TokenLike;
        fee?: BigNumberish;
        nonce?: Nonce;
        validFrom?: number;
        validUntil?: number;
    }): Promise<Transaction>;
    syncMultiTransfer(transfers: {
        to: Address;
        token: TokenLike;
        amount: BigNumberish;
        fee: BigNumberish;
        nonce?: Nonce;
        validFrom?: number;
        validUntil?: number;
    }[]): Promise<Transaction[]>;
    syncTransferNFT(transfer: {
        to: Address;
        token: NFT;
        feeToken: TokenLike;
        fee?: BigNumberish;
        nonce?: Nonce;
        validFrom?: number;
        validUntil?: number;
    }): Promise<Transaction[]>;
    getLimitOrder(order: {
        tokenSell: TokenLike;
        tokenBuy: TokenLike;
        ratio: TokenRatio | WeiRatio;
        recipient?: Address;
        nonce?: Nonce;
        validFrom?: number;
        validUntil?: number;
    }): Promise<Order>;
    getOrder(order: {
        tokenSell: TokenLike;
        tokenBuy: TokenLike;
        ratio: TokenRatio | WeiRatio;
        amount: BigNumberish;
        recipient?: Address;
        nonce?: Nonce;
        validFrom?: number;
        validUntil?: number;
    }): Promise<Order>;
    signOrder(order: Order): Promise<Order>;
    getSwap(swap: {
        orders: [Order, Order];
        feeToken: number;
        amounts: [BigNumberish, BigNumberish];
        nonce: number;
        fee: BigNumberish;
    }): Promise<Swap>;
    signSyncSwap(swap: {
        orders: [Order, Order];
        feeToken: number;
        amounts: [BigNumberish, BigNumberish];
        nonce: number;
        fee: BigNumberish;
    }): Promise<SignedTransaction>;
    syncSwap(swap: {
        orders: [Order, Order];
        feeToken: TokenLike;
        amounts?: [BigNumberish, BigNumberish];
        nonce?: number;
        fee?: BigNumberish;
    }): Promise<Transaction>;
    syncTransfer(transfer: {
        to: Address;
        token: TokenLike;
        amount: BigNumberish;
        fee?: BigNumberish;
        nonce?: Nonce;
        validFrom?: number;
        validUntil?: number;
        message?: string;
    }): Promise<Transaction>;
    getMintNFT(mintNFT: {
        recipient: string;
        contentHash: string;
        feeToken: TokenLike;
        fee: BigNumberish;
        nonce: number;
    }): Promise<MintNFT>;
    getWithdrawNFT(withdrawNFT: {
        to: string;
        token: TokenLike;
        feeToken: TokenLike;
        fee: BigNumberish;
        nonce: number;
        validFrom: number;
        validUntil: number;
    }): Promise<WithdrawNFT>;
    getWithdrawFromSyncToEthereum(withdraw: {
        ethAddress: string;
        token: TokenLike;
        amount: BigNumberish;
        fee: BigNumberish;
        nonce: number;
        validFrom: number;
        validUntil: number;
    }): Promise<Withdraw>;
    signMintNFT(mintNFT: {
        recipient: string;
        contentHash: string;
        feeToken: TokenLike;
        fee: BigNumberish;
        nonce: number;
    }): Promise<SignedTransaction>;
    signWithdrawNFT(withdrawNFT: {
        to: string;
        token: number;
        feeToken: TokenLike;
        fee: BigNumberish;
        nonce: number;
        validFrom?: number;
        validUntil?: number;
    }): Promise<SignedTransaction>;
    signWithdrawFromSyncToEthereum(withdraw: {
        ethAddress: string;
        token: TokenLike;
        amount: BigNumberish;
        fee: BigNumberish;
        nonce: number;
        validFrom?: number;
        validUntil?: number;
    }): Promise<SignedTransaction>;
    mintNFT(mintNFT: {
        recipient: Address;
        contentHash: ethers.BytesLike;
        feeToken: TokenLike;
        fee?: BigNumberish;
        nonce?: Nonce;
    }): Promise<Transaction>;
    withdrawNFT(withdrawNFT: {
        to: string;
        token: number;
        feeToken: TokenLike;
        fee?: BigNumberish;
        nonce?: Nonce;
        fastProcessing?: boolean;
        validFrom?: number;
        validUntil?: number;
    }): Promise<Transaction>;
    withdrawFromSyncToEthereum(withdraw: {
        ethAddress: string;
        token: TokenLike;
        amount: BigNumberish;
        fee?: BigNumberish;
        nonce?: Nonce;
        fastProcessing?: boolean;
        validFrom?: number;
        validUntil?: number;
    }): Promise<Transaction>;
    isSigningKeySet(): Promise<boolean>;
    getChangePubKey(changePubKey: {
        feeToken: TokenLike;
        fee: BigNumberish;
        nonce: number;
        ethAuthData?: ChangePubKeyOnchain | ChangePubKeyECDSA | ChangePubKeyCREATE2;
        ethSignature?: string;
        validFrom: number;
        validUntil: number;
    }): Promise<ChangePubKey>;
    signSetSigningKey(changePubKey: {
        feeToken: TokenLike;
        fee: BigNumberish;
        nonce: number;
        ethAuthType: ChangePubkeyTypes;
        batchHash?: string;
        validFrom?: number;
        validUntil?: number;
    }): Promise<SignedTransaction>;
    setSigningKey(changePubKey: {
        feeToken: TokenLike;
        ethAuthType: ChangePubkeyTypes;
        fee?: BigNumberish;
        nonce?: Nonce;
        validFrom?: number;
        validUntil?: number;
    }): Promise<Transaction>;
    getWithdrawNFTEthMessagePart(withdrawNFT: {
        to: string;
        token: number;
        feeToken: TokenLike;
        fee: BigNumberish;
    }): string;
    getTransferEthMessagePart(transfer: {
        to: Address;
        token: TokenLike;
        amount: BigNumberish;
        fee: BigNumberish;
    }): Promise<string>;
    getWithdrawEthMessagePart(withdraw: {
        ethAddress: string;
        token: TokenLike;
        amount: BigNumberish;
        fee: BigNumberish;
    }): string;
    getChangePubKeyEthMessagePart(changePubKey: {
        pubKeyHash: string;
        feeToken: TokenLike;
        fee: BigNumberish;
    }): string;
    getMintNFTEthMessagePart(mintNFT: {
        recipient: string;
        contentHash: string;
        feeToken: TokenLike;
        fee: BigNumberish;
    }): string;
    getSwapEthSignMessagePart(swap: {
        fee: BigNumberish;
        feeToken: TokenLike;
    }): string;
    getForcedExitEthMessagePart(forcedExit: {
        target: Address;
        token: TokenLike;
        fee: BigNumberish;
    }): string;
    isOnchainAuthSigningKeySet(nonce?: Nonce): Promise<boolean>;
    onchainAuthSigningKey(nonce?: Nonce, ethTxOptions?: ethers.providers.TransactionRequest): Promise<ContractTransaction>;
    getCurrentPubKeyHash(): Promise<PubKeyHash>;
    getNonce(nonce?: Nonce): Promise<number>;
    getAccountId(): Promise<number | undefined>;
    address(): Address;
    getAccountState(): Promise<AccountState>;
    getNFT(tokenId: number, type?: 'committed' | 'verified'): Promise<NFT>;
    getBalance(token: TokenLike, type?: 'committed' | 'verified'): Promise<BigNumber>;
    getEthereumBalance(token: TokenLike): Promise<BigNumber>;
    isERC20DepositsApproved(token: TokenLike, erc20ApproveThreshold?: BigNumber): Promise<boolean>;
    approveERC20TokenDeposits(token: TokenLike, max_erc20_approve_amount?: BigNumber): Promise<ContractTransaction>;
    depositToSyncFromEthereum(deposit: {
        depositTo: Address;
        token: TokenLike;
        amount: BigNumberish;
        ethTxOptions?: ethers.providers.TransactionRequest;
        approveDepositAmountForERC20?: boolean;
    }): Promise<ETHOperation>;
    resolveAccountId(): Promise<number>;
    emergencyWithdraw(withdraw: {
        token: TokenLike;
        accountId?: number;
        ethTxOptions?: ethers.providers.TransactionRequest;
    }): Promise<ETHOperation>;
    emergencyWithdrawNFT(withdrawNFT: {
        tokenId: number;
        accountId?: number;
        ethTxOptions?: ethers.providers.TransactionRequest;
    }): Promise<ETHOperation>;
    getZkSyncMainContract(): Contract;
    private modifyEthersError;
    private setRequiredAccountIdFromServer;
}
export declare class ETHOperation {
    ethTx: ContractTransaction;
    zkSyncProvider: SyncProvider;
    state: 'Sent' | 'Mined' | 'Committed' | 'Verified' | 'Failed';
    error?: ZKSyncTxError;
    priorityOpId?: BigNumber;
    constructor(ethTx: ContractTransaction, zkSyncProvider: SyncProvider);
    awaitEthereumTxCommit(): Promise<ethers.ContractReceipt>;
    awaitReceipt(): Promise<PriorityOperationReceipt>;
    awaitVerifyReceipt(): Promise<PriorityOperationReceipt>;
    private setErrorState;
    private throwErrorIfFailedState;
}
export declare class Transaction {
    txData: any;
    txHash: string;
    sidechainProvider: SyncProvider;
    state: 'Sent' | 'Committed' | 'Verified' | 'Failed';
    error?: ZKSyncTxError;
    constructor(txData: any, txHash: string, sidechainProvider: SyncProvider);
    awaitReceipt(): Promise<TransactionReceipt>;
    awaitVerifyReceipt(): Promise<TransactionReceipt>;
    private setErrorState;
    private throwErrorIfFailedState;
}
export declare function submitSignedTransaction(signedTx: SignedTransaction, provider: SyncProvider, fastProcessing?: boolean): Promise<Transaction>;
export declare function submitSignedTransactionsBatch(provider: SyncProvider, signedTxs: SignedTransaction[], ethSignatures?: TxEthSignature[]): Promise<Transaction[]>;
