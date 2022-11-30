package jp.co.soramitsu.wallet.impl.presentation.balance.list

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeableState
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import jp.co.soramitsu.account.api.domain.interfaces.AccountRepository
import jp.co.soramitsu.account.api.presentation.actions.AddAccountBottomSheet
import jp.co.soramitsu.common.AlertViewState
import jp.co.soramitsu.common.address.AddressIconGenerator
import jp.co.soramitsu.common.address.AddressModel
import jp.co.soramitsu.common.address.createAddressModel
import jp.co.soramitsu.common.base.BaseViewModel
import jp.co.soramitsu.common.compose.component.ActionItemType
import jp.co.soramitsu.common.compose.component.AssetBalanceViewState
import jp.co.soramitsu.common.compose.component.ChainSelectorViewState
import jp.co.soramitsu.common.compose.component.ChangeBalanceViewState
import jp.co.soramitsu.common.compose.component.HiddenItemState
import jp.co.soramitsu.common.compose.component.MainToolbarViewState
import jp.co.soramitsu.common.compose.component.MultiToggleButtonState
import jp.co.soramitsu.common.compose.component.SwipeState
import jp.co.soramitsu.common.compose.component.ToolbarHomeIconState
import jp.co.soramitsu.common.compose.viewstate.AssetListItemViewState
import jp.co.soramitsu.common.data.network.coingecko.FiatChooserEvent
import jp.co.soramitsu.common.data.network.coingecko.FiatCurrency
import jp.co.soramitsu.common.domain.AppVersion
import jp.co.soramitsu.common.domain.FiatCurrencies
import jp.co.soramitsu.common.domain.GetAvailableFiatCurrencies
import jp.co.soramitsu.common.domain.SelectedFiat
import jp.co.soramitsu.common.domain.get
import jp.co.soramitsu.common.mixin.api.NetworkStateMixin
import jp.co.soramitsu.common.mixin.api.NetworkStateUi
import jp.co.soramitsu.common.mixin.api.UpdatesMixin
import jp.co.soramitsu.common.mixin.api.UpdatesProviderUi
import jp.co.soramitsu.common.model.AssetKey
import jp.co.soramitsu.common.presentation.LoadingState
import jp.co.soramitsu.common.resources.ResourceManager
import jp.co.soramitsu.common.utils.Event
import jp.co.soramitsu.common.utils.format
import jp.co.soramitsu.common.utils.formatAsChange
import jp.co.soramitsu.common.utils.formatAsCurrency
import jp.co.soramitsu.common.utils.inBackground
import jp.co.soramitsu.common.utils.map
import jp.co.soramitsu.common.utils.mapList
import jp.co.soramitsu.common.utils.orZero
import jp.co.soramitsu.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import jp.co.soramitsu.fearless_utils.ss58.SS58Encoder.addressByteOrNull
import jp.co.soramitsu.feature_wallet_impl.R
import jp.co.soramitsu.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.runtime.multiNetwork.chain.model.defaultChainSort
import jp.co.soramitsu.runtime.multiNetwork.chain.model.polkadotChainId
import jp.co.soramitsu.wallet.impl.data.mappers.mapAssetToAssetModel
import jp.co.soramitsu.wallet.impl.domain.ChainInteractor
import jp.co.soramitsu.wallet.impl.domain.interfaces.WalletInteractor
import jp.co.soramitsu.wallet.impl.domain.model.AssetWithStatus
import jp.co.soramitsu.wallet.impl.domain.model.WalletAccount
import jp.co.soramitsu.wallet.impl.presentation.AssetPayload
import jp.co.soramitsu.wallet.impl.presentation.WalletRouter
import jp.co.soramitsu.wallet.impl.presentation.balance.chainselector.ChainItemState
import jp.co.soramitsu.wallet.impl.presentation.balance.chainselector.ChainSelectScreenViewState
import jp.co.soramitsu.wallet.impl.presentation.balance.chainselector.toChainItemState
import jp.co.soramitsu.wallet.impl.presentation.balance.list.model.AssetType
import jp.co.soramitsu.wallet.impl.presentation.balance.list.model.BalanceModel
import jp.co.soramitsu.wallet.impl.presentation.model.AssetModel
import jp.co.soramitsu.wallet.impl.presentation.model.AssetUpdateState
import jp.co.soramitsu.wallet.impl.presentation.model.AssetWithStateModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val CURRENT_ICON_SIZE = 40

@HiltViewModel
class BalanceListViewModel @Inject constructor(
    private val interactor: WalletInteractor,
    private val chainInteractor: ChainInteractor,
    private val addressIconGenerator: AddressIconGenerator,
    private val router: WalletRouter,
    private val getAvailableFiatCurrencies: GetAvailableFiatCurrencies,
    private val selectedFiat: SelectedFiat,
    private val accountRepository: AccountRepository,
    private val updatesMixin: UpdatesMixin,
    private val networkStateMixin: NetworkStateMixin,
    private val resourceManager: ResourceManager
) : BaseViewModel(), UpdatesProviderUi by updatesMixin, NetworkStateUi by networkStateMixin, WalletScreenInterface {

    private val accountAddressToChainIdMap = mutableMapOf<String, ChainId?>()

    private val _hideRefreshEvent = MutableLiveData<Event<Unit>>()
    val hideRefreshEvent: LiveData<Event<Unit>> = _hideRefreshEvent

    private val _showFiatChooser = MutableLiveData<FiatChooserEvent>()
    val showFiatChooser: LiveData<FiatChooserEvent> = _showFiatChooser

    private val _showUnsupportedChainAlert = MutableLiveData<Event<Unit>>()
    val showUnsupportedChainAlert: LiveData<Event<Unit>> = _showUnsupportedChainAlert

    private val _openPlayMarket = MutableLiveData<Event<Unit>>()
    val openPlayMarket: LiveData<Event<Unit>> = _openPlayMarket

    private val enteredChainQueryFlow = MutableStateFlow("")

    private val connectingChainIdsFlow = networkStateMixin.chainConnectionsLiveData.map {
        it.filter { (_, isConnecting) -> isConnecting }.keys
    }.asFlow()

    private val fiatSymbolFlow = combine(selectedFiat.flow(), getAvailableFiatCurrencies.flow()) { selectedFiat: String, fiatCurrencies: FiatCurrencies ->
        fiatCurrencies[selectedFiat]?.symbol
    }.onEach {
        sync()
    }

    private val chainsFlow = chainInteractor.getChainsFlow().mapList {
        it.toChainItemState()
    }
    private val selectedChainId = MutableStateFlow<ChainId?>(null)

    val chainsState = combine(chainsFlow, selectedChainId, enteredChainQueryFlow) { chainItems, selectedChainId, searchQuery ->
        val chains = chainItems
            .filter {
                searchQuery.isEmpty() || it.title.contains(searchQuery, true) || it.tokenSymbols.any { it.second.contains(searchQuery, true) }
            }
            .sortedWith(compareBy<ChainItemState> { it.id.defaultChainSort() }.thenBy { it.title })

        ChainSelectScreenViewState(
            chains = chains,
            selectedChainId = selectedChainId,
            searchQuery = searchQuery
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ChainSelectScreenViewState.default)

    private val fiatSymbolLiveData = fiatSymbolFlow
    private val assetModelsLiveData = assetModelsFlow()

    private val balanceLiveData = combine(
        assetModelsLiveData,
        fiatSymbolLiveData,
        tokenRatesUpdate.asFlow(),
        assetsUpdate.asFlow(),
        chainsUpdate.asFlow()
    ) { assetModels: List<AssetModel>?, fiatSymbol: String?, tokenRatesUpdate: Set<String>?, assetsUpdate: Set<AssetKey>?, chainsUpdate: Set<String>? ->
        val assetsWithState = assetModels?.map { asset ->
            val rateUpdate = tokenRatesUpdate?.let { asset.token.configuration.id in it }
            val balanceUpdate = assetsUpdate?.let { asset.primaryKey in it }
            val chainUpdate = chainsUpdate?.let { asset.token.configuration.chainId in it }
            val isTokenFiatChanged = when {
                fiatSymbol == null -> false
                asset.token.fiatSymbol == null -> false
                else -> fiatSymbol != asset.token.fiatSymbol
            }

            AssetWithStateModel(
                asset = asset,
                state = AssetUpdateState(rateUpdate, balanceUpdate, chainUpdate, isTokenFiatChanged)
            )
        }.orEmpty()

        BalanceModel(assetsWithState, fiatSymbol.orEmpty())
    }.inBackground()

    private val hiddenAssetsState = MutableLiveData(HiddenItemState(isExpanded = false))

    private val assetTypeSelectorState = MutableLiveData(
        MultiToggleButtonState(
            currentSelection = AssetType.Currencies,
            toggleStates = AssetType.values().toList()
        )
    )

    private val assetStates = combine(
        interactor.assetsFlow(),
        chainInteractor.getChainsFlow(),
        selectedChainId,
        connectingChainIdsFlow
    ) { assets: List<AssetWithStatus>, chains: List<Chain>, selectedChainId: ChainId?, chainConnecting: Set<ChainId> ->
        val assetStates = mutableListOf<AssetListItemViewState>()
        assets
            .filter { it.hasAccount || !it.asset.markedNotNeed }
            .filter { selectedChainId == null || selectedChainId == it.asset.token.configuration.chainId }
            .sortedWith(defaultAssetListSort())
            .map { assetWithStatus ->
                val token = assetWithStatus.asset.token
                val chainAsset = token.configuration

                val chainLocal = chains.find { it.id == token.configuration.chainId }

                val isSupported: Boolean = when (chainLocal?.minSupportedVersion) {
                    null -> true
                    else -> AppVersion.isSupported(chainLocal.minSupportedVersion)
                }

                val hasNetworkIssue = token.configuration.chainId in chainConnecting

                val assetChainUrls = when (selectedChainId) {
                    null -> chains.filter { it.assets.any { it.symbolToShow == chainAsset.symbolToShow } }
                        .associate { it.id to it.icon }
                    else -> emptyMap()
                }

                val stateItem = assetStates.find { it.displayName == chainAsset.symbolToShow }
                if (stateItem == null) {
                    val assetListItemViewState = AssetListItemViewState(
                        assetIconUrl = chainAsset.iconUrl,
                        assetChainName = chainLocal?.name.orEmpty(),
                        assetSymbol = chainAsset.symbol,
                        displayName = chainAsset.symbolToShow,
                        assetTokenFiat = token.fiatRate?.formatAsCurrency(token.fiatSymbol),
                        assetTokenRate = token.recentRateChange?.formatAsChange(),
                        assetBalance = assetWithStatus.asset.total.orZero().format(),
                        assetBalanceFiat = token.fiatRate?.multiply(assetWithStatus.asset.total.orZero())?.formatAsCurrency(token.fiatSymbol),
                        assetChainUrls = assetChainUrls,
                        chainId = chainLocal?.id.orEmpty(),
                        chainAssetId = chainAsset.id,
                        isSupported = isSupported,
                        isHidden = !assetWithStatus.asset.enabled,
                        hasAccount = assetWithStatus.hasAccount,
                        priceId = chainAsset.priceId,
                        hasNetworkIssue = hasNetworkIssue
                    )

                    assetStates.add(assetListItemViewState)
                }
            }
        assetStates
    }.inBackground().onStart { emit(buildInitialAssetsList().toMutableList()) }

    // we open screen - no assets in the list
    private suspend fun buildInitialAssetsList(): List<AssetListItemViewState> {
        val chains = chainInteractor.getChainsFlow().first()

        val chainAssets = chains.map { it.assets }.flatten().sortedWith(defaultChainAssetListSort())
        return chainAssets.map { chainAsset ->
            val chain = requireNotNull(chains.find { it.id == chainAsset.chainId })

            val assetChainUrls = chains.filter { it.assets.any { it.symbolToShow == chainAsset.symbolToShow } }
                .associate { it.id to it.icon }

            val isSupported: Boolean = when (chain.minSupportedVersion) {
                null -> true
                else -> AppVersion.isSupported(chain.minSupportedVersion)
            }

            AssetListItemViewState(
                assetIconUrl = chainAsset.iconUrl,
                assetChainName = chainAsset.chainName,
                assetSymbol = chainAsset.symbol,
                displayName = chainAsset.symbolToShow,
                assetTokenFiat = null,
                assetTokenRate = null,
                assetBalance = null,
                assetBalanceFiat = null,
                assetChainUrls = assetChainUrls,
                chainId = chainAsset.chainId,
                chainAssetId = chainAsset.id,
                isSupported = isSupported,
                isHidden = false,
                hasAccount = true,
                priceId = chainAsset.priceId,
                hasNetworkIssue = false
            )
        }.filter { selectedChainId.value == null || selectedChainId.value == it.chainId }
    }

    private fun defaultAssetListSort() = compareByDescending<AssetWithStatus> { it.asset.total.orZero() > BigDecimal.ZERO }
        .thenByDescending { it.asset.fiatAmount.orZero() }
        .thenBy { it.asset.token.configuration.isTestNet }
        .thenBy { it.asset.token.configuration.chainId.defaultChainSort() }
        .thenBy { it.asset.token.configuration.chainName }

    private fun defaultChainAssetListSort() = compareBy<Chain.Asset> { it.isTestNet }
        .thenBy { it.chainId.defaultChainSort() }
        .thenBy { it.chainName }

    val state = combine(
        assetStates,
        assetTypeSelectorState.asFlow(),
        balanceLiveData,
        hiddenAssetsState.asFlow()
    ) { assetsListItemStates: List<AssetListItemViewState>,
        multiToggleButtonState: MultiToggleButtonState<AssetType>,
        balanceModel: BalanceModel,
        hiddenState: HiddenItemState ->

        val balanceState = AssetBalanceViewState(
            balance = balanceModel.totalBalance?.formatAsCurrency(balanceModel.fiatSymbol).orEmpty(),
            address = "",
            changeViewState = ChangeBalanceViewState(
                percentChange = balanceModel.rate?.formatAsChange().orEmpty(),
                fiatChange = balanceModel.totalBalanceChange.abs().formatAsCurrency(balanceModel.fiatSymbol)
            )
        )

        val hasNetworkIssues = assetsListItemStates.any { !it.hasAccount || it.hasNetworkIssue }

        LoadingState.Loaded(
            WalletState(
                multiToggleButtonState,
                assetsListItemStates,
                balanceState,
                hiddenState,
                hasNetworkIssues
            )
        )
    }.stateIn(scope = this, started = SharingStarted.Eagerly, initialValue = LoadingState.Loading())

    private val selectedChainItemFlow = selectedChainId.flatMapLatest { selectedChainId ->
        chainsFlow.map { chainsState ->
            chainsState.firstOrNull {
                it.id == selectedChainId
            }
        }
    }

    val toolbarState = combine(currentAddressModelFlow(), selectedChainItemFlow) { addressModel, chain ->
        chainsFlow
        LoadingState.Loaded(
            MainToolbarViewState(
                title = addressModel.nameOrAddress,
                homeIconState = ToolbarHomeIconState(walletIcon = addressModel.image),
                selectorViewState = ChainSelectorViewState(chain?.title, chain?.id)
            )
        )
    }.stateIn(scope = this, started = SharingStarted.Eagerly, initialValue = LoadingState.Loading())

    private fun sync() {
        viewModelScope.launch {
            getAvailableFiatCurrencies.sync()

            val result = interactor.syncAssetsRates()

            result.exceptionOrNull()?.let(::showError)
            _hideRefreshEvent.value = Event(Unit)
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    override fun actionItemClicked(actionType: ActionItemType, chainId: ChainId, chainAssetId: String, swipeableState: SwipeableState<SwipeState>) {
        val payload = AssetPayload(chainId, chainAssetId)
        launch {
            swipeableState.snapTo(SwipeState.INITIAL)
        }
        when (actionType) {
            ActionItemType.SEND -> {
                sendClicked(payload)
            }
            ActionItemType.RECEIVE -> {
                receiveClicked(payload)
            }
            ActionItemType.TELEPORT -> {
                showMessage("YOU NEED THE BLUE KEY")
            }
            ActionItemType.HIDE -> {
                launch { hideAsset(chainId, chainAssetId) }
            }
            ActionItemType.SHOW -> {
                launch { showAsset(chainId, chainAssetId) }
            }
            else -> {}
        }
    }

    private suspend fun hideAsset(chainId: ChainId, chainAssetId: String) {
        interactor.markAssetAsHidden(chainId, chainAssetId)
    }

    private suspend fun showAsset(chainId: ChainId, chainAssetId: String) {
        interactor.markAssetAsShown(chainId, chainAssetId)
    }

    private fun sendClicked(assetPayload: AssetPayload) {
        router.openSend(assetPayload)
    }

    private fun receiveClicked(assetPayload: AssetPayload) {
        router.openReceive(assetPayload)
    }

    override fun assetClicked(asset: AssetListItemViewState) {
        if (asset.hasNetworkIssue) {
            launch {
                val chain = interactor.getChain(asset.chainId)
                if (chain.nodes.size > 1) {
                    router.openNodes(asset.chainId)
                } else {
                    val payload = AlertViewState(
                        title = resourceManager.getString(R.string.staking_main_network_title, chain.name),
                        message = resourceManager.getString(R.string.network_issue_unavailable),
                        buttonText = resourceManager.getString(R.string.top_up),
                        iconRes = R.drawable.ic_alert_16
                    )
                    router.openAlert(payload)
                }
            }
            return
        }
        if (!asset.hasAccount) {
            launch {
                val meta = accountRepository.getSelectedMetaAccount()
                val payload = AddAccountBottomSheet.Payload(
                    metaId = meta.id,
                    chainId = asset.chainId,
                    chainName = asset.assetChainName,
                    assetId = asset.chainAssetId,
                    priceId = asset.priceId,
                    markedAsNotNeed = false
                )
                router.openOptionsAddAccount(payload)
            }
            return
        }
        if (asset.isSupported.not()) {
            _showUnsupportedChainAlert.value = Event(Unit)
            return
        }

        val payload = AssetPayload(
            chainId = asset.chainId,
            chainAssetId = asset.chainAssetId
        )

        router.openAssetDetails(payload)
    }

    fun onChainSelected(item: ChainItemState? = null) {
        selectedChainId.value = item?.id
        viewModelScope.launch {
            val currentAddress = interactor.selectedAccountFlow(polkadotChainId).first().address
            accountAddressToChainIdMap[currentAddress] = item?.id
        }
    }

    fun onChainSearchEntered(query: String) {
        enteredChainQueryFlow.value = query
    }

    override fun onHiddenAssetClicked() {
        hiddenAssetsState.value = HiddenItemState(
            isExpanded = hiddenAssetsState.value?.isExpanded?.not() ?: false
        )
    }

    private fun currentAddressModelFlow(): Flow<AddressModel> {
        return interactor.selectedAccountFlow(polkadotChainId)
            .onEach {
                if (accountAddressToChainIdMap.containsKey(it.address).not()) {
                    selectedChainId.value = null
                    accountAddressToChainIdMap[it.address] = null
                } else {
                    selectedChainId.value = accountAddressToChainIdMap.getOrDefault(it.address, null)
                }
            }
            .map { generateAddressModel(it, CURRENT_ICON_SIZE) }
    }

    private suspend fun generateAddressModel(account: WalletAccount, sizeInDp: Int): AddressModel {
        return addressIconGenerator.createAddressModel(account.address, sizeInDp, account.name)
    }

    private fun assetModelsFlow(): Flow<List<AssetModel>> =
        interactor.assetsFlow()
            .mapList {
                when {
                    it.hasAccount -> it.asset
                    else -> null
                }
            }
            .map { it.filterNotNull() }
            .mapList { mapAssetToAssetModel(it) }

    override fun onBalanceClicked() {
        viewModelScope.launch {
            val currencies = getAvailableFiatCurrencies()
            if (currencies.isEmpty()) return@launch
            val selected = selectedFiat.get()
            val selectedItem = currencies.first { it.id == selected }
            _showFiatChooser.value = FiatChooserEvent(DynamicListBottomSheet.Payload(currencies, selectedItem))
        }
    }

    override fun onNetworkIssuesClicked() {
        router.openNetworkIssues()
    }

    fun onFiatSelected(item: FiatCurrency) {
        viewModelScope.launch {
            selectedFiat.set(item.id)
        }
    }

    fun updateAppClicked() {
        _openPlayMarket.value = Event(Unit)
    }

    override fun assetTypeChanged(type: AssetType) {
        assetTypeSelectorState.value = assetTypeSelectorState.value?.copy(currentSelection = type)
    }

    fun qrCodeScanned(content: String) {
        viewModelScope.launch {
            val result = interactor.tryReadAddressFromSoraFormat(content) ?: content
            val qrTokenId = interactor.tryReadTokenIdFromSoraFormat(content)
            val payloadFromQr = qrTokenId?.let {
                val addressChains = interactor.getChains().first()
                    .filter { it.addressPrefix.toShort() == result.addressByteOrNull() }
                    .filter { it.assets.any { it.currencyId == qrTokenId } }
                if (addressChains.size == 1) {
                    val chain = addressChains[0]
                    val soraAsset = chain.assets.firstOrNull {
                        it.currencyId == qrTokenId
                    }

                    soraAsset?.let {
                        AssetPayload(it.chainId, it.id)
                    }
                } else {
                    null
                }
            }
            router.openSend(assetPayload = payloadFromQr, initialSendToAddress = result, currencyId = qrTokenId)
        }
    }

    fun openWalletSelector() {
        router.openSelectWallet()
    }

    fun openSearchAssets() {
        router.openSearchAssets(selectedChainId.value)
    }
}
