package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Gender
import com.example.data.model.Member
import com.example.data.model.PrivateMemberDetail
import com.example.data.model.RecordVisibility
import com.example.data.model.RelationToHead
import com.example.data.model.VerificationStatus
import com.example.localization.AppLanguage
import com.example.localization.VillageStrings
import com.example.localization.tr
import com.example.ui.components.HeroVillageBanner
import com.example.ui.components.MaskedIdPill
import com.example.ui.components.PrivacyBadge
import com.example.ui.components.VerificationBadge
import com.example.ui.viewmodel.MemberScopeFilter
import com.example.ui.viewmodel.UiState
import com.example.ui.viewmodel.VillageDashboardData

@Composable
fun DirectoryScreen(
    data: VillageDashboardData,
    currentUserId: String,
    language: AppLanguage,
    searchQuery: String,
    selectedWard: String?,
    scopeFilter: MemberScopeFilter,
    onSearchChange: (String) -> Unit,
    onWardSelect: (String?) -> Unit,
    onScopeSelect: (MemberScopeFilter) -> Unit,
    onEditVillageClick: () -> Unit,
    onMemberClick: (Member) -> Unit,
    onAddMemberClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val wards = data.villageConfig.wards
    val filteredMembers = remember(data.members, searchQuery, selectedWard, scopeFilter, currentUserId) {
        val q = searchQuery.trim().lowercase()
        data.members.filter { m ->
            val matchesScope = when (scopeFilter) {
                MemberScopeFilter.ALL -> true
                MemberScopeFilter.PUBLIC -> m.visibility == RecordVisibility.PUBLIC.name
                MemberScopeFilter.MY_RECORDS -> m.ownerUid == currentUserId
                MemberScopeFilter.SHARED_WITH_ME -> currentUserId in m.authorizedViewerUids
            }
            val matchesWard = selectedWard == null || m.ward == selectedWard
            val matchesSearch = q.isEmpty() ||
                m.fullName.lowercase().contains(q) ||
                m.fullNameLocal.lowercase().contains(q) ||
                m.guardianName.lowercase().contains(q) ||
                m.ward.lowercase().contains(q) ||
                m.familyCode.lowercase().contains(q) ||
                m.aadhaarMasked.lowercase().contains(q) ||
                m.aadhaarLast4.lowercase().contains(q) ||
                m.voterIdMasked.lowercase().contains(q) ||
                m.voterIdLast4.lowercase().contains(q) ||
                m.occupation.lowercase().contains(q)
            matchesScope && matchesWard && matchesSearch
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HeroVillageBanner(
                config = data.villageConfig,
                lang = language,
                isAdmin = data.isAdmin,
                memberCount = data.members.size,
                familyCount = data.families.size,
                onEditVillageClick = onEditVillageClick
            )
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = tr(language, "साफ़ करें", "ସଫା କରନ୍ତୁ"))
                        }
                    }
                },
                placeholder = {
                    Text(
                        VillageStrings.searchHint(language),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("member_search_input")
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = scopeFilter == MemberScopeFilter.ALL,
                        onClick = { onScopeSelect(MemberScopeFilter.ALL) },
                        label = { Text(VillageStrings.allFilter(language)) }
                    )
                    FilterChip(
                        selected = scopeFilter == MemberScopeFilter.PUBLIC,
                        onClick = { onScopeSelect(MemberScopeFilter.PUBLIC) },
                        label = { Text(VillageStrings.publicRecord(language)) }
                    )
                    FilterChip(
                        selected = scopeFilter == MemberScopeFilter.MY_RECORDS,
                        onClick = { onScopeSelect(MemberScopeFilter.MY_RECORDS) },
                        label = { Text(VillageStrings.myRecordsFilter(language)) }
                    )
                    FilterChip(
                        selected = scopeFilter == MemberScopeFilter.SHARED_WITH_ME,
                        onClick = { onScopeSelect(MemberScopeFilter.SHARED_WITH_ME) },
                        label = { Text(VillageStrings.sharedRecord(language)) }
                    )
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedWard == null,
                        onClick = { onWardSelect(null) },
                        label = { Text(tr(language, "सभी वार्ड", "ସମସ୍ତ ୱାର୍ଡ")) }
                    )
                    wards.forEach { ward ->
                        FilterChip(
                            selected = selectedWard == ward,
                            onClick = { onWardSelect(if (selectedWard == ward) null else ward) },
                            label = { Text(ward.substringBefore("/").trim()) }
                        )
                    }
                }
            }
        }

        if (filteredMembers.isEmpty()) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(42.dp)
                        )
                        Text(
                            text = tr(
                                language,
                                "अभी कोई सदस्य रिकॉर्ड नहीं मिला",
                                "ବର୍ତ୍ତମାନ କୌଣସି ସଦସ୍ୟ ରେକର୍ଡ ମିଳିଲା ନାହିଁ"
                            ),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = tr(
                                language,
                                "नीचे '+' बटन दबाकर गाँव के सदस्य को मास्क्ड आधार व वोटर आईडी के साथ जोड़ें।",
                                "ତଳେ '+' ବଟନ୍ ଦବାଇ ମାସ୍କ ଆଧାର ଏବଂ ଭୋଟର ଆଇଡି ସହିତ ସଦସ୍ୟ ଯୋଡନ୍ତୁ।"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = onAddMemberClick) {
                            Text(VillageStrings.registerMember(language))
                        }
                    }
                }
            }
        } else {
            items(filteredMembers, key = { it.id }) { member ->
                MemberDirectoryCard(
                    member = member,
                    currentUserId = currentUserId,
                    language = language,
                    onClick = { onMemberClick(member) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemberDirectoryCard(
    member: Member,
    currentUserId: String,
    language: AppLanguage,
    onClick: () -> Unit
) {
    val visibility = RecordVisibility.fromRaw(member.visibility)
    val verification = VerificationStatus.fromRaw(member.verificationStatus)
    val isSharedViewer = currentUserId in member.authorizedViewerUids && member.ownerUid != currentUserId

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("member_card_${member.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = member.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (member.fullNameLocal.isNotBlank()) {
                        Text(
                            text = member.fullNameLocal,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "${tr(language, "पिता/पति", "ପିତା/ସ୍ୱାମୀ")}: ${member.guardianName} • ${member.age}y • ${Gender.fromRaw(member.gender).label(language)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    VerificationBadge(status = verification, lang = language)
                    PrivacyBadge(
                        visibility = visibility,
                        isSharedViewer = isSharedViewer,
                        lang = language
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = member.ward.substringBefore("/").trim(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                if (member.familyCode.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "${member.familyCode} (${RelationToHead.fromRaw(member.relationToHead).label(language)})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }

            if (member.aadhaarMasked.isNotBlank() || member.voterIdMasked.isNotBlank()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MaskedIdPill(
                        label = "Aadhaar (Masked)",
                        maskedValue = member.aadhaarMasked,
                        icon = Icons.Default.Badge
                    )
                    MaskedIdPill(
                        label = "Voter ID (Masked)",
                        maskedValue = member.voterIdMasked,
                        icon = Icons.Default.HowToVote
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MemberDetailBottomSheet(
    member: Member,
    currentUserId: String,
    isAdmin: Boolean,
    language: AppLanguage,
    privateDetailState: UiState<PrivateMemberDetail?>,
    onDismiss: () -> Unit,
    onEditClick: (PrivateMemberDetail?) -> Unit,
    onDeleteClick: () -> Unit,
    onRequestAccessClick: () -> Unit,
    onVerifyClick: (VerificationStatus) -> Unit
) {
    val isOwner = member.ownerUid == currentUserId
    val hasPrivateAccess = isOwner || isAdmin || (currentUserId in member.authorizedViewerUids)
    val loadedDetail = (privateDetailState as? UiState.Success)?.data

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = member.fullName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (member.fullNameLocal.isNotBlank()) {
                        Text(
                            text = member.fullNameLocal,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                VerificationBadge(
                    status = VerificationStatus.fromRaw(member.verificationStatus),
                    lang = language
                )
            }

            Text(
                text = "${tr(language, "पिता/पति", "ପିତା/ସ୍ୱାମୀ")}: ${member.guardianName}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${VillageStrings.age(language)}: ${member.age} • ${Gender.fromRaw(member.gender).label(language)} • ${member.ward}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (member.occupation.isNotBlank()) {
                Text(
                    text = "${VillageStrings.occupation(language)}: ${member.occupation}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (member.familyCode.isNotBlank()) {
                Text(
                    text = "${tr(language, "परिवार", "ପରିବାର")}: ${member.familyCode} (${RelationToHead.fromRaw(member.relationToHead).label(language)})",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MaskedIdPill(
                    label = "Aadhaar (Masked)",
                    maskedValue = member.aadhaarMasked,
                    icon = Icons.Default.Badge
                )
                MaskedIdPill(
                    label = "Voter ID (Masked)",
                    maskedValue = member.voterIdMasked,
                    icon = Icons.Default.HowToVote
                )
            }

            HorizontalDivider()

            // Isolated Private Details Section
            Text(
                text = VillageStrings.privateDetailsHeader(language),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            if (!hasPrivateAccess) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = tr(
                                    language,
                                    "मोबाइल नंबर और पता गोपनीय (Private) है।",
                                    "ମୋବାଇଲ୍ ନମ୍ବର ଏବଂ ଠିକଣା ଗୋପନୀୟ ଅଟେ।"
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Button(
                            onClick = onRequestAccessClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(VillageStrings.requestAccess(language))
                        }
                    }
                }
            } else {
                when (privateDetailState) {
                    is UiState.Loading -> {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text(tr(language, "प्राइवेट विवरण लोड हो रहा है...", "ପ୍ରାଇଭେଟ୍ ବିବରଣୀ ଲୋଡ୍ ହେଉଛି..."))
                        }
                    }
                    is UiState.Error -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                Text(privateDetailState.message, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                    is UiState.Success -> {
                        val detail = privateDetailState.data
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (detail == null) {
                                    Text(
                                        text = tr(language, "कोई अतिरिक्त प्राइवेट विवरण दर्ज नहीं है", "କୌଣସି ଅତିରିକ୍ତ ପ୍ରାଇଭେଟ୍ ବିବରଣୀ ନାହିଁ"),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                } else {
                                    if (detail.phoneNumber.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Text("${VillageStrings.phoneLabel(language)}: ${detail.phoneNumber}", style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                    if (detail.fullAddress.isNotBlank()) {
                                        Text("${VillageStrings.addressLabel(language)}: ${detail.fullAddress}", style = MaterialTheme.typography.bodyMedium)
                                    }
                                    if (detail.emergencyContact.isNotBlank()) {
                                        Text("${VillageStrings.emergencyLabel(language)}: ${detail.emergencyContact}", style = MaterialTheme.typography.bodyMedium)
                                    }
                                    if (detail.privateNotes.isNotBlank()) {
                                        Text("${VillageStrings.notesLabel(language)}: ${detail.privateNotes}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isAdmin) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onVerifyClick(VerificationStatus.VERIFIED) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(VillageStrings.verify(language))
                    }
                    OutlinedButton(
                        onClick = { onVerifyClick(VerificationStatus.REJECTED) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(VillageStrings.reject(language))
                    }
                }
            }

            if (isOwner || isAdmin) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onEditClick(loadedDetail) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr(language, "संपादित करें", "ସମ୍ପାଦନ କରନ୍ତୁ"))
                    }
                    OutlinedButton(
                        onClick = onDeleteClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(VillageStrings.delete(language))
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}
