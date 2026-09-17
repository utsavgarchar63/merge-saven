package com.mergeseven.game.cloud

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class CloudAccount(
    val playerId: String,
    val displayName: String?
)

interface PlayGamesAuth {
    val account: StateFlow<CloudAccount?>
    val isSignedIn: Boolean
        get() = account.value != null

    suspend fun signInSilently(): Boolean
    fun getSignInIntent(): Intent
    suspend fun handleSignInResult(data: Intent?): Boolean
    suspend fun signOut()
    fun lastAccount(): GoogleSignInAccount?
}

@Singleton
class PlayGamesAuthImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PlayGamesAuth {

    private val _account = MutableStateFlow<CloudAccount?>(null)
    override val account: StateFlow<CloudAccount?> = _account.asStateFlow()

    private val client: GoogleSignInClient by lazy {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN).build()
        GoogleSignIn.getClient(context, options)
    }

    init {
        refreshFromLastSignedIn()
    }

    override suspend fun signInSilently(): Boolean {
        return try {
            val result = client.silentSignIn().await()
            setFrom(result)
            true
        } catch (e: Exception) {
            Log.d(TAG, "Silent sign-in unavailable: ${e.message}")
            _account.value = null
            false
        }
    }

    override fun getSignInIntent(): Intent = client.signInIntent

    override suspend fun handleSignInResult(data: Intent?): Boolean {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            setFrom(account)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Interactive sign-in failed: ${e.message}")
            false
        }
    }

    override suspend fun signOut() {
        runCatching { client.signOut().await() }
        _account.value = null
    }

    override fun lastAccount(): GoogleSignInAccount? =
        GoogleSignIn.getLastSignedInAccount(context)

    private fun refreshFromLastSignedIn() {
        val last = GoogleSignIn.getLastSignedInAccount(context) ?: return
        setFrom(last)
    }

    private fun setFrom(account: GoogleSignInAccount?) {
        if (account == null) {
            _account.value = null
            return
        }
        val id = account.id ?: account.email ?: "play_games_user"
        _account.value = CloudAccount(
            playerId = id,
            displayName = account.displayName
        )
    }

    private companion object {
        const val TAG = "PlayGamesAuth"
    }
}

/** JVM / unit-test fake. */
class FakePlayGamesAuth(
    initiallySignedIn: CloudAccount? = null
) : PlayGamesAuth {
    private val _account = MutableStateFlow(initiallySignedIn)
    override val account: StateFlow<CloudAccount?> = _account.asStateFlow()

    var silentSuccess: Boolean = initiallySignedIn != null
    var interactiveSuccess: Boolean = true
    var signInSilentCalls: Int = 0
    var signOutCalls: Int = 0

    override suspend fun signInSilently(): Boolean {
        signInSilentCalls++
        if (!silentSuccess) {
            _account.value = null
            return false
        }
        if (_account.value == null) {
            _account.value = CloudAccount("fake_player", "Fake")
        }
        return true
    }

    override fun getSignInIntent(): Intent = Intent()

    override suspend fun handleSignInResult(data: Intent?): Boolean {
        if (!interactiveSuccess) return false
        _account.value = CloudAccount("fake_player", "Fake")
        return true
    }

    override suspend fun signOut() {
        signOutCalls++
        _account.value = null
    }

    override fun lastAccount(): GoogleSignInAccount? = null

    fun setSignedIn(account: CloudAccount?) {
        _account.value = account
    }
}
