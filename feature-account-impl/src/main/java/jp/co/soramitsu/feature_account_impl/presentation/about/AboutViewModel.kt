package jp.co.soramitsu.feature_account_impl.presentation.about

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import jp.co.soramitsu.common.BuildConfig
import jp.co.soramitsu.common.base.BaseViewModel
import jp.co.soramitsu.common.data.network.AppLinksProvider
import jp.co.soramitsu.common.mixin.api.Browserable
import jp.co.soramitsu.common.resources.ResourceManager
import jp.co.soramitsu.common.utils.Event
import jp.co.soramitsu.feature_account_impl.R
import jp.co.soramitsu.feature_account_impl.presentation.AccountRouter

class AboutViewModel(
    private val router: AccountRouter,
    context: Context,
    private val appLinksProvider: AppLinksProvider,
    resourceManager: ResourceManager
) : BaseViewModel(), Browserable {

    private val _websiteLiveData = MutableLiveData<String>()
    val websiteLiveData: LiveData<String> = _websiteLiveData

    private val _twitterLiveData = MutableLiveData<String>()
    val twitterLiveData: LiveData<String> = _twitterLiveData

    private val _youtubeLiveData = MutableLiveData<String>()
    val youtubeLiveData: LiveData<String> = _youtubeLiveData

    private val _mediumLiveData = MutableLiveData<String>()
    val mediumLiveData: LiveData<String> = _mediumLiveData

    private val _versionLiveData = MutableLiveData<String>()
    val versionLiveData: LiveData<String> = _versionLiveData

    private val _telegramLiveData = MutableLiveData<String>()
    val telegramLiveData: LiveData<String> = _telegramLiveData

    private val _announcementLiveData = MutableLiveData<String>()
    val announcementLiveData: LiveData<String> = _announcementLiveData

    private val _supportLiveData = MutableLiveData<String>()
    val supportLiveData: LiveData<String> = _supportLiveData

    private val _emailLiveData = MutableLiveData<String>()
    val emailLiveData: LiveData<String> = _emailLiveData

    private val _openSendEmailEvent = MutableLiveData<Event<String>>()
    val openSendEmailEvent: LiveData<Event<String>> = _openSendEmailEvent

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    init {
        _websiteLiveData.value = BuildConfig.WEBSITE_URL
        _twitterLiveData.value = BuildConfig.TWITTER_URL
        _youtubeLiveData.value = BuildConfig.YOUTUBE_URL
        _mediumLiveData.value = BuildConfig.MEDIUM_URL

        val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        _versionLiveData.value = "${resourceManager.getString(R.string.about_version)} $versionName"

        _telegramLiveData.value = BuildConfig.TELEGRAM_URL
        _announcementLiveData.value = BuildConfig.ANNOUNCEMENT_URL
        _supportLiveData.value = BuildConfig.SUPPORT_URL
        _emailLiveData.value = BuildConfig.EMAIL
    }

    fun backButtonPressed() {
        router.backToProfileScreen()
    }

    fun websiteClicked() {
        openBrowserEvent.value = Event(BuildConfig.WEBSITE_URL)
    }

    fun twitterClicked() {
        openBrowserEvent.value = Event(BuildConfig.TWITTER_URL)
    }

    fun youtubeClicked() {
        openBrowserEvent.value = Event(BuildConfig.YOUTUBE_URL)
    }

    fun mediumClicked() {
        openBrowserEvent.value = Event(BuildConfig.MEDIUM_URL)
    }

    fun githubClicked() {
        openBrowserEvent.value = Event(BuildConfig.GITHUB_URL)
    }

    fun telegramClicked() {
        openBrowserEvent.value = Event(BuildConfig.TELEGRAM_URL)
    }

    fun announcementClicked() {
        openBrowserEvent.value = Event(BuildConfig.ANNOUNCEMENT_URL)
    }

    fun supportClicked() {
        openBrowserEvent.value = Event(BuildConfig.SUPPORT_URL)
    }

    fun emailClicked() {
        _openSendEmailEvent.value = Event(BuildConfig.EMAIL)
    }

    fun termsClicked() {
        openBrowserEvent.value = Event(appLinksProvider.termsUrl)
    }

    fun privacyClicked() {
        openBrowserEvent.value = Event(appLinksProvider.privacyUrl)
    }
}
