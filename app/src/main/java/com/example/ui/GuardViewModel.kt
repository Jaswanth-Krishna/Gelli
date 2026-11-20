package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GuardPrefs
import com.example.data.PairingResult
import com.example.data.SafetyRepository
import com.example.service.DeviceSetupStatus
import com.example.service.DeviceStatusHelper
import com.example.service.ProtectionRuntimeState
import com.example.service.SafetyForegroundService
import com.example.service.ServiceStateBus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BannerTone { INFO, SUCCESS, ERROR }

data class FeedbackBanner(
    val message: String,
    val tone: BannerTone
)

data class GuardUiState(
    val codeInput: String = "",
    val pairedCode: String? = null,
    val isVerifying: Boolean = false,
    val isSendingSos: Boolean = false,
    val feedbackBanner: FeedbackBanner? = null,
    val setupStatus: DeviceSetupStatus = DeviceSetupStatus(),
    val runtimeState: ProtectionRuntimeState = ProtectionRuntimeState()
)

sealed interface GuardUiEvent {
    data class RequestPermissionsAndStart(val code: String) : GuardUiEvent
    data class ShowSnackbar(val message: String) : GuardUiEvent
}

class GuardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SafetyRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(
        GuardUiState(
            codeInput = GuardPrefs.getPairingCode(application.applicationContext).orEmpty(),
            pairedCode = GuardPrefs.getPairingCode(application.applicationContext),
            setupStatus = DeviceStatusHelper.readSetupStatus(application.applicationContext),
            runtimeState = ServiceStateBus.state.value
        )
    )
    val uiState: StateFlow<GuardUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<GuardUiEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<GuardUiEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            ServiceStateBus.state.collect { runtime ->
                _uiState.update { it.copy(runtimeState = runtime) }
            }
        }
    }

    fun refreshDeviceStatus() {
        val context = getApplication<Application>().applicationContext
        val status = DeviceStatusHelper.readSetupStatus(context)
        val savedCode = GuardPrefs.getPairingCode(context)
        _uiState.update {
            it.copy(
                setupStatus = status,
                pairedCode = savedCode
            )
        }
    }

    fun onCodeInputChanged(raw: String) {
        val filtered = raw.filter(Char::isDigit).take(6)
        _uiState.update {
            it.copy(
                codeInput = filtered,
                feedbackBanner = if (it.feedbackBanner?.tone == BannerTone.ERROR) null else it.feedbackBanner
            )
        }
    }

    fun verifyCodeAndStartProtection() {
        val currentCode = _uiState.value.codeInput.trim()
        if (currentCode.length != 6) {
            _uiState.update {
                it.copy(
                    feedbackBanner = FeedbackBanner(
                        message = "Enter all 6 digits from your parent app before verifying.",
                        tone = BannerTone.ERROR
                    )
                )
            }
            return
        }

        if (_uiState.value.isVerifying) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isVerifying = true,
                    feedbackBanner = FeedbackBanner(
                        message = "Verifying pairing code $currentCode with FlashGet Guard…",
                        tone = BannerTone.INFO
                    )
                )
            }

            when (val result = repository.verifyAndPairCode(currentCode)) {
                is PairingResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isVerifying = false,
                            pairedCode = result.code,
                            feedbackBanner = FeedbackBanner(
                                message = "Paired with code ${result.code}. Requesting safety permissions & starting protection…",
                                tone = BannerTone.SUCCESS
                            )
                        )
                    }
                    _events.tryEmit(GuardUiEvent.RequestPermissionsAndStart(result.code))
                }

                is PairingResult.InvalidCode -> {
                    _uiState.update {
                        it.copy(
                            isVerifying = false,
                            feedbackBanner = FeedbackBanner(
                                message = result.message,
                                tone = BannerTone.ERROR
                            )
                        )
                    }
                }

                is PairingResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isVerifying = false,
                            feedbackBanner = FeedbackBanner(
                                message = result.message,
                                tone = BannerTone.ERROR
                            )
                        )
                    }
                }
            }
        }
    }

    fun onPermissionsResult(code: String, results: Map<String, Boolean>) {
        val context = getApplication<Application>().applicationContext
        refreshDeviceStatus()

        val hasLocation = DeviceStatusHelper.hasLocationPermission(context)
        val grantedCount = results.values.count { it }

        if (hasLocation || results.values.any { it }) {
            SafetyForegroundService.start(context, code)
            _uiState.update {
                it.copy(
                    feedbackBanner = FeedbackBanner(
                        message = "Protection active for code $code ($grantedCount/${results.size} permissions granted).",
                        tone = BannerTone.SUCCESS
                    )
                )
            }
            _events.tryEmit(GuardUiEvent.ShowSnackbar("SafetyForegroundService started"))
        } else {
            _uiState.update {
                it.copy(
                    feedbackBanner = FeedbackBanner(
                        message = "Location permission is required to run background GPS safety telemetry.",
                        tone = BannerTone.ERROR
                    )
                )
            }
        }
    }

    fun sendEmergencySos() {
        val code = _uiState.value.pairedCode ?: GuardPrefs.getPairingCode(getApplication())
        if (code.isNullOrBlank()) {
            val msg = "Pair this device with a 6-digit code first to send SOS alerts."
            _uiState.update {
                it.copy(feedbackBanner = FeedbackBanner(msg, BannerTone.ERROR))
            }
            _events.tryEmit(GuardUiEvent.ShowSnackbar(msg))
            return
        }

        if (_uiState.value.isSendingSos) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingSos = true) }
            val result = repository.sendSosAlert(code)
            result.fold(
                onSuccess = {
                    val msg = "Emergency SOS alert sent to parent dashboard!"
                    _uiState.update {
                        it.copy(
                            isSendingSos = false,
                            feedbackBanner = FeedbackBanner(msg, BannerTone.SUCCESS)
                        )
                    }
                    _events.tryEmit(GuardUiEvent.ShowSnackbar(msg))
                },
                onFailure = { error ->
                    val msg = error.message ?: "Unable to send SOS alert. Check internet connection."
                    _uiState.update {
                        it.copy(
                            isSendingSos = false,
                            feedbackBanner = FeedbackBanner(msg, BannerTone.ERROR)
                        )
                    }
                    _events.tryEmit(GuardUiEvent.ShowSnackbar(msg))
                }
            )
        }
    }
}
