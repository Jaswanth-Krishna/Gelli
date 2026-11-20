package com.example.data

import android.content.Context
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

sealed interface PairingResult {
    data class Success(val code: String) : PairingResult
    data class InvalidCode(val message: String) : PairingResult
    data class Error(val message: String) : PairingResult
}

class SafetyRepository(private val context: Context) {

    suspend fun verifyAndPairCode(code: String): PairingResult {
        val cleanCode = code.trim()
        if (cleanCode.length != 6 || !cleanCode.all(Char::isDigit)) {
            return PairingResult.InvalidCode("Please enter a valid 6-digit numeric pairing code.")
        }

        return suspendCancellableCoroutine { continuation ->
            val ref = FirebaseRtdbProvider.codeRef(context, cleanCode)
            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!continuation.isActive) return
                    if (!snapshot.exists()) {
                        continuation.resume(
                            PairingResult.InvalidCode(
                                "Pairing code $cleanCode was not found. Check the parent app and try again."
                            )
                        )
                        return
                    }

                    val updates = mapOf<String, Any>(
                        "paired" to true,
                        "pairedAt" to ServerValue.TIMESTAMP
                    )
                    ref.updateChildren(updates) { error, _ ->
                        if (!continuation.isActive) return@updateChildren
                        if (error != null) {
                            continuation.resume(
                                PairingResult.Error(
                                    error.message.ifBlank { "Unable to update pairing status." }
                                )
                            )
                        } else {
                            GuardPrefs.savePairingCode(context, cleanCode)
                            continuation.resume(PairingResult.Success(cleanCode))
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    if (!continuation.isActive) return
                    continuation.resume(
                        PairingResult.Error(
                            error.message.ifBlank { "Failed to connect to safety server." }
                        )
                    )
                }
            })
        }
    }

    suspend fun sendSosAlert(code: String): Result<Unit> {
        val cleanCode = code.trim()
        if (cleanCode.length != 6) {
            return Result.failure(IllegalStateException("Device must be paired before sending an SOS alert."))
        }

        return suspendCancellableCoroutine { continuation ->
            val alertPayload = mapOf<String, Any>(
                "type" to "SOS_BUTTON",
                "message" to "EMERGENCY: Child pressed SOS!",
                "time" to ServerValue.TIMESTAMP
            )
            FirebaseRtdbProvider.alertsRef(context, cleanCode)
                .push()
                .setValue(alertPayload) { error, _ ->
                    if (!continuation.isActive) return@setValue
                    if (error != null) {
                        continuation.resume(
                            Result.failure(
                                RuntimeException(error.message.ifBlank { "Failed to deliver SOS alert." })
                            )
                        )
                    } else {
                        continuation.resume(Result.success(Unit))
                    }
                }
        }
    }
}
