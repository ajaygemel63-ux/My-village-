package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Family
import com.example.data.model.Member
import com.example.data.model.RationCategory
import com.example.data.model.RecordVisibility
import com.example.data.model.RelationToHead
import com.example.data.model.VerificationStatus
import com.example.localization.AppLanguage
import com.example.localization.VillageStrings
import com.example.localization.tr
import com.example.ui.components.PrivacyBadge
import com.example.ui.components.VerificationBadge
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.VillageDashboardData

@Composable
fun FamiliesScreen(
    data: VillageDashboardData,
    currentUserId: String,
    language: AppLanguage,
    onFamilyClick: (Family) -> Unit,
    onAddFamilyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    val filteredFamilies = remember(data.families, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) data.families
        else data.families.filter { f ->
            f.familyCode.lowercase().contains(q) ||
                f.headName.lowercase().contains(q) ||
                f.headNameLocal.lowercase().contains(q) ||
                f.ward.lowercase().contains(q) ||
                f.houseNumber.lowercase().contains(q)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = tr(language, "गाँव के पारिवारिक रिकॉर्ड", "ଗାଁର ପାରିବାରିକ ରେକର୍ଡ"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                placeholder = {
                    Text(tr(language, "परिवार कोड, मुखिया नाम या वार्ड खोजें...", "ପରିବାର କୋଡ୍, ମୁଖିଆଙ୍କ ନାମ କିମ୍ବା ୱାର୍ଡ ଖୋଜନ୍ତୁ..."))
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("family_search_input")
            )
        }

        if (filteredFamilies.isEmpty()) {
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
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(42.dp)
                        )
                        Text(
                            text = tr(language, "कोई परिवार रिकॉर्ड नहीं मिला", "କୌଣସି ପରିବାର ରେକର୍ଡ ମିଳିଲା ନାହିଁ"),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Button(onClick = onAddFamilyClick) {
                            Text(VillageStrings.registerFamily(language))
                        }
                    }
                }
            }
        } else {
            items(filteredFamilies, key = { it.id }) { family ->
                val linkedCount = data.members.count { it.familyId == family.id }
                FamilyRecordCard(
                    family = family,
                    linkedMembersCount = linkedCount,
                    currentUserId = currentUserId,
                    language = language,
                    onClick = { onFamilyClick(family) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FamilyRecordCard(
    family: Family,
    linkedMembersCount: Int,
    currentUserId: String,
    language: AppLanguage,
    onClick: () -> Unit
) {
    val isSharedViewer = currentUserId in family.authorizedViewerUids && family.ownerUid != currentUserId
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("family_card_${family.id}"),
        shape = RoundedCornerShape(20.dp)
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
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = family.familyCode,
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = family.headName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (family.headNameLocal.isNotBlank()) {
                        Text(
                            text = family.headNameLocal,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    VerificationBadge(
                        status = VerificationStatus.fromRaw(family.verificationStatus),
                        lang = language
                    )
                    PrivacyBadge(
                        visibility = RecordVisibility.fromRaw(family.visibility),
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
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(13.dp))
                        Text(
                            text = family.ward.substringBefore("/").trim(),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                if (family.houseNumber.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(13.dp))
                            Text(
                                text = "H.No ${family.houseNumber}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = "${tr(language, "राशन", "ରାସନ")}: ${RationCategory.fromRaw(family.rationCardCategory).label(language)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "${tr(language, "सदस्य", "ସଦସ୍ୟ")}: $linkedMembersCount/${family.memberCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyDetailBottomSheet(
    family: Family,
    linkedMembers: List<Member>,
    currentUserId: String,
    isAdmin: Boolean,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onAddMemberToFamily: (Family) -> Unit,
    onMemberClick: (Member) -> Unit,
    onEditFamily: (Family) -> Unit,
    onDeleteFamily: (Family) -> Unit
) {
    val canEdit = family.ownerUid == currentUserId || isAdmin

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
                        text = "${family.familyCode} — ${family.headName}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (family.headNameLocal.isNotBlank()) {
                        Text(
                            text = family.headNameLocal,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "${family.ward} • ${tr(language, "मकान", "ଘର")}: ${family.houseNumber.ifBlank { "-" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                VerificationBadge(
                    status = VerificationStatus.fromRaw(family.verificationStatus),
                    lang = language
                )
            }

            Button(
                onClick = { onAddMemberToFamily(family) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(tr(language, "इस परिवार में सदस्य जोड़ें", "ଏହି ପରିବାରରେ ସଦସ୍ୟ ଯୋଡନ୍ତୁ"))
            }

            HorizontalDivider()

            Text(
                text = "${tr(language, "परिवार के सदस्य", "ପରିବାର ସଦସ୍ୟ")} (${linkedMembers.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            if (linkedMembers.isEmpty()) {
                Text(
                    text = tr(
                        language,
                        "इस परिवार में अभी कोई सदस्य लिंक नहीं है।",
                        "ଏହି ପରିବାରରେ ଏପର୍ଯ୍ୟନ୍ତ କୌଣସି ସଦସ୍ୟ ଲିଙ୍କ୍ ହୋଇନାହାଁନ୍ତି।"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                linkedMembers.forEach { member ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMemberClick(member) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = member.fullName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${RelationToHead.fromRaw(member.relationToHead).label(language)} • ${member.age}y • ${member.aadhaarMasked}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (canEdit) {
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onEditFamily(family) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr(language, "संपादित करें", "ସମ୍ପାଦନ"))
                    }
                    OutlinedButton(
                        onClick = { onDeleteFamily(family) },
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
