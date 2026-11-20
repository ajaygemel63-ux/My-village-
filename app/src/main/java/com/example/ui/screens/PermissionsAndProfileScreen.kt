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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AccessRequest
import com.example.data.model.AccessRequestStatus
import com.example.localization.AppLanguage
import com.example.localization.VillageStrings
import com.example.localization.tr
import com.example.ui.components.LanguageToggleBar
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusRejected
import com.example.ui.theme.StatusVerified
import com.example.ui.viewmodel.VillageDashboardData

@Composable
fun PermissionsAndProfileScreen(
    data: VillageDashboardData,
    currentUserId: String,
    userName: String?,
    userEmail: String?,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onApproveRequest: (AccessRequest) -> Unit,
    onRejectRequest: (AccessRequest) -> Unit,
    onRevokeAccess: (AccessRequest) -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // User Profile & Role Card
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
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Icon(
                                imageVector = if (data.isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .padding(12.dp)
                                    .size(28.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userName?.ifBlank { null } ?: tr(language, "ग्रामीण सदस्य", "ଗ୍ରାମବାସୀ ସଦସ୍ୟ"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = userEmail ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (data.isAdmin) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = if (data.isAdmin) "ADMIN" else tr(language, "ग्रामीण", "ଗ୍ରାମବାସୀ"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Text(
                        text = "UID: $currentUserId",
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tr(language, "भाषा / ଭାଷା", "ଭାଷା / भाषा"),
                            style = MaterialTheme.typography.labelLarge
                        )
                        LanguageToggleBar(selected = language, onSelect = onLanguageChange)
                    }

                    OutlinedButton(
                        onClick = onSignOutClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(VillageStrings.signOut(language))
                    }
                }
            }
        }

        // Incoming Permission Requests for records I own
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "${VillageStrings.incomingRequests(language)} (${data.incomingRequests.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (data.incomingRequests.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = tr(
                            language,
                            "आपके प्राइवेट रिकॉर्ड के लिए कोई अनुमति अनुरोध नहीं आया है।",
                            "ଆପଣଙ୍କ ପ୍ରାଇଭେଟ୍ ରେକର୍ଡ ପାଇଁ କୌଣସି ଅନୁମତି ଅନୁରୋଧ ଆସିନାହିଁ।"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(data.incomingRequests, key = { "in_${it.id}" }) { req ->
                PermissionRequestCard(
                    request = req,
                    isIncoming = true,
                    language = language,
                    onApprove = { onApproveRequest(req) },
                    onReject = { onRejectRequest(req) },
                    onRevoke = { onRevokeAccess(req) }
                )
            }
        }

        // Outgoing Permission Requests
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.LockOpen, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Text(
                    text = "${VillageStrings.outgoingRequests(language)} (${data.outgoingRequests.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (data.outgoingRequests.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = tr(
                            language,
                            "आपने अभी तक कोई प्राइवेट अनुमति अनुरोध नहीं भेजा है।",
                            "ଆପଣ ଏପର୍ଯ୍ୟନ୍ତ କୌଣସି ପ୍ରାଇଭେଟ୍ ଅନୁମତି ଅନୁରୋଧ ପଠାଇନାହାଁନ୍ତି।"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(data.outgoingRequests, key = { "out_${it.id}" }) { req ->
                PermissionRequestCard(
                    request = req,
                    isIncoming = false,
                    language = language,
                    onApprove = {},
                    onReject = {},
                    onRevoke = {}
                )
            }
        }
    }
}

@Composable
fun PermissionRequestCard(
    request: AccessRequest,
    isIncoming: Boolean,
    language: AppLanguage,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onRevoke: () -> Unit
) {
    val status = AccessRequestStatus.fromRaw(request.status)
    val statusColor = when (status) {
        AccessRequestStatus.APPROVED -> StatusVerified
        AccessRequestStatus.PENDING -> StatusPending
        AccessRequestStatus.REJECTED, AccessRequestStatus.REVOKED -> StatusRejected
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request.targetName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isIncoming) {
                            "${tr(language, "अनुरोधकर्ता", "ଅନୁରୋଧକାରୀ")}: ${request.requesterName}"
                        } else {
                            "${tr(language, "रिकॉर्ड प्रकार", "ରେକର୍ଡ ପ୍ରକାର")}: ${request.targetType}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = status.label(language),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "${tr(language, "कारण", "କାରଣ")}: ${request.reason}",
                style = MaterialTheme.typography.bodySmall
            )

            if (isIncoming) {
                when (status) {
                    AccessRequestStatus.PENDING -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onApprove,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(VillageStrings.approve(language))
                            }
                            OutlinedButton(
                                onClick = onReject,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(VillageStrings.reject(language))
                            }
                        }
                    }
                    AccessRequestStatus.APPROVED -> {
                        OutlinedButton(
                            onClick = onRevoke,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(VillageStrings.revoke(language))
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}
