package com.example.auth

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

/**
 * Google identity sign-in backed by Firebase Authentication.
 *
 * Pass the Web OAuth client ID from the Firebase project's google-services.json
 * configuration (default_web_client_id). Never place a client secret in the APK.
 */
class GoogleFirebaseAuth(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val currentUser get() = auth.currentUser

    suspend fun signIn(activity: Activity, webClientId: String): Result<String> =
        runCatching {
            require(webClientId.isNotBlank()) { "Configure the Firebase Web OAuth client ID first." }
            val credentialManager = CredentialManager.create(activity)
            val googleOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build()
            val result = credentialManager.getCredential(activity, request)
            val googleCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(
                googleCredential.idToken,
                null
            )
            val user = auth.signInWithCredential(firebaseCredential).await().user
                ?: error("Firebase did not return an authenticated user.")
            user.uid
        }

    fun signOut() {
        auth.signOut()
    }
}
