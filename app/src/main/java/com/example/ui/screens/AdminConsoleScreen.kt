package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.model.AccessRequest
import com.example.data.model.VerificationStatus
import com.example.data.model.VillageConfig
import com.example.localization.AppLanguage
import com.example.localization.VillageStrings
import com.example.localization.tr
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.VillageDashboardData

@Composable
fun AdminConsoleScreen(
    data: VillageDashboardData,
    currentUserId: String,
    language: AppLanguage,
    isSubmitting: Boolean,
    onEditVillageClick: () -> Unit,
    onVerifyMember: (String, VerificationStatus) -> Unit,
    onVerifyFamily: (String, VerificationStatus) -> Unit,
    onApproveRequest: (AccessRequest) -> Unit,
    onRejectRequest: (AccessRequest) -> Unit,
    onRevokeRequest: (AccessRequest) -> Unit,
    onAddAdmin: (uid: String, email: String, name: String, roleLabel: String, onDone: (Boolean) -> Unit) -> Unit,
    onRemoveAdmin: (uid: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val pendingMembers = remember(data.members) {
        data.members.filter { it.verificationStatus == VerificationStatus.PENDING.name }
    }
    val pendingFamilies = remember(data.families) {
        data.families.filter { it.verificationStatus == VerificationStatus.PENDING.name }
    }
    var showAddAdminDialog by remember { mutableStateOf(false) }

    if (showAddAdminDialog) {
        AddAdminDialog(
            language = language,
            isSubmitting = isSubmitting,
            onDismiss = { showAddAdminDialog = false },
            onConfirm = { uid, email, name, role ->
                onAddAdmin(uid, email, name, role) { ok ->
                    if (ok) showAddAdminDialog = false
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Village Name & Ward Settings Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.EditLocationAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = VillageStrings.villageSettings(language),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "${data.villageConfig.villageNameHi} • ${data.villageConfig.villageNameOr} (${data.villageConfig.villageNameEn})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${tr(language, "वार्ड संख्या", "ୱାର୍ଡ ସଂଖ୍ୟା")}: ${data.villageConfig.wards.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onEditVillageClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_edit_village_button")
                    ) {
                        Text(VillageStrings.villageSettings(language))
                    }
                }
            }
        }

        // Pending Approvals Header
        item {
            Text(
                text = "${VillageStrings.pendingApprovals(language)} (${pendingMembers.size + pendingFamilies.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (pendingMembers.isEmpty() && pendingFamilies.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = tr(language, "कोई लंबित सत्यापन नहीं है", "କୌଣସି ବିଚାରାଧୀନ ଯାଞ୍ଚ ନାହିଁ"),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(pendingMembers, key = { "pm_${it.id}" }) { member ->
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "${tr(language, "सदस्य", "ସଦସ୍ୟ")}: ${member.fullName}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${member.ward} • ${member.aadhaarMasked} • ${member.voterIdMasked}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onVerifyMember(member.id, VerificationStatus.VERIFIED) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(VillageStrings.verify(language))
                            }
                            OutlinedButton(
                                onClick = { onVerifyMember(member.id, VerificationStatus.REJECTED) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(VillageStrings.reject(language))
                            }
                        }
                    }
                }
            }

            items(pendingFamilies, key = { "pf_${it.id}" }) { family ->
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "${tr(language, "परिवार", "ପରିବାର")}: ${family.familyCode} — ${family.headName}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${family.ward} • ${family.memberCount} members",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onVerifyFamily(family.id, VerificationStatus.VERIFIED) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(VillageStrings.verify(language))
                            }
                            OutlinedButton(
                                onClick = { onVerifyFamily(family.id, VerificationStatus.REJECTED) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(VillageStrings.reject(language))
                            }
                        }
                    }
                }
            }
        }

        // Village-wide Permission Requests Oversight
        item {
            Text(
                text = "${tr(language, "सभी प्राइवेट अनुमति अनुरोध", "ସମସ୍ତ ପ୍ରାଇଭେଟ୍ ଅନୁମତି ଅନୁରୋଧ")} (${data.allRequestsAdmin.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        items(data.allRequestsAdmin.take(20), key = { "ar_${it.id}" }) { req ->
            PermissionRequestCard(
                request = req,
                isIncoming = true,
                language = language,
                onApprove = { onApproveRequest(req) },
                onReject = { onRejectRequest(req) },
                onRevoke = { onRevokeRequest(req) }
            )
        }

        // Admin Roles Manager
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = VillageStrings.manageAdmins(language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(onClick = { showAddAdminDialog = true }) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(tr(language, "एडमिन जोड़ें", "ଆଡମିନ୍ ଯୋଡନ୍ତୁ"))
                }
            }
        }

        items(data.adminRoles, key = { "adm_${it.id}" }) { role ->
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Column {
                            Text("${role.displayName} (${role.roleLabel})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(role.email, style = MaterialTheme.typography.bodySmall)
                            Text(role.uid, fontFamily = JetBrainsMonoFontFamily, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (role.uid != currentUserId) {
                        IconButton(onClick = { onRemoveAdmin(role.uid) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove Admin", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VillageSettingsDialog(
    config: VillageConfig,
    language: AppLanguage,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSave: (nameHi: String, nameOr: String, nameEn: String, district: String, stateName: String, wardsRaw: String) -> Unit
) {
    var nameHi by remember(config) { mutableStateOf(config.villageNameHi) }
    var nameOr by remember(config) { mutableStateOf(config.villageNameOr) }
    var nameEn by remember(config) { mutableStateOf(config.villageNameEn) }
    var district by remember(config) { mutableStateOf(config.district) }
    var stateName by remember(config) { mutableStateOf(config.stateName) }
    var wardsRaw by remember(config) { mutableStateOf(config.wards.joinToString(", ")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(VillageStrings.villageSettings(language)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nameHi,
                    onValueChange = { nameHi = it.take(100) },
                    label = { Text("गाँव का नाम (हिन्दी)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nameOr,
                    onValueChange = { nameOr = it.take(100) },
                    label = { Text("ଗାଁର ନାମ (ଓଡ଼ିଆ)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it.take(100) },
                    label = { Text("Village Name (English)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = district,
                        onValueChange = { district = it.take(100) },
                        label = { Text(tr(language, "जिला", "ଜିଲ୍ଲା")) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = stateName,
                        onValueChange = { stateName = it.take(100) },
                        label = { Text(tr(language, "राज्य", "ରାଜ୍ୟ")) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = wardsRaw,
                    onValueChange = { wardsRaw = it },
                    label = { Text(tr(language, "वार्ड सूची (कॉमा से अलग)", "ୱାର୍ଡ ତାଲିକା (କମା ଦ୍ୱାରା ଅଲଗା)")) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(nameHi, nameOr, nameEn, district, stateName, wardsRaw) },
                enabled = !isSubmitting && nameHi.isNotBlank() && nameOr.isNotBlank() && nameEn.isNotBlank()
            ) {
                Text(VillageStrings.save(language))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(VillageStrings.cancel(language))
            }
        }
    )
}

@Composable
private fun AddAdminDialog(
    language: AppLanguage,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (uid: String, email: String, name: String, roleLabel: String) -> Unit
) {
    var uid by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var roleLabel by remember { mutableStateOf("SARPANCH") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr(language, "नया एडमिन जोड़ें", "ନୂତନ ଆଡମିନ୍ ଯୋଡନ୍ତୁ")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = uid,
                    onValueChange = { uid = it.trim() },
                    label = { Text("Firebase User UID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(100) },
                    label = { Text(tr(language, "नाम", "ନାମ")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.trim().take(200) },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("ADMIN", "SARPANCH", "SECRETARY").forEach { r ->
                        FilterChip(
                            selected = roleLabel == r,
                            onClick = { roleLabel = r },
                            label = { Text(r, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(uid, email, name, roleLabel) },
                enabled = !isSubmitting && uid.isNotBlank() && name.isNotBlank() && email.contains("@")
            ) {
                Text(VillageStrings.save(language))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(VillageStrings.cancel(language))
            }
        }
    )
}
