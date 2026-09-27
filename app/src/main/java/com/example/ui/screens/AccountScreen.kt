package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.auth.GoogleFirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(
    activity: Activity,
    auth: GoogleFirebaseAuth,
    onContinue: () -> Unit
) {
    var webClientId by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val user = auth.currentUser

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("DroidCommand account", style = MaterialTheme.typography.headlineSmall)
        Text("Sign in to use authenticated support-session features. Sign-in does not grant device-control permissions.")
        if (user != null) {
            Text("Signed in: ${user.email ?: user.uid}")
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
            OutlinedButton(onClick = { auth.signOut(); status = "Signed out." }, modifier = Modifier.fillMaxWidth()) { Text("Sign out") }
        } else {
            OutlinedTextField(
                value = webClientId,
                onValueChange = { webClientId = it },
                label = { Text("Firebase Web OAuth client ID") },
                placeholder = { Text("...apps.googleusercontent.com") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                enabled = !busy && webClientId.isNotBlank(),
                onClick = {
                    busy = true
                    status = "Opening Google sign-in…"
                    scope.launch {
                        auth.signIn(activity, webClientId.trim())
                            .onSuccess { status = "Signed in."; onContinue() }
                            .onFailure { status = it.message ?: "Sign-in failed." }
                        busy = false
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (busy) "Signing in…" else "Continue with Google") }
        }
        if (status.isNotBlank()) Text(status, color = MaterialTheme.colorScheme.secondary)
    }
}
