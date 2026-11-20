package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AccessRequest
import com.example.data.model.AccessRequestStatus
import com.example.data.model.AccessTargetType
import com.example.data.model.AdminRole
import com.example.data.model.Family
import com.example.data.model.Gender
import com.example.data.model.IdMaskingUtils
import com.example.data.model.Member
import com.example.data.model.PrivateMemberDetail
import com.example.data.model.RationCategory
import com.example.data.model.RecordVisibility
import com.example.data.model.RelationToHead
import com.example.data.model.UserProfile
import com.example.data.model.VerificationStatus
import com.example.data.model.VillageConfig
import com.example.localization.AppLanguage
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class VillageRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun <T> Flow<T>.handleSnapshotErrors(
        operationType: OperationType,
        path: String,
        expectedUid: String
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }

    fun observeVillageConfig(): Flow<VillageConfig> = flow {
        val uid = requireUserId()
        val path = "village_config/main"
        emitAll(
            db.collection("village_config").document("main")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            VillageConfig::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ) ?: VillageConfig()
                    } else {
                        VillageConfig()
                    }
                }
                .handleSnapshotErrors(OperationType.GET, path, uid)
        )
    }

    fun observeIsAdmin(uid: String, email: String?, emailVerified: Boolean): Flow<Boolean> = flow {
        if (email == BOOTSTRAPPED_ADMIN_EMAIL && emailVerified) {
            emit(true)
            return@flow
        }
        val path = "admins/$uid"
        emitAll(
            db.collection("admins").document(uid)
                .snapshots()
                .map { it.exists() }
                .handleSnapshotErrors(OperationType.GET, path, uid)
        )
    }

    fun observeAdminsList(): Flow<List<AdminRole>> = flow {
        val uid = requireUserId()
        val path = "admins"
        emitAll(
            db.collection("admins")
                .snapshots()
                .map { snap ->
                    snap.toObjects(AdminRole::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .handleSnapshotErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeUserProfile(uid: String): Flow<UserProfile?> = flow {
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .handleSnapshotErrors(OperationType.GET, path, uid)
        )
    }

    fun observePublicMembers(): Flow<List<Member>> = flow {
        val uid = requireUserId()
        val path = "members[visibility=PUBLIC]"
        emitAll(
            db.collection("members")
                .whereEqualTo("visibility", RecordVisibility.PUBLIC.name)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Member::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .handleSnapshotErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeMyMembers(uid: String): Flow<List<Member>> = flow {
        val path = "members[ownerUid=$uid]"
        emitAll(
            db.collection("members")
                .whereEqualTo("ownerUid", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Member::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .handleSnapshotErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeSharedMembers(uid: String): Flow<List<Member>> = flow {
        val path = "members[authorizedViewerUids=$uid]"
        emitAll(
            db.collection("members")
                .whereArrayContains("authorizedViewerUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Member::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .handleSnapshotErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeAllMembersAdmin(): Flow<List<Member>> = flow {
        val uid = requireUserId()
        val path = "members[admin]"
        emitAll(
            db.collection("members")
                .snapshots()
                .map { snap ->
                    snap.toObjects(Member::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .handleSnapshotErrors(OperationType.LIST, path, uid)
        )
    }

    fun observePublicFamilies(): Flow<List<Family>> = flow {
        val uid = requireUserId()
        val path = "families[visibility=PUBLIC]"
        emitAll(
            db.collection("families")
                .whereEqualTo("visibility", RecordVisibility.PUBLIC.name)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Family::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .handleSnapshotErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeMyFamilies(uid: String): Flow<List<Family>> = flow {
        val path = "families[ownerUid=$uid]"
        emitAll(
            db.collection("families")
                .whereEqualTo("ownerUid", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Family::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .handleSnapshotErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeSharedFamilies(uid: String): Flow<List<Family>> = flow {
        val path = "families[authorizedViewerUids=$uid]"
        emitAll(
            db.collection("families")
                .whereArrayContains("authorizedViewerUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Family::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .handleSnapshotErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeAllFamiliesAdmin(): Flow<List<Family>> = flow {
        val uid = requireUserId()
        val path = "families[admin]"
        emitAll(
            db.collection("families")
                .snapshots()
                .map { snap ->
                    snap.toObjects(Family::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .handleSnapshotErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeIncomingRequests(uid: String): Flow<List<AccessRequest>> = flow {
        val path = "access_requests[targetOwnerUid=$uid]"
        emitAll(
            db.collection("access_requests")
                .whereEqualTo("targetOwnerUid", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(AccessRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .handleSnapshotErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeOutgoingRequests(uid: String): Flow<List<AccessRequest>> = flow {
        val path = "access_requests[requesterUid=$uid]"
        emitAll(
            db.collection("access_requests")
                .whereEqualTo("requesterUid", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(AccessRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .handleSnapshotErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeAllRequestsAdmin(): Flow<List<AccessRequest>> = flow {
        val uid = requireUserId()
        val path = "access_requests[admin]"
        emitAll(
            db.collection("access_requests")
                .snapshots()
                .map { snap ->
                    snap.toObjects(AccessRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .handleSnapshotErrors(OperationType.LIST, path, uid)
        )
    }

    suspend fun ensureUserProfile(user: FirebaseUser, preferredLanguage: AppLanguage): Result<Unit> {
        val docPath = "users/${user.uid}"
        return try {
            val uid = requireUserId()
            require(uid == user.uid)
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            val safeName = (user.displayName?.trim()?.take(100)).orEmpty().ifBlank { "Villager" }
            val safeEmail = (user.email?.trim()?.take(200)).orEmpty().ifBlank { "villager@myvillage.local" }
            if (!existing.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to safeEmail,
                    "preferredLanguage" to preferredLanguage.name,
                    "ward" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                docRef.update(
                    mapOf(
                        "displayName" to safeName,
                        "email" to safeEmail,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateUserLanguage(language: AppLanguage): Result<Unit> {
        var docPath = "users/unknown"
        return try {
            val uid = requireUserId()
            docPath = "users/$uid"
            db.collection("users").document(uid).update(
                mapOf(
                    "preferredLanguage" to language.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveVillageConfig(
        villageNameHi: String,
        villageNameOr: String,
        villageNameEn: String,
        district: String,
        stateName: String,
        wards: List<String>
    ): Result<Unit> {
        val docPath = "village_config/main"
        return try {
            val uid = requireUserId()
            val hi = villageNameHi.trim().take(100)
            val or = villageNameOr.trim().take(100)
            val en = villageNameEn.trim().take(100)
            require(hi.isNotEmpty() && or.isNotEmpty() && en.isNotEmpty()) {
                "Village names cannot be blank"
            }
            val cleanWards = wards.map { it.trim().take(80) }.filter { it.isNotEmpty() }.take(30)
            val docRef = db.collection("village_config").document("main")
            val existing = docRef.get().await()
            if (!existing.exists()) {
                docRef.set(
                    mapOf(
                        "villageNameHi" to hi,
                        "villageNameOr" to or,
                        "villageNameEn" to en,
                        "district" to district.trim().take(100),
                        "stateName" to stateName.trim().take(100),
                        "wards" to cleanWards,
                        "updatedByUid" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "villageNameHi" to hi,
                        "villageNameOr" to or,
                        "villageNameEn" to en,
                        "district" to district.trim().take(100),
                        "stateName" to stateName.trim().take(100),
                        "wards" to cleanWards,
                        "updatedByUid" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun registerFamily(
        familyCode: String,
        headName: String,
        headNameLocal: String,
        ward: String,
        houseNumber: String,
        rationCategory: RationCategory,
        memberCount: Long,
        visibility: RecordVisibility,
        isAdmin: Boolean
    ): Result<String> {
        var docPath = "families"
        return try {
            val uid = requireUserId()
            val code = familyCode.trim().uppercase().take(30)
            val head = headName.trim().take(100)
            val cleanWard = ward.trim().take(80)
            require(code.length >= 2) { "Family code must be at least 2 characters" }
            require(head.length >= 2) { "Head name must be at least 2 characters" }
            require(cleanWard.isNotEmpty()) { "Ward is required" }
            val count = memberCount.coerceIn(1L, 50L)

            val docRef = db.collection("families").document()
            docPath = docRef.path
            val payload = mapOf(
                "ownerUid" to uid,
                "familyCode" to code,
                "headName" to head,
                "headNameLocal" to headNameLocal.trim().take(100),
                "ward" to cleanWard,
                "houseNumber" to houseNumber.trim().take(30),
                "rationCardCategory" to rationCategory.name,
                "memberCount" to count,
                "visibility" to visibility.name,
                "authorizedViewerUids" to emptyList<String>(),
                "verificationStatus" to if (isAdmin) VerificationStatus.VERIFIED.name else VerificationStatus.PENDING.name,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateFamily(
        familyId: String,
        familyCode: String,
        headName: String,
        headNameLocal: String,
        ward: String,
        houseNumber: String,
        rationCategory: RationCategory,
        memberCount: Long,
        visibility: RecordVisibility
    ): Result<Unit> {
        val docPath = "families/$familyId"
        return try {
            requireUserId()
            require(familyId.isNotBlank())
            val code = familyCode.trim().uppercase().take(30)
            val head = headName.trim().take(100)
            val cleanWard = ward.trim().take(80)
            require(code.length >= 2 && head.length >= 2 && cleanWard.isNotEmpty())
            val count = memberCount.coerceIn(1L, 50L)

            db.collection("families").document(familyId).update(
                mapOf(
                    "familyCode" to code,
                    "headName" to head,
                    "headNameLocal" to headNameLocal.trim().take(100),
                    "ward" to cleanWard,
                    "houseNumber" to houseNumber.trim().take(30),
                    "rationCardCategory" to rationCategory.name,
                    "memberCount" to count,
                    "visibility" to visibility.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteFamily(familyId: String): Result<Unit> {
        val docPath = "families/$familyId"
        return try {
            requireUserId()
            db.collection("families").document(familyId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun registerMember(
        familyId: String,
        familyCode: String,
        fullName: String,
        fullNameLocal: String,
        guardianName: String,
        relationToHead: RelationToHead,
        age: Long,
        gender: Gender,
        ward: String,
        occupation: String,
        rawAadhaar: String,
        rawVoterId: String,
        visibility: RecordVisibility,
        phoneNumber: String,
        fullAddress: String,
        emergencyContact: String,
        privateNotes: String,
        isAdmin: Boolean
    ): Result<String> {
        var docPath = "members"
        return try {
            val uid = requireUserId()
            val cleanName = fullName.trim().take(100)
            val cleanGuardian = guardianName.trim().take(100)
            val cleanWard = ward.trim().take(80)
            require(cleanName.length >= 2) { "Full name must be at least 2 characters" }
            require(cleanGuardian.isNotEmpty()) { "Guardian name is required" }
            require(cleanWard.isNotEmpty()) { "Ward is required" }
            val safeAge = age.coerceIn(0L, 120L)

            val (aadhaarMasked, aadhaarLast4) = IdMaskingUtils.maskAadhaar(rawAadhaar)
            val (voterMasked, voterLast4) = IdMaskingUtils.maskVoterId(rawVoterId)

            val memberRef = db.collection("members").document()
            docPath = memberRef.path
            val privateRef = memberRef.collection("private_details").document("info")

            val batch = db.batch()
            batch.set(
                memberRef,
                mapOf(
                    "ownerUid" to uid,
                    "familyId" to familyId.trim().take(128),
                    "familyCode" to familyCode.trim().take(30),
                    "fullName" to cleanName,
                    "fullNameLocal" to fullNameLocal.trim().take(100),
                    "guardianName" to cleanGuardian,
                    "relationToHead" to relationToHead.name,
                    "age" to safeAge,
                    "gender" to gender.name,
                    "ward" to cleanWard,
                    "occupation" to occupation.trim().take(80),
                    "aadhaarMasked" to aadhaarMasked,
                    "aadhaarLast4" to aadhaarLast4,
                    "voterIdMasked" to voterMasked,
                    "voterIdLast4" to voterLast4,
                    "visibility" to visibility.name,
                    "authorizedViewerUids" to emptyList<String>(),
                    "verificationStatus" to if (isAdmin) VerificationStatus.VERIFIED.name else VerificationStatus.PENDING.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                privateRef,
                mapOf(
                    "ownerUid" to uid,
                    "authorizedViewerUids" to emptyList<String>(),
                    "phoneNumber" to phoneNumber.trim().take(20),
                    "fullAddress" to fullAddress.trim().take(250),
                    "emergencyContact" to emergencyContact.trim().take(100),
                    "privateNotes" to privateNotes.trim().take(500),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(memberRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateMember(
        existingMember: Member,
        familyId: String,
        familyCode: String,
        fullName: String,
        fullNameLocal: String,
        guardianName: String,
        relationToHead: RelationToHead,
        age: Long,
        gender: Gender,
        ward: String,
        occupation: String,
        rawAadhaar: String,
        rawVoterId: String,
        visibility: RecordVisibility,
        phoneNumber: String?,
        fullAddress: String?,
        emergencyContact: String?,
        privateNotes: String?
    ): Result<Unit> {
        val docPath = "members/${existingMember.id}"
        return try {
            val uid = requireUserId()
            require(existingMember.id.isNotBlank())
            val cleanName = fullName.trim().take(100)
            val cleanGuardian = guardianName.trim().take(100)
            val cleanWard = ward.trim().take(80)
            require(cleanName.length >= 2 && cleanGuardian.isNotEmpty() && cleanWard.isNotEmpty())
            val safeAge = age.coerceIn(0L, 120L)

            val (aadhaarMasked, aadhaarLast4) = if (rawAadhaar.isNotBlank()) {
                IdMaskingUtils.maskAadhaar(rawAadhaar)
            } else {
                existingMember.aadhaarMasked to existingMember.aadhaarLast4
            }
            val (voterMasked, voterLast4) = if (rawVoterId.isNotBlank()) {
                IdMaskingUtils.maskVoterId(rawVoterId)
            } else {
                existingMember.voterIdMasked to existingMember.voterIdLast4
            }

            val memberRef = db.collection("members").document(existingMember.id)
            val privateRef = memberRef.collection("private_details").document("info")
            val privateExists = try {
                privateRef.get().await().exists()
            } catch (_: Exception) {
                false
            }

            val batch = db.batch()
            batch.update(
                memberRef,
                mapOf(
                    "familyId" to familyId.trim().take(128),
                    "familyCode" to familyCode.trim().take(30),
                    "fullName" to cleanName,
                    "fullNameLocal" to fullNameLocal.trim().take(100),
                    "guardianName" to cleanGuardian,
                    "relationToHead" to relationToHead.name,
                    "age" to safeAge,
                    "gender" to gender.name,
                    "ward" to cleanWard,
                    "occupation" to occupation.trim().take(80),
                    "aadhaarMasked" to aadhaarMasked,
                    "aadhaarLast4" to aadhaarLast4,
                    "voterIdMasked" to voterMasked,
                    "voterIdLast4" to voterLast4,
                    "visibility" to visibility.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            if (phoneNumber != null || fullAddress != null || emergencyContact != null || privateNotes != null) {
                if (privateExists) {
                    batch.update(
                        privateRef,
                        mapOf(
                            "phoneNumber" to (phoneNumber ?: "").trim().take(20),
                            "fullAddress" to (fullAddress ?: "").trim().take(250),
                            "emergencyContact" to (emergencyContact ?: "").trim().take(100),
                            "privateNotes" to (privateNotes ?: "").trim().take(500),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else if (existingMember.ownerUid == uid) {
                    batch.set(
                        privateRef,
                        mapOf(
                            "ownerUid" to uid,
                            "authorizedViewerUids" to existingMember.authorizedViewerUids.take(20),
                            "phoneNumber" to (phoneNumber ?: "").trim().take(20),
                            "fullAddress" to (fullAddress ?: "").trim().take(250),
                            "emergencyContact" to (emergencyContact ?: "").trim().take(100),
                            "privateNotes" to (privateNotes ?: "").trim().take(500),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteMember(memberId: String): Result<Unit> {
        val docPath = "members/$memberId"
        return try {
            requireUserId()
            val memberRef = db.collection("members").document(memberId)
            val privateRef = memberRef.collection("private_details").document("info")
            val privateExists = try {
                privateRef.get().await().exists()
            } catch (_: Exception) {
                false
            }
            val batch = db.batch()
            if (privateExists) {
                batch.delete(privateRef)
            }
            batch.delete(memberRef)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun fetchPrivateMemberDetail(memberId: String): Result<PrivateMemberDetail?> {
        val docPath = "members/$memberId/private_details/info"
        return try {
            requireUserId()
            val snap = db.collection("members").document(memberId)
                .collection("private_details").document("info")
                .get()
                .await()
            val detail = if (snap.exists()) {
                snap.toObject(PrivateMemberDetail::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else null
            Result.success(detail)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docPath)
            Result.failure(e)
        }
    }

    suspend fun requestPrivateAccess(
        requesterName: String,
        targetType: AccessTargetType,
        targetId: String,
        targetName: String,
        targetOwnerUid: String,
        reason: String
    ): Result<Unit> {
        var docPath = "access_requests"
        return try {
            val uid = requireUserId()
            val cleanReason = reason.trim().take(250)
            require(cleanReason.length >= 3) { "Please enter a brief reason (at least 3 characters)" }
            val docRef = db.collection("access_requests").document()
            docPath = docRef.path
            docRef.set(
                mapOf(
                    "requesterUid" to uid,
                    "requesterName" to requesterName.trim().take(100).ifBlank { "Villager" },
                    "targetType" to targetType.name,
                    "targetId" to targetId.trim().take(128),
                    "targetName" to targetName.trim().take(100).ifBlank { "Record" },
                    "targetOwnerUid" to targetOwnerUid.trim().take(128),
                    "reason" to cleanReason,
                    "status" to AccessRequestStatus.PENDING.name,
                    "reviewedByUid" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun respondToAccessRequest(request: AccessRequest, approve: Boolean): Result<Unit> {
        val docPath = "access_requests/${request.id}"
        return try {
            val uid = requireUserId()
            require(request.id.isNotBlank() && request.targetId.isNotBlank())
            val reqRef = db.collection("access_requests").document(request.id)
            val newStatus = if (approve) AccessRequestStatus.APPROVED.name else AccessRequestStatus.REJECTED.name

            val batch = db.batch()
            batch.update(
                reqRef,
                mapOf(
                    "status" to newStatus,
                    "reviewedByUid" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (approve) {
                if (request.targetType == AccessTargetType.MEMBER.name) {
                    val memberRef = db.collection("members").document(request.targetId)
                    batch.update(
                        memberRef,
                        mapOf(
                            "authorizedViewerUids" to FieldValue.arrayUnion(request.requesterUid),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    val privateRef = memberRef.collection("private_details").document("info")
                    val privateExists = try {
                        privateRef.get().await().exists()
                    } catch (_: Exception) {
                        false
                    }
                    if (privateExists) {
                        batch.update(
                            privateRef,
                            mapOf(
                                "authorizedViewerUids" to FieldValue.arrayUnion(request.requesterUid),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                } else {
                    val familyRef = db.collection("families").document(request.targetId)
                    batch.update(
                        familyRef,
                        mapOf(
                            "authorizedViewerUids" to FieldValue.arrayUnion(request.requesterUid),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun revokeAccess(request: AccessRequest): Result<Unit> {
        val docPath = "access_requests/${request.id}"
        return try {
            val uid = requireUserId()
            val reqRef = db.collection("access_requests").document(request.id)
            val batch = db.batch()
            batch.update(
                reqRef,
                mapOf(
                    "status" to AccessRequestStatus.REVOKED.name,
                    "reviewedByUid" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (request.targetType == AccessTargetType.MEMBER.name) {
                val memberRef = db.collection("members").document(request.targetId)
                batch.update(
                    memberRef,
                    mapOf(
                        "authorizedViewerUids" to FieldValue.arrayRemove(request.requesterUid),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                val privateRef = memberRef.collection("private_details").document("info")
                val privateExists = try {
                    privateRef.get().await().exists()
                } catch (_: Exception) {
                    false
                }
                if (privateExists) {
                    batch.update(
                        privateRef,
                        mapOf(
                            "authorizedViewerUids" to FieldValue.arrayRemove(request.requesterUid),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            } else {
                val familyRef = db.collection("families").document(request.targetId)
                batch.update(
                    familyRef,
                    mapOf(
                        "authorizedViewerUids" to FieldValue.arrayRemove(request.requesterUid),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun verifyMember(memberId: String, status: VerificationStatus): Result<Unit> {
        val docPath = "members/$memberId"
        return try {
            requireUserId()
            db.collection("members").document(memberId).update(
                mapOf(
                    "verificationStatus" to status.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun verifyFamily(familyId: String, status: VerificationStatus): Result<Unit> {
        val docPath = "families/$familyId"
        return try {
            requireUserId()
            db.collection("families").document(familyId).update(
                mapOf(
                    "verificationStatus" to status.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addAdminRole(
        targetUid: String,
        email: String,
        displayName: String,
        roleLabel: String
    ): Result<Unit> {
        val cleanUid = targetUid.trim()
        val docPath = "admins/$cleanUid"
        return try {
            val uid = requireUserId()
            require(cleanUid.isNotBlank()) { "User UID is required" }
            val safeRole = if (roleLabel in listOf("ADMIN", "SARPANCH", "SECRETARY")) roleLabel else "ADMIN"
            db.collection("admins").document(cleanUid).set(
                mapOf(
                    "uid" to cleanUid,
                    "email" to email.trim().take(200),
                    "displayName" to displayName.trim().take(100),
                    "roleLabel" to safeRole,
                    "addedByUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun removeAdminRole(targetUid: String): Result<Unit> {
        val docPath = "admins/$targetUid"
        return try {
            val uid = requireUserId()
            require(targetUid != uid) { "Cannot remove your own admin role" }
            db.collection("admins").document(targetUid).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    companion object {
        const val BOOTSTRAPPED_ADMIN_EMAIL = "ajayhoney795@gmail.com"
    }
}
