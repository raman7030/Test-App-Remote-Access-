package com.example.auth

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Authenticated session-directory operations. This stores coordination metadata only;
 * it is not a signaling channel and never transports screen frames or input commands.
 */
class FirebaseSupportSessions(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    data class Session(val id: String, val ownerUid: String, val operatorUid: String?, val status: String)

    suspend fun createSession(ttlMinutes: Long = 15): String {
        require(ttlMinutes in 1..60) { "Session lifetime must be 1–60 minutes." }
        val owner = auth.currentUser ?: error("Sign in before creating a support session.")
        val id = UUID.randomUUID().toString()
        val expires = Timestamp((System.currentTimeMillis() / 1000L) + ttlMinutes * 60L, 0)
        db.collection("sessions").document(id).set(
            mapOf(
                "ownerUid" to owner.uid,
                "operatorUid" to null,
                "status" to "pending",
                "createdAt" to FieldValue.serverTimestamp(),
                "expiresAt" to expires
            )
        ).await()
        return id
    }

    suspend fun joinSession(sessionId: String): Session {
        require(sessionId.matches(Regex("[A-Za-z0-9-]{16,64}"))) { "Invalid session code." }
        val operator = auth.currentUser ?: error("Sign in before joining a support session.")
        val ref = db.collection("sessions").document(sessionId)
        db.runTransaction { tx ->
            val snap = tx.get(ref)
            check(snap.exists()) { "Session not found." }
            check(snap.getString("status") == "pending") { "Session is not available." }
            check(snap.getTimestamp("expiresAt")?.toDate()?.time?.let { it > System.currentTimeMillis() } == true) {
                "Session has expired."
            }
            val ownerUid = snap.getString("ownerUid") ?: error("Invalid session owner.")
            check(ownerUid != operator.uid) { "The device owner cannot join as the operator." }
            check(snap.getString("operatorUid").isNullOrBlank()) { "Session already has an operator." }
            tx.update(ref, mapOf("operatorUid" to operator.uid, "status" to "awaiting_consent",
                "updatedAt" to FieldValue.serverTimestamp()))
            Session(sessionId, ownerUid, operator.uid, "awaiting_consent")
        }.await()
    }

    suspend fun setConsent(sessionId: String, granted: Boolean) {
        val owner = auth.currentUser ?: error("Sign in first.")
        val ref = db.collection("sessions").document(sessionId)
        db.runTransaction { tx ->
            val snap = tx.get(ref)
            check(snap.exists() && snap.getString("ownerUid") == owner.uid) {
                "Only the device owner can change consent."
            }
            check(snap.getString("status") == "awaiting_consent" || !granted) {
                "No operator is waiting for consent."
            }
            tx.update(ref, mapOf(
                "status" to if (granted) "consented" else "denied",
                "consentGrantedAt" to if (granted) FieldValue.serverTimestamp() else null,
                "updatedAt" to FieldValue.serverTimestamp()
            ))
                    true
        }.await()
    }

    suspend fun endSession(sessionId: String) {
        val uid = auth.currentUser?.uid ?: error("Sign in first.")
        val ref = db.collection("sessions").document(sessionId)
        db.runTransaction { tx ->
            val snap = tx.get(ref)
            check(snap.exists() && (snap.getString("ownerUid") == uid || snap.getString("operatorUid") == uid)) {
                "Only a session participant can end this session."
            }
            tx.update(ref, mapOf("status" to "ended", "endedAt" to FieldValue.serverTimestamp()))
        }.await()
    }
}
