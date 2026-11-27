package com.deepkosh.goldpulse.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.deepkosh.goldpulse.GoldPulseApp
import com.deepkosh.goldpulse.R
import com.deepkosh.goldpulse.config.GoldConfig
import com.deepkosh.goldpulse.data.AppSettings
import com.deepkosh.goldpulse.data.GoldRateRepository
import com.deepkosh.goldpulse.data.GoldRates
import com.deepkosh.goldpulse.notifications.DailyRateScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.concurrent.TimeUnit

enum class UserMessage(@param:StringRes val text: Int, @param:StringRes val action: Int) {
    REFRESH_FAILED(R.string.refresh_failed, R.string.action_retry),
    NOTIFICATIONS_DENIED(R.string.notifications_denied, R.string.action_open_settings),
}

data class HomeUiState(
    val rates: GoldRates? = null,
    val isRefreshing: Boolean = true,
    /** The last refresh failed, so [rates] is the saved copy. */
    val showingSavedRate: Boolean = false,
    val dailyNotificationEnabled: Boolean = false,
    val message: UserMessage? = null,
)

class MainViewModel(
    private val repository: GoldRateRepository,
    private val scheduler: DailyRateScheduler,
    private val settings: AppSettings,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(dailyNotificationEnabled = scheduler.isEnabled))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.savedRates()?.let { saved -> _uiState.update { it.copy(rates = saved) } }
            fetch(userInitiated = false)
        }
    }

    fun refresh() = startRefresh(userInitiated = true)

    fun refreshIfStale() {
        val rates = _uiState.value.rates ?: return
        val age = System.currentTimeMillis() - rates.live.fetchedAtMillis
        if (age > TimeUnit.MINUTES.toMillis(GoldConfig.STALE_AFTER_MINUTES)) {
            startRefresh(userInitiated = false)
        }
    }

    private fun startRefresh(userInitiated: Boolean) {
        if (_uiState.value.isRefreshing) return
        _uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch { fetch(userInitiated) }
    }

    private suspend fun fetch(userInitiated: Boolean) {
        try {
            val fresh = repository.refresh()
            _uiState.update { it.copy(rates = fresh, isRefreshing = false, showingSavedRate = false) }
        } catch (_: IOException) {
            _uiState.update {
                val hasSaved = it.rates != null
                it.copy(
                    isRefreshing = false,
                    showingSavedRate = hasSaved,
                    message = if (hasSaved && userInitiated) UserMessage.REFRESH_FAILED else it.message,
                )
            }
        }
    }

    /**
     * On Android 13+ the daily notification is on by default, so ask for permission once on
     * first launch. Returns true (and records the prompt) if the caller should ask now.
     */
    fun takeFirstLaunchPermissionPrompt(hasPermission: Boolean): Boolean {
        if (hasPermission || !scheduler.isEnabled || settings.notificationPermissionAsked) return false
        settings.notificationPermissionAsked = true
        return true
    }

    fun onNotificationPermissionResult(granted: Boolean, fromToggle: Boolean) {
        settings.notificationPermissionAsked = true
        setDailyNotification(granted)
        if (!granted && fromToggle) {
            _uiState.update { it.copy(message = UserMessage.NOTIFICATIONS_DENIED) }
        }
    }

    fun setDailyNotification(enabled: Boolean) {
        scheduler.setEnabled(enabled)
        _uiState.update { it.copy(dailyNotificationEnabled = enabled) }
    }

    fun messageShown() = _uiState.update { it.copy(message = null) }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as GoldPulseApp
                MainViewModel(app.repository, app.scheduler, app.settings)
            }
        }
    }
}
