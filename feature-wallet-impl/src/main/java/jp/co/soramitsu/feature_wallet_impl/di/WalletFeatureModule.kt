package jp.co.soramitsu.feature_wallet_impl.di

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import jp.co.soramitsu.common.data.network.NetworkApiCreator
import jp.co.soramitsu.common.data.network.rpc.RxWebSocket
import jp.co.soramitsu.common.di.scope.FeatureScope
import jp.co.soramitsu.core_db.dao.AssetDao
import jp.co.soramitsu.core_db.dao.TransactionDao
import jp.co.soramitsu.fearless_utils.encrypt.KeypairFactory
import jp.co.soramitsu.fearless_utils.encrypt.Signer
import jp.co.soramitsu.fearless_utils.ss58.SS58Encoder
import jp.co.soramitsu.fearless_utils.wsrpc.Logger
import jp.co.soramitsu.feature_account_api.domain.interfaces.AccountRepository
import jp.co.soramitsu.feature_wallet_api.domain.interfaces.WalletInteractor
import jp.co.soramitsu.feature_wallet_api.domain.interfaces.WalletRepository
import jp.co.soramitsu.feature_wallet_impl.data.network.blockchain.WssSubstrateSource
import jp.co.soramitsu.feature_wallet_impl.data.network.subscan.SubscanNetworkApi
import jp.co.soramitsu.feature_wallet_impl.data.repository.WalletRepositoryImpl
import jp.co.soramitsu.feature_wallet_impl.domain.WalletInteractorImpl

@Module
class WalletFeatureModule {

    @Provides
    @FeatureScope
    fun provideSubscanApi(networkApiCreator: NetworkApiCreator): SubscanNetworkApi {
        return networkApiCreator.create(SubscanNetworkApi::class.java)
    }

    @Provides
    @FeatureScope
    fun provideRxWebSocket(mapper: Gson, logger: Logger) = RxWebSocket(mapper, logger)

    @Provides
    @FeatureScope
    fun provideSubstrateSource(
        rxWebSocket: RxWebSocket,
        keypairFactory: KeypairFactory,
        signer: Signer,
        sS58Encoder: SS58Encoder
    ) = WssSubstrateSource(rxWebSocket, signer, keypairFactory, sS58Encoder)

    @Provides
    @FeatureScope
    fun provideJsonMapper() = Gson()

    @Provides
    @FeatureScope
    fun provideWalletRepository(
        substrateSource: WssSubstrateSource,
        accountRepository: AccountRepository,
        assetDao: AssetDao,
        transactionDao: TransactionDao,
        subscanNetworkApi: SubscanNetworkApi
    ): WalletRepository = WalletRepositoryImpl(substrateSource, accountRepository, assetDao, transactionDao, subscanNetworkApi)

    @Provides
    @FeatureScope
    fun provideWalletInteractor(
        walletRepository: WalletRepository,
        accountRepository: AccountRepository
    ): WalletInteractor = WalletInteractorImpl(walletRepository, accountRepository)
}