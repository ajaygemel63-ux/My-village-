package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AccessRequest
import com.example.data.model.AccessTargetType
import com.example.data.model.AdminRole
import com.example.data.model.Family
import com.example.data.model.Gender
import com.example.data.model.Member
import com.example.data.model.PrivateMemberDetail
import com.example.data.model.RationCategory
import com.example.data.model.RecordVisibility
import com.example.data.model.RelationToHead
import com.example.data.model.VerificationStatus
import com.example.data.model.VillageConfig
import com.example.data.repository.VillageRepository
import com.example.localization.AppLanguage
import com.example.localization.LanguagePreferences
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class MemberScopeFilter {
    ALL, PUBLIC, MY_RECORDS, SHARED_WITH_ME
}

data class VillageDashboardData(
    val villageConfig: VillageConfig = VillageConfig(),
    val isAdmin: Boolean = false,
    val members: List<Member> = emptyList(),
    val families: List<Family> = emptyList(),
    val incomingRequests: List<AccessRequest> = emptyList(),
    val outgoingRequests: List<AccessRequest> = emptyList(),
    val allRequestsAdmin: List<AccessRequest> = emptyList(),
    val adminRoles: List<AdminRole> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class VillageViewModel(
    private val repository: VillageRepository,
    private val languagePreferences: LanguagePreferences,
    private val currentUserId: String,
    private val currentUserEmail: String?,
    private val currentUserName: String?,
    private val isEmailVerified: Boolean
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedWard = MutableStateFlow<String?>(null)
    val selectedWard: StateFlow<String?> = _selectedWard.asStateFlow()

    private val _scopeFilter = MutableStateFlow(MemberScopeFilter.ALL)
    val scopeFilter: StateFlow<MemberScopeFilter> = _scopeFilter.asStateFlow()

    private val _privateDetailState = MutableStateFlow<UiState<PrivateMemberDetail?>>(UiState.Success(null))
    val privateDetailState: StateFlow<UiState<PrivateMemberDetail?>> = _privateDetailState.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()

    val language: StateFlow<AppLanguage> = languagePreferences.languageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppLanguage.BILINGUAL)

    private fun <T> Flow<T>.unwrapFirestoreErrors(fallbackMsg: String): Flow<Result<T>> =
        map { Result.success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, fallbackMsg, rootError)
                emit(Result.failure(Exception(rootError.message ?: fallbackMsg, rootError)))
            }

    private val isAdminFlow: Flow<Boolean> = repository
        .observeIsAdmin(currentUserId, currentUserEmail, isEmailVerified)
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Admin check failed, defaulting to villager", error)
            emit(false)
        }

    private val membersFlow: Flow<List<Member>> = isAdminFlow.flatMapLatest { admin ->
        if (admin) {
            repository.observeAllMembersAdmin()
        } else {
            combine(
                repository.observePublicMembers(),
                repository.observeMyMembers(currentUserId),
                repository.observeSharedMembers(currentUserId)
            ) { publicList, myList, sharedList ->
                (myList + sharedList + publicList)
                    .distinctBy { it.id }
            }
        }
    }

    private val familiesFlow: Flow<List<Family>> = isAdminFlow.flatMapLatest { admin ->
        if (admin) {
            repository.observeAllFamiliesAdmin()
        } else {
            combine(
                repository.observePublicFamilies(),
                repository.observeMyFamilies(currentUserId),
                repository.observeSharedFamilies(currentUserId)
            ) { publicList, myList, sharedList ->
                (myList + sharedList + publicList)
                    .distinctBy { it.id }
            }
        }
    }

    private val adminRequestsFlow: Flow<List<AccessRequest>> = isAdminFlow.flatMapLatest { admin ->
        if (admin) repository.observeAllRequestsAdmin() else flowOf(emptyList())
    }

    private val adminRolesFlow: Flow<List<AdminRole>> = isAdminFlow.flatMapLatest { admin ->
        if (admin) repository.observeAdminsList() else flowOf(emptyList())
    }

    private val coreDataFlow: Flow<VillageDashboardData> = combine(
        repository.observeVillageConfig(),
        isAdminFlow,
        membersFlow,
        familiesFlow,
        repository.observeIncomingRequests(currentUserId)
    ) { config, isAdmin, members, families, incoming ->
        VillageDashboardData(
            villageConfig = config,
            isAdmin = isAdmin,
            members = members.sortedBy { it.fullName.lowercase() },
            families = families.sortedBy { it.familyCode.lowercase() },
            incomingRequests = incoming.sortedByDescending { it.createdAt?.seconds ?: 0L }
        )
    }

    val dashboardState: StateFlow<UiState<VillageDashboardData>> = combine(
        coreDataFlow,
        repository.observeOutgoingRequests(currentUserId),
        adminRequestsFlow,
        adminRolesFlow
    ) { core, outgoing, allAdminReqs, adminRoles ->
        core.copy(
            outgoingRequests = outgoing.sortedByDescending { it.createdAt?.seconds ?: 0L },
            allRequestsAdmin = allAdminReqs.sortedByDescending { it.createdAt?.seconds ?: 0L },
            adminRoles = adminRoles
        )
    }
        .map<VillageDashboardData, UiState<VillageDashboardData>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing village dashboard", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load village records"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    fun syncUserProfile(user: FirebaseUser) {
        viewModelScope.launch {
            repository.ensureUserProfile(user, language.value)
        }
    }

    fun setLanguage(newLang: AppLanguage) {
        viewModelScope.launch {
            languagePreferences.setLanguage(newLang)
            repository.updateUserLanguage(newLang)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedWard(ward: String?) {
        _selectedWard.value = ward
    }

    fun setScopeFilter(filter: MemberScopeFilter) {
        _scopeFilter.value = filter
    }

    fun loadPrivateDetail(memberId: String) {
        _privateDetailState.value = UiState.Loading
        viewModelScope.launch {
            val result = repository.fetchPrivateMemberDetail(memberId)
            result.fold(
                onSuccess = { detail ->
                    _privateDetailState.value = UiState.Success(detail)
                },
                onFailure = { error ->
                    _privateDetailState.value = UiState.Error(error.message ?: "Permission required")
                }
            )
        }
    }

    fun clearPrivateDetail() {
        _privateDetailState.value = UiState.Success(null)
    }

    fun registerOrUpdateMember(
        editingMember: Member?,
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
        isAdmin: Boolean,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = if (editingMember == null) {
                repository.registerMember(
                    familyId = familyId,
                    familyCode = familyCode,
                    fullName = fullName,
                    fullNameLocal = fullNameLocal,
                    guardianName = guardianName,
                    relationToHead = relationToHead,
                    age = age,
                    gender = gender,
                    ward = ward,
                    occupation = occupation,
                    rawAadhaar = rawAadhaar,
                    rawVoterId = rawVoterId,
                    visibility = visibility,
                    phoneNumber = phoneNumber,
                    fullAddress = fullAddress,
                    emergencyContact = emergencyContact,
                    privateNotes = privateNotes,
                    isAdmin = isAdmin
                ).map { Unit }
            } else {
                repository.updateMember(
                    existingMember = editingMember,
                    familyId = familyId,
                    familyCode = familyCode,
                    fullName = fullName,
                    fullNameLocal = fullNameLocal,
                    guardianName = guardianName,
                    relationToHead = relationToHead,
                    age = age,
                    gender = gender,
                    ward = ward,
                    occupation = occupation,
                    rawAadhaar = rawAadhaar,
                    rawVoterId = rawVoterId,
                    visibility = visibility,
                    phoneNumber = phoneNumber,
                    fullAddress = fullAddress,
                    emergencyContact = emergencyContact,
                    privateNotes = privateNotes
                )
            }
            _isSubmitting.value = false
            result.fold(
                onSuccess = {
                    _messages.tryEmit("Member saved / सदस्य सहेजा गया / ସଦସ୍ୟ ସେଭ୍ ହେଲା")
                    onComplete(true)
                },
                onFailure = { err ->
                    _messages.tryEmit(err.localizedMessage ?: "Failed to save member")
                    onComplete(false)
                }
            )
        }
    }

    fun deleteMember(memberId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = repository.deleteMember(memberId)
            _isSubmitting.value = false
            res.fold(
                onSuccess = {
                    _messages.tryEmit("Member deleted / सदस्य हटाया गया")
                    onComplete()
                },
                onFailure = { err ->
                    _messages.tryEmit(err.localizedMessage ?: "Delete failed")
                }
            )
        }
    }

    fun registerOrUpdateFamily(
        editingFamily: Family?,
        familyCode: String,
        headName: String,
        headNameLocal: String,
        ward: String,
        houseNumber: String,
        rationCategory: RationCategory,
        memberCount: Long,
        visibility: RecordVisibility,
        isAdmin: Boolean,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = if (editingFamily == null) {
                repository.registerFamily(
                    familyCode = familyCode,
                    headName = headName,
                    headNameLocal = headNameLocal,
                    ward = ward,
                    houseNumber = houseNumber,
                    rationCategory = rationCategory,
                    memberCount = memberCount,
                    visibility = visibility,
                    isAdmin = isAdmin
                ).map { Unit }
            } else {
                repository.updateFamily(
                    familyId = editingFamily.id,
                    familyCode = familyCode,
                    headName = headName,
                    headNameLocal = headNameLocal,
                    ward = ward,
                    houseNumber = houseNumber,
                    rationCategory = rationCategory,
                    memberCount = memberCount,
                    visibility = visibility
                )
            }
            _isSubmitting.value = false
            res.fold(
                onSuccess = {
                    _messages.tryEmit("Family saved / परिवार सहेजा गया / ପରିବାର ସେଭ୍ ହେଲା")
                    onComplete(true)
                },
                onFailure = { err ->
                    _messages.tryEmit(err.localizedMessage ?: "Failed to save family")
                    onComplete(false)
                }
            )
        }
    }

    fun deleteFamily(familyId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = repository.deleteFamily(familyId)
            _isSubmitting.value = false
            res.fold(
                onSuccess = {
                    _messages.tryEmit("Family deleted / परिवार हटाया गया")
                    onComplete()
                },
                onFailure = { err ->
                    _messages.tryEmit(err.localizedMessage ?: "Delete failed")
                }
            )
        }
    }

    fun requestAccess(
        targetType: AccessTargetType,
        targetId: String,
        targetName: String,
        targetOwnerUid: String,
        reason: String,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = repository.requestPrivateAccess(
                requesterName = currentUserName ?: currentUserEmail ?: "Villager",
                targetType = targetType,
                targetId = targetId,
                targetName = targetName,
                targetOwnerUid = targetOwnerUid,
                reason = reason
            )
            _isSubmitting.value = false
            res.fold(
                onSuccess = {
                    _messages.tryEmit("Permission request sent / अनुरोध भेजा गया / ଅନୁରୋଧ ପଠାଗଲା")
                    onComplete(true)
                },
                onFailure = { err ->
                    _messages.tryEmit(err.localizedMessage ?: "Request failed")
                    onComplete(false)
                }
            )
        }
    }

    fun respondToRequest(request: AccessRequest, approve: Boolean) {
        viewModelScope.launch {
            val res = repository.respondToAccessRequest(request, approve)
            res.fold(
                onSuccess = {
                    val msg = if (approve) "Permission granted / अनुमति दी गई" else "Request rejected / अस्वीकृत"
                    _messages.tryEmit(msg)
                },
                onFailure = { err ->
                    _messages.tryEmit(err.localizedMessage ?: "Action failed")
                }
            )
        }
    }

    fun revokeRequestAccess(request: AccessRequest) {
        viewModelScope.launch {
            val res = repository.revokeAccess(request)
            res.fold(
                onSuccess = { _messages.tryEmit("Permission revoked / अनुमति वापस ली गई") },
                onFailure = { err -> _messages.tryEmit(err.localizedMessage ?: "Revoke failed") }
            )
        }
    }

    fun saveVillageSettings(
        nameHi: String,
        nameOr: String,
        nameEn: String,
        district: String,
        stateName: String,
        wardsRaw: String,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val wards = wardsRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val res = repository.saveVillageConfig(
                villageNameHi = nameHi,
                villageNameOr = nameOr,
                villageNameEn = nameEn,
                district = district,
                stateName = stateName,
                wards = if (wards.isEmpty()) VillageConfig.DEFAULT_WARDS else wards
            )
            _isSubmitting.value = false
            res.fold(
                onSuccess = {
                    _messages.tryEmit("Village details updated / गाँव की जानकारी सहेजी गई")
                    onComplete(true)
                },
                onFailure = { err ->
                    _messages.tryEmit(err.localizedMessage ?: "Update failed")
                    onComplete(false)
                }
            )
        }
    }

    fun verifyMember(memberId: String, status: VerificationStatus) {
        viewModelScope.launch {
            val res = repository.verifyMember(memberId, status)
            res.fold(
                onSuccess = { _messages.tryEmit("Status updated: ${status.name}") },
                onFailure = { err -> _messages.tryEmit(err.localizedMessage ?: "Update failed") }
            )
        }
    }

    fun verifyFamily(familyId: String, status: VerificationStatus) {
        viewModelScope.launch {
            val res = repository.verifyFamily(familyId, status)
            res.fold(
                onSuccess = { _messages.tryEmit("Family status updated: ${status.name}") },
                onFailure = { err -> _messages.tryEmit(err.localizedMessage ?: "Update failed") }
            )
        }
    }

    fun addAdmin(uid: String, email: String, name: String, roleLabel: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = repository.addAdminRole(uid, email, name, roleLabel)
            _isSubmitting.value = false
            res.fold(
                onSuccess = {
                    _messages.tryEmit("Admin added / एडमिन जोड़ा गया")
                    onComplete(true)
                },
                onFailure = { err ->
                    _messages.tryEmit(err.localizedMessage ?: "Failed to add admin")
                    onComplete(false)
                }
            )
        }
    }

    fun removeAdmin(uid: String) {
        viewModelScope.launch {
            val res = repository.removeAdminRole(uid)
            res.fold(
                onSuccess = { _messages.tryEmit("Admin removed") },
                onFailure = { err -> _messages.tryEmit(err.localizedMessage ?: "Failed to remove admin") }
            )
        }
    }

    private companion object {
        const val TAG = "VillageVM"
    }
}
