package jp.co.soramitsu.wallet.impl.presentation

import android.graphics.drawable.Drawable
import it.airgap.beaconsdk.blockchain.substrate.data.SubstrateSignerPayload
import jp.co.soramitsu.account.api.presentation.actions.AddAccountBottomSheet
import jp.co.soramitsu.common.AlertViewState
import jp.co.soramitsu.common.navigation.DelayedNavigation
import jp.co.soramitsu.common.navigation.PinRequired
import jp.co.soramitsu.common.navigation.SecureRouter
import jp.co.soramitsu.common.navigation.payload.WalletSelectorPayload
import jp.co.soramitsu.common.presentation.StoryGroupModel
import jp.co.soramitsu.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.wallet.api.domain.model.XcmChainType
import jp.co.soramitsu.wallet.impl.domain.beacon.SignStatus
import jp.co.soramitsu.wallet.impl.domain.model.PhishingType
import jp.co.soramitsu.wallet.impl.presentation.balance.detail.frozen.FrozenAssetPayload
import jp.co.soramitsu.wallet.impl.presentation.balance.walletselector.light.WalletSelectionMode
import jp.co.soramitsu.wallet.impl.presentation.beacon.main.DAppMetadataModel
import jp.co.soramitsu.wallet.impl.presentation.cross_chain.CrossChainTransferDraft
import jp.co.soramitsu.wallet.impl.presentation.model.OperationParcelizeModel
import jp.co.soramitsu.wallet.impl.presentation.send.TransferDraft
import jp.co.soramitsu.wallet.impl.presentation.transaction.detail.extrinsic.ExtrinsicDetailsPayload
import jp.co.soramitsu.wallet.impl.presentation.transaction.detail.reward.RewardDetailsPayload
import kotlinx.coroutines.flow.Flow
import jp.co.soramitsu.wallet.api.presentation.WalletRouter as WalletRouterApi

interface WalletRouter : SecureRouter, WalletRouterApi {
    fun openAssetDetails(assetPayload: AssetPayload)

    fun openSend(assetPayload: AssetPayload?, initialSendToAddress: String? = null, currencyId: String? = null)

    fun openCrossChainSend(assetPayload: AssetPayload?)

    fun openSwapTokensScreen(chainId: String, assetIdFrom: String?, assetIdTo: String?)

    fun openSelectChain(
        assetId: String,
        chainId: ChainId? = null,
        chooserMode: Boolean = true,
        isSelectAsset: Boolean = true
    )

    fun openSelectChain(
        selectedChainId: ChainId? = null,
        filterChainIds: List<ChainId>? = null,
        chooserMode: Boolean = true,
        currencyId: String? = null,
        showAllChains: Boolean = true,
        isSelectAsset: Boolean = true
    )

    fun openSelectChainForXcm(
        selectedChainId: ChainId?,
        xcmChainType: XcmChainType,
        selectedOriginChainId: String? = null,
        xcmAssetSymbol: String? = null
    )

    fun openSelectAsset(selectedAssetId: String)

    fun openSelectAsset(chainId: ChainId, selectedAssetId: String?, isFilterXcmAssets: Boolean)

    fun openFilter()

    fun openOperationSuccess(operationHash: String?, chainId: ChainId)

    fun openSendConfirm(transferDraft: TransferDraft, phishingType: PhishingType?)

    fun openCrossChainSendConfirm(transferDraft: CrossChainTransferDraft, phishingType: PhishingType?)

    fun finishSendFlow()

    fun openTransferDetail(transaction: OperationParcelizeModel.Transfer, assetPayload: AssetPayload)

    fun openSwapDetail(operation: OperationParcelizeModel.Swap)

    fun openExtrinsicDetail(payload: ExtrinsicDetailsPayload)

    fun openRewardDetail(payload: RewardDetailsPayload)

    fun openCreateAccountFromWallet()

    fun openAccountDetails(metaAccountId: Long)

    fun openExportWallet(metaAccountId: Long)

    fun openImportAccountScreen(blockChainType: Int)

    fun openImportAccountScreenFromWallet(blockChainType: Int)

    fun openReceive(assetPayload: AssetPayload)

    fun openSignBeaconTransaction(payload: SubstrateSignerPayload, dAppMetadata: DAppMetadataModel)

    val beaconSignStatus: Flow<SignStatus>

    fun setBeaconSignStatus(status: SignStatus)

    fun openNodes(chainId: ChainId)

    @PinRequired
    fun openExportMnemonic(metaId: Long, chainId: ChainId): DelayedNavigation

    @PinRequired
    fun openExportSeed(metaId: Long, chainId: ChainId): DelayedNavigation

    @PinRequired
    fun openExportJsonPassword(metaId: Long, chainId: ChainId): DelayedNavigation

    fun openOnboardingNavGraph(chainId: ChainId, metaId: Long, isImport: Boolean)

    fun openEducationalStories(stories: StoryGroupModel)

    fun openSuccessFragment(avatar: Drawable)

    fun openTransactionRawData(rawData: String)

    fun openSelectWallet()

    fun openNetworkIssues()

    fun openGetSoraCard()

    fun openOptionsAddAccount(payload: AddAccountBottomSheet.Payload)

    fun openAlert(payload: AlertViewState)

    fun openAlert(payload: AlertViewState, resultKey: String)

    fun openSearchAssets()

    fun openOptionsWallet(walletId: Long)

    fun setWalletSelectorPayload(payload: WalletSelectorPayload)

    fun openFrozenTokens(payload: FrozenAssetPayload)

    fun openAddressHistory(chainId: ChainId)

    fun openWalletSelectorForResult(
        selectedWalletId: Long?,
        walletSelectionMode: WalletSelectionMode
    ): Flow<Long>

    fun openAddressHistoryWithResult(chainId: ChainId): Flow<String>

    fun openCreateContact(chainId: ChainId?, address: String?)

    val chainSelectorPayloadFlow: Flow<ChainId?>
}
