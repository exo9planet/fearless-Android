package jp.co.soramitsu.common.di.modules

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import jp.co.soramitsu.common.BuildConfig
import jp.co.soramitsu.common.data.network.AndroidLogger
import jp.co.soramitsu.common.data.network.AppLinksProvider
import jp.co.soramitsu.common.data.network.NetworkApiCreator
import jp.co.soramitsu.common.data.network.RxCallAdapterFactory
import jp.co.soramitsu.common.data.network.rpc.RxWebSocketCreator
import jp.co.soramitsu.common.data.network.rpc.SocketSingleRequestExecutor
import jp.co.soramitsu.common.di.scope.ApplicationScope
import jp.co.soramitsu.common.resources.ResourceManager
import jp.co.soramitsu.fearless_utils.wsrpc.Logger
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Named

@Module
class NetworkModule {

    @Provides
    @ApplicationScope
    @Named("TERMS_URL")
    fun provideTermsUrl(): String {
        return BuildConfig.TERMS_URL
    }

    @Provides
    @ApplicationScope
    @Named("PRIVACY_URL")
    fun providePrivacyUrl(): String {
        return BuildConfig.PRIVACY_URL
    }

    @Provides
    @ApplicationScope
    fun provideAppLinksProvider(
        @Named("TERMS_URL") termsUrl: String,
        @Named("PRIVACY_URL") privacyUrl: String
    ): AppLinksProvider {
        return AppLinksProvider(termsUrl, privacyUrl)
    }

    @Provides
    @ApplicationScope
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        }

        return builder.build()
    }

    @Provides
    @ApplicationScope
    fun provideRxCallAdapterFactory(resourceManager: ResourceManager): RxCallAdapterFactory {
        return RxCallAdapterFactory(resourceManager)
    }

    @Provides
    @ApplicationScope
    fun provideLogger(): Logger = AndroidLogger()

    @Provides
    @ApplicationScope
    fun provideApiCreator(
        okHttpClient: OkHttpClient,
        rxCallAdapterFactory: RxCallAdapterFactory
    ): NetworkApiCreator {
        return NetworkApiCreator(okHttpClient, "https://placeholder.com", rxCallAdapterFactory)
    }

    @Provides
    @ApplicationScope
    fun provideSingleRequestExecutor(mapper: Gson, logger: Logger, resourceManager: ResourceManager) = SocketSingleRequestExecutor(mapper, logger, resourceManager)

    @Provides
    @ApplicationScope
    fun provideRxWebSocketCreator(mapper: Gson, logger: Logger, resourceManager: ResourceManager) = RxWebSocketCreator(mapper, logger, resourceManager)

    @Provides
    @ApplicationScope
    fun provideJsonMapper() = Gson()
}