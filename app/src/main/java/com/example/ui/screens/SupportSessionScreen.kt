package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.auth.FirebaseSupportSessions
import kotlinx.coroutines.launch

@Composable
fun SupportSessionScreen(onBack: () -> Unit) {
    val sessions = remember { FirebaseSupportSessions() }
    var joinCode by remember { mutableStateOf("") }
    var activeCode by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun runTask(block: suspend () -> Unit) {
        scope.launch {
            busy = true
            try { block() } catch (e: Exception) { status = e.message ?: "Request failed." }
            finally { busy = false }
        }
    }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Consent-based support session", style = MaterialTheme.typography.headlineSmall)
        Text("Create a short-lived pairing code on the device, or join a code supplied by its owner. The device owner must separately approve assistance.")
        Button(enabled = !busy, onClick = {
            runTask {
                activeCode = sessions.createSession()
                status = "Pairing code created. Share it only with the intended support operator."
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Create device session (15 min)") }
        if (activeCode.isNotBlank()) {
            Text("Session code: $activeCode", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { activeCode = ""; status = "Code hidden locally; the session remains pending until it expires or is ended." }, modifier = Modifier.fillMaxWidth()) { Text("Hide code") }
            Button(enabled = !busy, onClick = { runTask { sessions.setConsent(activeCode, true); status = "Consent recorded. Screen capture still requires Android's system prompt." } }, modifier = Modifier.fillMaxWidth()) { Text("Approve waiting operator") }
            OutlinedButton(enabled = !busy, onClick = { runTask { sessions.setConsent(activeCode, false); status = "Request denied." } }, modifier = Modifier.fillMaxWidth()) { Text("Deny request") }
            OutlinedButton(enabled = !busy, onClick = { runTask { sessions.endSession(activeCode); activeCode = ""; status = "Session ended." } }, modifier = Modifier.fillMaxWidth()) { Text("End session") }
        }
        Divider()
        OutlinedTextField(value = joinCode, onValueChange = { joinCode = it.trim().take(64) }, label = { Text("Owner's pairing code") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(enabled = !busy && joinCode.isNotBlank(), onClick = {
            runTask { val s = sessions.joinSession(joinCode); activeCode = s.id; status = "Join request sent. Waiting for device-owner consent." }
        }, modifier = Modifier.fillMaxWidth()) { Text("Request to join as operator") }
        if (status.isNotBlank()) Text(status, color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.weight(1f))
        Text("This screen manages session metadata only. Live screen streaming and remote input transport are not yet implemented.")
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}
