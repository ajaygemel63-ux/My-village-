package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.authStateFlow
import com.example.auth.signOut
import com.example.data.model.AccessRequestStatus
import com.example.data.model.AccessTargetType
import com.example.data.model.Family
import com.example.data.model.Member
import com.example.data.model.PrivateMemberDetail
import com.example.data.model.VerificationStatus
import com.example.data.repository.VillageRepository
import com.example.localization.AppLanguage
import com.example.localization.LanguagePreferences
import com.example.localization.VillageStrings
import com.example.localization.tr
import com.example.ui.components.LanguageToggleBar
import com.example.ui.screens.AdminConsoleScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DirectoryScreen
import com.example.ui.screens.FamiliesScreen
import com.example.ui.screens.FamilyDetailBottomSheet
import com.example.ui.screens.FamilyFormScreen
import com.example.ui.screens.MemberDetailBottomSheet
import com.example.ui.screens.MemberFormScreen
import com.example.ui.screens.PermissionsAndProfileScreen
import com.example.ui.screens.RequestAccessDialog
import com.example.ui.screens.VillageSettingsDialog
import com.example.ui.theme.MyVillageTheme
import com.example.ui.viewmodel.UiState
import com.example.ui.viewmodel.VillageDashboardData
import com.example.ui.viewmodel.VillageViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

enum class VillageTab {
    DIRECTORY, FAMILIES, PERMISSIONS, ADMIN
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyVillageTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val languagePreferences = remember(context) { LanguagePreferences(context.applicationContext) }
    val persistedLanguage by languagePreferences.languageFlow
        .collectAsStateWithLifecycle(initialValue = AppLanguage.BILINGUAL)

    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var preferAdminTabOnLogin by rememberSaveable { mutableStateOf(false) }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            language = persistedLanguage,
            onLanguageChange = { newLang ->
                scope.launch { languagePreferences.setLanguage(newLang) }
            },
            onAuthSuccess = { requestedAdmin ->
                preferAdminTabOnLogin = requestedAdmin
            }
        )
    } else {
        key(user.uid) {
            VillageAppShell(
                user = user,
                preferAdminTab = preferAdminTabOnLogin,
                onClearPreferAdminTab = { preferAdminTabOnLogin = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VillageAppShell(
    user: FirebaseUser,
    preferAdminTab: Boolean,
    onClearPreferAdminTab: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: VillageViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                VillageViewModel(
                    repository = VillageRepository(db),
                    languagePreferences = LanguagePreferences(app),
                    currentUserId = user.uid,
                    currentUserEmail = user.email,
                    currentUserName = user.displayName,
                    isEmailVerified = user.isEmailVerified
                )
            }
        }
    )

    LaunchedEffect(user.uid) {
        viewModel.syncUserProfile(user)
    }

    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedWard by viewModel.selectedWard.collectAsStateWithLifecycle()
    val scopeFilter by viewModel.scopeFilter.collectAsStateWithLifecycle()
    val privateDetailState by viewModel.privateDetailState.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.messages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    var activeTab by rememberSaveable { mutableStateOf(VillageTab.DIRECTORY) }
    var showMemberForm by remember { mutableStateOf(false) }
    var editingMember by remember { mutableStateOf<Member?>(null) }
    var editingMemberPrivateDetail by remember { mutableStateOf<PrivateMemberDetail?>(null) }
    var prefilledFamilyForMember by remember { mutableStateOf<Family?>(null) }

    var showFamilyForm by remember { mutableStateOf(false) }
    var editingFamily by remember { mutableStateOf<Family?>(null) }

    var selectedMemberDetail by remember { mutableStateOf<Member?>(null) }
    var selectedFamilyDetail by remember { mutableStateOf<Family?>(null) }
    var requestAccessTarget by remember { mutableStateOf<Triple<AccessTargetType, String, Pair<String, String>>?>(null) }
    var showVillageSettingsDialog by remember { mutableStateOf(false) }

    val dashboardData = (dashboardState as? UiState.Success)?.data

    LaunchedEffect(preferAdminTab, dashboardData?.isAdmin) {
        if (preferAdminTab && dashboardData != null) {
            if (dashboardData.isAdmin) {
                activeTab = VillageTab.ADMIN
            } else {
                snackbarHostState.showSnackbar(
                    tr(
                        language,
                        "इस खाते के पास एडमिन अधिकार नहीं है।",
                        "ଏହି ଆକାଉଣ୍ଟରେ ଆଡମିନ୍ ଅଧିକାର ନାହିଁ।"
                    )
                )
            }
            onClearPreferAdminTab()
        }
    }

    if (showMemberForm && dashboardData != null) {
        MemberFormScreen(
            language = language,
            wards = dashboardData.villageConfig.wards,
            families = dashboardData.families,
            editingMember = editingMember,
            initialPrivateDetail = editingMemberPrivateDetail,
            prefilledFamily = prefilledFamilyForMember,
            isSubmitting = isSubmitting,
            onDismiss = {
                showMemberForm = false
                editingMember = null
                editingMemberPrivateDetail = null
                prefilledFamilyForMember = null
            },
            onSubmit = { familyId, familyCode, fullName, localName, guardian, rel, age, gender, ward, occupation, rawAadhaar, rawVoter, visibility, phone, address, emergency, notes ->
                viewModel.registerOrUpdateMember(
                    editingMember = editingMember,
                    familyId = familyId,
                    familyCode = familyCode,
                    fullName = fullName,
                    fullNameLocal = localName,
                    guardianName = guardian,
                    relationToHead = rel,
                    age = age,
                    gender = gender,
                    ward = ward,
                    occupation = occupation,
                    rawAadhaar = rawAadhaar,
                    rawVoterId = rawVoter,
                    visibility = visibility,
                    phoneNumber = phone,
                    fullAddress = address,
                    emergencyContact = emergency,
                    privateNotes = notes,
                    isAdmin = dashboardData.isAdmin
                ) { ok ->
                    if (ok) {
                        showMemberForm = false
                        editingMember = null
                        editingMemberPrivateDetail = null
                        prefilledFamilyForMember = null
                    }
                }
            }
        )
        return
    }

    if (showFamilyForm && dashboardData != null) {
        FamilyFormScreen(
            language = language,
            wards = dashboardData.villageConfig.wards,
            editingFamily = editingFamily,
            isSubmitting = isSubmitting,
            onDismiss = {
                showFamilyForm = false
                editingFamily = null
            },
            onSubmit = { code, head, headLocal, ward, houseNo, ration, count, visibility ->
                viewModel.registerOrUpdateFamily(
                    editingFamily = editingFamily,
                    familyCode = code,
                    headName = head,
                    headNameLocal = headLocal,
                    ward = ward,
                    houseNumber = houseNo,
                    rationCategory = ration,
                    memberCount = count,
                    visibility = visibility,
                    isAdmin = dashboardData.isAdmin
                ) { ok ->
                    if (ok) {
                        showFamilyForm = false
                        editingFamily = null
                    }
                }
            }
        )
        return
    }

    if (showVillageSettingsDialog && dashboardData != null) {
        VillageSettingsDialog(
            config = dashboardData.villageConfig,
            language = language,
            isSubmitting = isSubmitting,
            onDismiss = { showVillageSettingsDialog = false },
            onSave = { hi, or, en, dist, state, wards ->
                viewModel.saveVillageSettings(hi, or, en, dist, state, wards) { ok ->
                    if (ok) showVillageSettingsDialog = false
                }
            }
        )
    }

    requestAccessTarget?.let { (targetType, targetId, nameAndOwner) ->
        RequestAccessDialog(
            language = language,
            targetName = nameAndOwner.first,
            isSubmitting = isSubmitting,
            onDismiss = { requestAccessTarget = null },
            onConfirm = { reason ->
                viewModel.requestAccess(
                    targetType = targetType,
                    targetId = targetId,
                    targetName = nameAndOwner.first,
                    targetOwnerUid = nameAndOwner.second,
                    reason = reason
                ) { ok ->
                    if (ok) requestAccessTarget = null
                }
            }
        )
    }

    selectedMemberDetail?.let { member ->
        MemberDetailBottomSheet(
            member = member,
            currentUserId = user.uid,
            isAdmin = dashboardData?.isAdmin == true,
            language = language,
            privateDetailState = privateDetailState,
            onDismiss = {
                selectedMemberDetail = null
                viewModel.clearPrivateDetail()
            },
            onEditClick = { loadedDetail ->
                selectedMemberDetail = null
                viewModel.clearPrivateDetail()
                editingMember = member
                editingMemberPrivateDetail = loadedDetail
                prefilledFamilyForMember = null
                showMemberForm = true
            },
            onDeleteClick = {
                viewModel.deleteMember(member.id) {
                    selectedMemberDetail = null
                    viewModel.clearPrivateDetail()
                }
            },
            onRequestAccessClick = {
                selectedMemberDetail = null
                requestAccessTarget = Triple(
                    AccessTargetType.MEMBER,
                    member.id,
                    member.fullName to member.ownerUid
                )
            },
            onVerifyClick = { status ->
                viewModel.verifyMember(member.id, status)
                selectedMemberDetail = null
            }
        )
    }

    selectedFamilyDetail?.let { family ->
        val linkedMembers = dashboardData?.members?.filter { it.familyId == family.id }.orEmpty()
        FamilyDetailBottomSheet(
            family = family,
            linkedMembers = linkedMembers,
            currentUserId = user.uid,
            isAdmin = dashboardData?.isAdmin == true,
            language = language,
            onDismiss = { selectedFamilyDetail = null },
            onAddMemberToFamily = { fam ->
                selectedFamilyDetail = null
                editingMember = null
                editingMemberPrivateDetail = null
                prefilledFamilyForMember = fam
                showMemberForm = true
            },
            onMemberClick = { member ->
                selectedFamilyDetail = null
                selectedMemberDetail = member
                if (member.ownerUid == user.uid || dashboardData?.isAdmin == true || user.uid in member.authorizedViewerUids) {
                    viewModel.loadPrivateDetail(member.id)
                }
            },
            onEditFamily = { fam ->
                selectedFamilyDetail = null
                editingFamily = fam
                showFamilyForm = true
            },
            onDeleteFamily = { fam ->
                viewModel.deleteFamily(fam.id) {
                    selectedFamilyDetail = null
                }
            }
        )
    }

    val pendingIncomingCount = dashboardData?.incomingRequests?.count {
        it.status == AccessRequestStatus.PENDING.name
    } ?: 0
    val pendingApprovalsCount = if (dashboardData?.isAdmin == true) {
        dashboardData.members.count { it.verificationStatus == VerificationStatus.PENDING.name } +
            dashboardData.families.count { it.verificationStatus == VerificationStatus.PENDING.name }
    } else 0

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    title = {
                        Text(
                            text = VillageStrings.appTitle(language),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    actions = {
                        LanguageToggleBar(
                            selected = language,
                            onSelect = { viewModel.setLanguage(it) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                )
            },
            floatingActionButton = {
                if (dashboardData != null && (activeTab == VillageTab.DIRECTORY || activeTab == VillageTab.FAMILIES)) {
                    FloatingActionButton(
                        onClick = {
                            if (activeTab == VillageTab.DIRECTORY) {
                                editingMember = null
                                editingMemberPrivateDetail = null
                                prefilledFamilyForMember = null
                                showMemberForm = true
                            } else {
                                editingFamily = null
                                showFamilyForm = true
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.testTag("main_fab")
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = if (activeTab == VillageTab.DIRECTORY) {
                                VillageStrings.registerMember(language)
                            } else {
                                VillageStrings.registerFamily(language)
                            }
                        )
                    }
                }
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar {
                        VillageBottomBarItems(
                            activeTab = activeTab,
                            isAdmin = dashboardData?.isAdmin == true,
                            language = language,
                            pendingIncomingCount = pendingIncomingCount,
                            pendingApprovalsCount = pendingApprovalsCount,
                            onSelect = { activeTab = it }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        VillageRailItems(
                            activeTab = activeTab,
                            isAdmin = dashboardData?.isAdmin == true,
                            language = language,
                            pendingIncomingCount = pendingIncomingCount,
                            pendingApprovalsCount = pendingApprovalsCount,
                            onSelect = { activeTab = it }
                        )
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 720.dp)
                            .fillMaxSize()
                    ) {
                        when (val state = dashboardState) {
                            is UiState.Loading -> {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                                }
                            }
                            is UiState.Error -> {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Text(
                                        text = state.message,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                            is UiState.Success -> {
                                val data = state.data
                                when (activeTab) {
                                    VillageTab.DIRECTORY -> DirectoryScreen(
                                        data = data,
                                        currentUserId = user.uid,
                                        language = language,
                                        searchQuery = searchQuery,
                                        selectedWard = selectedWard,
                                        scopeFilter = scopeFilter,
                                        onSearchChange = viewModel::setSearchQuery,
                                        onWardSelect = viewModel::setSelectedWard,
                                        onScopeSelect = viewModel::setScopeFilter,
                                        onEditVillageClick = { showVillageSettingsDialog = true },
                                        onMemberClick = { member ->
                                            selectedMemberDetail = member
                                            if (member.ownerUid == user.uid || data.isAdmin || user.uid in member.authorizedViewerUids) {
                                                viewModel.loadPrivateDetail(member.id)
                                            }
                                        },
                                        onAddMemberClick = {
                                            editingMember = null
                                            editingMemberPrivateDetail = null
                                            prefilledFamilyForMember = null
                                            showMemberForm = true
                                        }
                                    )
                                    VillageTab.FAMILIES -> FamiliesScreen(
                                        data = data,
                                        currentUserId = user.uid,
                                        language = language,
                                        onFamilyClick = { selectedFamilyDetail = it },
                                        onAddFamilyClick = {
                                            editingFamily = null
                                            showFamilyForm = true
                                        }
                                    )
                                    VillageTab.PERMISSIONS -> PermissionsAndProfileScreen(
                                        data = data,
                                        currentUserId = user.uid,
                                        userName = user.displayName,
                                        userEmail = user.email,
                                        language = language,
                                        onLanguageChange = viewModel::setLanguage,
                                        onApproveRequest = { viewModel.respondToRequest(it, approve = true) },
                                        onRejectRequest = { viewModel.respondToRequest(it, approve = false) },
                                        onRevokeAccess = viewModel::revokeRequestAccess,
                                        onSignOutClick = {
                                            signOut(
                                                credentialManager = credentialManager,
                                                scope = scope
                                            )
                                        }
                                    )
                                    VillageTab.ADMIN -> {
                                        if (data.isAdmin) {
                                            AdminConsoleScreen(
                                                data = data,
                                                currentUserId = user.uid,
                                                language = language,
                                                isSubmitting = isSubmitting,
                                                onEditVillageClick = { showVillageSettingsDialog = true },
                                                onVerifyMember = viewModel::verifyMember,
                                                onVerifyFamily = viewModel::verifyFamily,
                                                onApproveRequest = { viewModel.respondToRequest(it, approve = true) },
                                                onRejectRequest = { viewModel.respondToRequest(it, approve = false) },
                                                onRevokeRequest = viewModel::revokeRequestAccess,
                                                onAddAdmin = viewModel::addAdmin,
                                                onRemoveAdmin = viewModel::removeAdmin
                                            )
                                        } else {
                                            activeTab = VillageTab.DIRECTORY
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun navDestinations(
    isAdmin: Boolean,
    language: AppLanguage
) = buildList {
    add(Triple(VillageTab.DIRECTORY, Icons.Default.People, VillageStrings.villageDirectory(language)))
    add(Triple(VillageTab.FAMILIES, Icons.Default.Groups, VillageStrings.families(language)))
    add(Triple(VillageTab.PERMISSIONS, Icons.Default.Security, VillageStrings.permissions(language)))
    if (isAdmin) {
        add(Triple(VillageTab.ADMIN, Icons.Default.AdminPanelSettings, VillageStrings.admin(language)))
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.VillageBottomBarItems(
    activeTab: VillageTab,
    isAdmin: Boolean,
    language: AppLanguage,
    pendingIncomingCount: Int,
    pendingApprovalsCount: Int,
    onSelect: (VillageTab) -> Unit
) {
    navDestinations(isAdmin, language).forEach { (tab, icon, label) ->
        val badgeCount = when (tab) {
            VillageTab.PERMISSIONS -> pendingIncomingCount
            VillageTab.ADMIN -> pendingApprovalsCount
            else -> 0
        }
        NavigationBarItem(
            selected = activeTab == tab,
            onClick = { onSelect(tab) },
            icon = {
                if (badgeCount > 0) {
                    BadgedBox(badge = { Badge { Text("$badgeCount") } }) {
                        Icon(icon, contentDescription = label)
                    }
                } else {
                    Icon(icon, contentDescription = label)
                }
            },
            label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
        )
    }
}

@Composable
private fun VillageRailItems(
    activeTab: VillageTab,
    isAdmin: Boolean,
    language: AppLanguage,
    pendingIncomingCount: Int,
    pendingApprovalsCount: Int,
    onSelect: (VillageTab) -> Unit
) {
    navDestinations(isAdmin, language).forEach { (tab, icon, label) ->
        val badgeCount = when (tab) {
            VillageTab.PERMISSIONS -> pendingIncomingCount
            VillageTab.ADMIN -> pendingApprovalsCount
            else -> 0
        }
        NavigationRailItem(
            selected = activeTab == tab,
            onClick = { onSelect(tab) },
            icon = {
                if (badgeCount > 0) {
                    BadgedBox(badge = { Badge { Text("$badgeCount") } }) {
                        Icon(icon, contentDescription = label)
                    }
                } else {
                    Icon(icon, contentDescription = label)
                }
            },
            label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
        )
    }
}
