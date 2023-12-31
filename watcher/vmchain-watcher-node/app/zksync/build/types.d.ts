import { BigNumber, BigNumberish } from 'ethers';
export declare type Address = string;
export declare type PubKeyHash = string;
export declare type TokenLike = TokenSymbol | TokenAddress | number;
export declare type TokenSymbol = string;
export declare type TokenAddress = string;
export declare type TotalFee = Map<TokenLike, BigNumber>;
export declare type Nonce = number | 'committed';
export declare type Network = 'localhost' | 'rinkeby' | 'ropsten' | 'mainnet' | 'rinkeby-beta' | 'ropsten-beta';
export interface Create2Data {
    creatorAddress: string;
    saltArg: string;
    codeHash: string;
}
export interface NFT {
    id: number;
    symbol: string;
    creatorId: number;
    serialId: number;
    address: Address;
    creatorAddress: Address;
    contentHash: string;
}
export interface NFTInfo {
    id: number;
    symbol: string;
    creatorId: number;
    serialId: number;
    address: Address;
    creatorAddress: Address;
    contentHash: string;
    currentFactory: Address;
    withdrawnFactory?: Address;
}
export declare type EthAccountType = 'Owned' | 'CREATE2';
export declare type AccountState = AccountStateRest | AccountStateRpc;
export interface AccountStateRest {
    address: Address;
    id?: number;
    accountType?: EthAccountType;
    committed: {
        balances: {
            [token: string]: BigNumberish;
        };
        nfts: {
            [tokenId: number]: NFT;
        };
        mintedNfts: {
            [tokenId: number]: NFT;
        };
        nonce: number;
        pubKeyHash: PubKeyHash;
    };
    verified: {
        balances: {
            [token: string]: BigNumberish;
        };
        nfts: {
            [tokenId: number]: NFT;
        };
        mintedNfts: {
            [tokenId: number]: NFT;
        };
        nonce: number;
        pubKeyHash: PubKeyHash;
    };
}
export interface AccountStateRpc {
    address: Address;
    id?: number;
    accountType?: EthAccountType;
    depositing: {
        balances: {
            [token: string]: {
                amount: BigNumberish;
                expectedAcceptBlock: number;
            };
        };
    };
    committed: {
        balances: {
            [token: string]: BigNumberish;
        };
        nfts: {
            [tokenId: number]: NFT;
        };
        mintedNfts: {
            [tokenId: number]: NFT;
        };
        nonce: number;
        pubKeyHash: PubKeyHash;
    };
    verified: {
        balances: {
            [token: string]: BigNumberish;
        };
        nfts: {
            [tokenId: number]: NFT;
        };
        mintedNfts: {
            [tokenId: number]: NFT;
        };
        nonce: number;
        pubKeyHash: PubKeyHash;
    };
}
export declare type EthSignerType = {
    verificationMethod: 'ECDSA' | 'ERC-1271';
    isSignedMsgPrefixed: boolean;
};
export interface TxEthSignature {
    type: 'EthereumSignature' | 'EIP1271Signature';
    signature: string;
}
export interface Signature {
    pubKey: string;
    signature: string;
}
export declare type Ratio = [BigNumberish, BigNumberish];
export declare type TokenRatio = {
    type: 'Token';
    [token: string]: string | number;
    [token: number]: string | number;
};
export declare type WeiRatio = {
    type: 'Wei';
    [token: string]: BigNumberish;
    [token: number]: BigNumberish;
};
export interface Order {
    accountId: number;
    recipient: Address;
    nonce: number;
    tokenSell: number;
    tokenBuy: number;
    ratio: Ratio;
    amount: BigNumberish;
    signature?: Signature;
    ethSignature?: TxEthSignature;
    validFrom: number;
    validUntil: number;
}
export interface Swap {
    type: 'Swap';
    orders: [Order, Order];
    amounts: [BigNumberish, BigNumberish];
    submitterId: number;
    submitterAddress: Address;
    nonce: number;
    signature?: Signature;
    feeToken: number;
    fee: BigNumberish;
}
export interface Transfer {
    type: 'Transfer';
    accountId: number;
    from: Address;
    to: Address;
    token: number;
    amount: BigNumberish;
    fee: BigNumberish;
    nonce: number;
    signature?: Signature;
    validFrom: number;
    validUntil: number;
}
export interface Withdraw {
    type: 'Withdraw';
    accountId: number;
    from: Address;
    to: Address;
    token: number;
    amount: BigNumberish;
    fee: BigNumberish;
    nonce: number;
    signature?: Signature;
    validFrom: number;
    validUntil: number;
}
export interface MintNFT {
    type: 'MintNFT';
    creatorId: number;
    creatorAddress: Address;
    recipient: Address;
    contentHash: string;
    fee: BigNumberish;
    feeToken: number;
    nonce: number;
    signature?: Signature;
}
export interface WithdrawNFT {
    type: 'WithdrawNFT';
    accountId: number;
    from: Address;
    to: Address;
    token: number;
    feeToken: number;
    fee: BigNumberish;
    nonce: number;
    signature?: Signature;
    validFrom: number;
    validUntil: number;
}
export interface ForcedExit {
    type: 'ForcedExit';
    initiatorAccountId: number;
    target: Address;
    token: number;
    fee: BigNumberish;
    nonce: number;
    signature?: Signature;
    validFrom: number;
    validUntil: number;
}
export declare type ChangePubkeyTypes = 'Onchain' | 'ECDSA' | 'CREATE2' | 'ECDSALegacyMessage';
export interface ChangePubKeyOnchain {
    type: 'Onchain';
}
export interface ChangePubKeyECDSA {
    type: 'ECDSA';
    ethSignature: string;
    batchHash?: string;
}
export interface ChangePubKeyCREATE2 {
    type: 'CREATE2';
    creatorAddress: string;
    saltArg: string;
    codeHash: string;
}
export interface ChangePubKey {
    type: 'ChangePubKey';
    accountId: number;
    account: Address;
    newPkHash: PubKeyHash;
    feeToken: number;
    fee: BigNumberish;
    nonce: number;
    signature?: Signature;
    ethAuthData?: ChangePubKeyOnchain | ChangePubKeyECDSA | ChangePubKeyCREATE2;
    ethSignature?: string;
    validFrom: number;
    validUntil: number;
}
export interface CloseAccount {
    type: 'Close';
    account: Address;
    nonce: number;
    signature: Signature;
}
export declare type TxEthSignatureVariant = null | TxEthSignature | (TxEthSignature | null)[];
export interface SignedTransaction {
    tx: Transfer | Withdraw | ChangePubKey | CloseAccount | ForcedExit | MintNFT | WithdrawNFT | Swap;
    ethereumSignature?: TxEthSignatureVariant;
}
export interface BlockInfo {
    blockNumber: number;
    committed: boolean;
    verified: boolean;
}
export interface TransactionReceipt {
    executed: boolean;
    success?: boolean;
    failReason?: string;
    block?: BlockInfo;
}
export interface PriorityOperationReceipt {
    executed: boolean;
    block?: BlockInfo;
}
export interface ContractAddress {
    mainContract: string;
    govContract: string;
}
export interface Tokens {
    [token: string]: {
        address: string;
        id: number;
        symbol: string;
        decimals: number;
    };
}
export interface ChangePubKeyFee {
    "ChangePubKey": ChangePubkeyTypes;
}
export interface LegacyChangePubKeyFee {
    ChangePubKey: {
        onchainPubkeyAuth: boolean;
    };
}
export declare type Fee = FeeRpc | FeeRest;
export interface FeeRpc {
    feeType: 'Withdraw' | 'Transfer' | 'TransferToNew' | 'FastWithdraw' | ChangePubKeyFee | 'MintNFT' | 'WithdrawNFT' | 'Swap';
    gasTxAmount: BigNumber;
    gasPriceWei: BigNumber;
    gasFee: BigNumber;
    zkpFee: BigNumber;
    totalFee: BigNumber;
}
export declare type BatchFee = BatchFeeRpc | FeeRest;
export interface BatchFeeRpc {
    totalFee: BigNumber;
}
export declare type IncomingTxFeeType = 'Withdraw' | 'Transfer' | 'FastWithdraw' | 'ForcedExit' | 'MintNFT' | 'WithdrawNFT' | 'FastWithdrawNFT' | 'Swap' | ChangePubKeyFee | LegacyChangePubKeyFee;
export interface PaginationQuery<F> {
    from: F | 'latest';
    limit: number;
    direction: 'newer' | 'older';
}
export interface Paginated<T, F> {
    list: T[];
    pagination: {
        from: F;
        limit: number;
        direction: 'newer' | 'older';
        count: number;
    };
}
export interface ApiBlockInfo {
    blockNumber: number;
    newStateRoot: string;
    blockSize: number;
    commitTxHash?: string;
    verifyTxHash?: string;
    committedAt: string;
    finalizedAt?: string;
    status: 'committed' | 'finalized';
}
export declare type BlockPosition = number | 'lastCommitted' | 'lastFinalized';
export interface ApiAccountInfo {
    accountId: number;
    address: Address;
    nonce: number;
    pubKeyHash: PubKeyHash;
    lastUpdateInBlock: number;
    balances: {
        [token: string]: BigNumber;
    };
    accountType?: EthAccountType;
    nfts: {
        [tokenId: number]: NFT;
    };
    mintedNfts: {
        [tokenId: number]: NFT;
    };
}
export interface ApiAccountFullInfo {
    committed: ApiAccountInfo;
    finalized: ApiAccountInfo;
}
export interface ApiConfig {
    network: Network;
    contract: Address;
    govContract: Address;
    depositConfirmations: number;
    zksyncVersion: 'contractV4';
}
export interface FeeRest {
    gasFee: BigNumber;
    zkpFee: BigNumber;
    totalFee: BigNumber;
}
export interface NetworkStatus {
    lastCommitted: number;
    finalized: number;
    totalTransactions: number;
    mempoolSize: number;
}
export interface TokenInfo {
    id: number;
    address: Address;
    symbol: string;
    decimals: number;
    enabledForFees: boolean;
}
export interface TokenPriceInfo {
    tokenId: number;
    tokenSymbol: string;
    priceIn: string;
    decimals: number;
    price: BigNumber;
}
export interface SubmitBatchResponse {
    transactionHashes: string[];
    batchHash: string;
}
export interface ApiL1TxReceipt {
    status: 'queued' | 'committed' | 'finalized';
    ethBlock: number;
    rollupBlock?: number;
    id: number;
}
export declare type L2TxStatus = 'queued' | 'committed' | 'finalized' | 'rejected';
export interface ApiL2TxReceipt {
    txHash: string;
    rollupBlock?: number;
    status: L2TxStatus;
    failReason?: string;
}
export declare type ApiTxReceipt = ApiL1TxReceipt | ApiL2TxReceipt;
export interface WithdrawData {
    type: 'Withdraw';
    accountId: number;
    from: Address;
    to: Address;
    token: number;
    amount: BigNumberish;
    fee: BigNumberish;
    nonce: number;
    signature?: Signature;
    validFrom: number;
    validUntil: number;
    ethTxHash?: string;
}
export interface ForcedExitData {
    type: 'ForcedExit';
    initiatorAccountId: number;
    target: Address;
    token: number;
    fee: BigNumberish;
    nonce: number;
    signature?: Signature;
    validFrom: number;
    validUntil: number;
    ethTxHash?: string;
}
export interface WithdrawNFTData {
    type: 'WithdrawNFT';
    accountId: number;
    from: Address;
    to: Address;
    token: number;
    feeToken: number;
    fee: BigNumberish;
    nonce: number;
    signature?: Signature;
    validFrom: number;
    validUntil: number;
    ethTxHash?: string;
}
export interface ApiDeposit {
    type: 'Deposit';
    from: Address;
    tokenId: number;
    amount: BigNumber;
    to: Address;
    accountId?: number;
    ethHash: string;
    id: number;
    txHash: string;
}
export interface ApiFullExit {
    type: 'FullExit';
    accountId: number;
    tokenId: number;
    ethHash: string;
    id: number;
    txHash: string;
}
export declare type L2Tx = Transfer | Withdraw | ChangePubKey | ForcedExit | CloseAccount | MintNFT | WithdrawNFT | Swap;
export declare type L2TxData = Transfer | WithdrawData | ChangePubKey | ForcedExitData | CloseAccount | MintNFT | WithdrawNFTData | Swap;
export declare type TransactionData = L2TxData | ApiDeposit | ApiFullExit;
export interface ApiTransaction {
    txHash: string;
    blockNumber?: number;
    op: TransactionData;
    status: L2TxStatus;
    failReason?: string;
    createdAt?: string;
}
export interface ApiSignedTx {
    tx: ApiTransaction;
    ethSignature?: string;
}
export interface ApiBatchStatus {
    updatedAt: string;
    lastState: L2TxStatus;
}
export interface ApiBatchData {
    batchHash: string;
    transactionHashes: string[];
    createdAt: string;
    batchStatus: ApiBatchStatus;
}
