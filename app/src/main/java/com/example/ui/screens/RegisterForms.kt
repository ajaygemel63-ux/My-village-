package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Family
import com.example.data.model.Gender
import com.example.data.model.IdMaskingUtils
import com.example.data.model.Member
import com.example.data.model.PrivateMemberDetail
import com.example.data.model.RationCategory
import com.example.data.model.RecordVisibility
import com.example.data.model.RelationToHead
import com.example.data.model.VillageConfig
import com.example.localization.AppLanguage
import com.example.localization.VillageStrings
import com.example.localization.tr
import com.example.ui.theme.JetBrainsMonoFontFamily

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MemberFormScreen(
    language: AppLanguage,
    wards: List<String>,
    families: List<Family>,
    editingMember: Member?,
    initialPrivateDetail: PrivateMemberDetail?,
    prefilledFamily: Family?,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (
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
        privateNotes: String
    ) -> Unit
) {
    BackHandler(onBack = onDismiss)

    val safeWards = wards.ifEmpty { VillageConfig.DEFAULT_WARDS }
    var fullName by remember(editingMember) { mutableStateOf(editingMember?.fullName ?: "") }
    var fullNameLocal by remember(editingMember) { mutableStateOf(editingMember?.fullNameLocal ?: "") }
    var guardianName by remember(editingMember) { mutableStateOf(editingMember?.guardianName ?: "") }
    var ageText by remember(editingMember) { mutableStateOf(editingMember?.age?.takeIf { it > 0 }?.toString() ?: "") }
    var selectedGender by remember(editingMember) {
        mutableStateOf(Gender.fromRaw(editingMember?.gender))
    }
    var selectedWard by remember(editingMember, prefilledFamily) {
        mutableStateOf(editingMember?.ward ?: prefilledFamily?.ward ?: safeWards.first())
    }
    var occupation by remember(editingMember) { mutableStateOf(editingMember?.occupation ?: "") }
    var selectedFamilyId by remember(editingMember, prefilledFamily) {
        mutableStateOf(editingMember?.familyId ?: prefilledFamily?.id ?: "")
    }
    var selectedFamilyCode by remember(editingMember, prefilledFamily) {
        mutableStateOf(editingMember?.familyCode ?: prefilledFamily?.familyCode ?: "")
    }
    var selectedRelation by remember(editingMember) {
        mutableStateOf(RelationToHead.fromRaw(editingMember?.relationToHead))
    }
    var rawAadhaar by remember(editingMember) { mutableStateOf("") }
    var rawVoterId by remember(editingMember) { mutableStateOf("") }
    var selectedVisibility by remember(editingMember) {
        mutableStateOf(RecordVisibility.fromRaw(editingMember?.visibility))
    }

    var phoneNumber by remember(initialPrivateDetail) { mutableStateOf(initialPrivateDetail?.phoneNumber ?: "") }
    var fullAddress by remember(initialPrivateDetail) { mutableStateOf(initialPrivateDetail?.fullAddress ?: "") }
    var emergencyContact by remember(initialPrivateDetail) { mutableStateOf(initialPrivateDetail?.emergencyContact ?: "") }
    var privateNotes by remember(initialPrivateDetail) { mutableStateOf(initialPrivateDetail?.privateNotes ?: "") }

    var wardMenuExpanded by remember { mutableStateOf(false) }
    var familyMenuExpanded by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val aadhaarPreview = remember(rawAadhaar, editingMember) {
        if (rawAadhaar.isNotBlank()) IdMaskingUtils.maskAadhaar(rawAadhaar).first
        else editingMember?.aadhaarMasked ?: ""
    }
    val voterPreview = remember(rawVoterId, editingMember) {
        if (rawVoterId.isNotBlank()) IdMaskingUtils.maskVoterId(rawVoterId).first
        else editingMember?.voterIdMasked ?: ""
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (editingMember == null) VillageStrings.registerMember(language)
                        else VillageStrings.editMember(language)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = VillageStrings.cancel(language))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            validationError?.let { err ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text(VillageStrings.fullName(language)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("member_fullname_input")
            )

            OutlinedTextField(
                value = fullNameLocal,
                onValueChange = { fullNameLocal = it },
                label = { Text(VillageStrings.localName(language)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = guardianName,
                onValueChange = { guardianName = it },
                label = { Text(VillageStrings.guardianName(language)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("member_guardian_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = ageText,
                    onValueChange = { input -> ageText = input.filter { it.isDigit() }.take(3) },
                    label = { Text(VillageStrings.age(language)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = occupation,
                    onValueChange = { occupation = it },
                    label = { Text(VillageStrings.occupation(language)) },
                    singleLine = true,
                    modifier = Modifier.weight(1.4f)
                )
            }

            Text(
                text = VillageStrings.gender(language),
                style = MaterialTheme.typography.labelLarge
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Gender.entries.forEach { gender ->
                    FilterChip(
                        selected = selectedGender == gender,
                        onClick = { selectedGender = gender },
                        label = { Text(gender.label(language)) }
                    )
                }
            }

            // Ward selector
            ExposedDropdownMenuBox(
                expanded = wardMenuExpanded,
                onExpandedChange = { wardMenuExpanded = !wardMenuExpanded }
            ) {
                OutlinedTextField(
                    value = selectedWard,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(VillageStrings.ward(language)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = wardMenuExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = wardMenuExpanded,
                    onDismissRequest = { wardMenuExpanded = false }
                ) {
                    safeWards.forEach { w ->
                        DropdownMenuItem(
                            text = { Text(w) },
                            onClick = {
                                selectedWard = w
                                wardMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Family selector
            ExposedDropdownMenuBox(
                expanded = familyMenuExpanded,
                onExpandedChange = { familyMenuExpanded = !familyMenuExpanded }
            ) {
                val familyLabel = families.firstOrNull { it.id == selectedFamilyId }?.let {
                    "${it.familyCode} — ${it.headName}"
                } ?: tr(language, "कोई परिवार लिंक नहीं", "କୌଣସି ପରିବାର ଲିଙ୍କ୍ ନାହିଁ")
                OutlinedTextField(
                    value = familyLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(VillageStrings.selectFamily(language)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = familyMenuExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = familyMenuExpanded,
                    onDismissRequest = { familyMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(tr(language, "कोई नहीं", "କିଛି ନାହିଁ")) },
                        onClick = {
                            selectedFamilyId = ""
                            selectedFamilyCode = ""
                            familyMenuExpanded = false
                        }
                    )
                    families.forEach { fam ->
                        DropdownMenuItem(
                            text = { Text("${fam.familyCode} — ${fam.headName} (${fam.ward})") },
                            onClick = {
                                selectedFamilyId = fam.id
                                selectedFamilyCode = fam.familyCode
                                familyMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Text(
                text = VillageStrings.relationToHead(language),
                style = MaterialTheme.typography.labelLarge
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                RelationToHead.entries.forEach { rel ->
                    FilterChip(
                        selected = selectedRelation == rel,
                        onClick = { selectedRelation = rel },
                        label = { Text(rel.label(language)) }
                    )
                }
            }

            // Masked IDs section
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = tr(language, "मास्क्ड पहचान पत्र (गोपनीयता सुरक्षित)", "ମାସ୍କ ପରିଚୟ ପତ୍ର (ଗୋପନୀୟତା ସୁରକ୍ଷିତ)"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = VillageStrings.maskingNotice(language),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = rawAadhaar,
                        onValueChange = { input -> rawAadhaar = input.filter { it.isDigit() }.take(12) },
                        label = { Text(VillageStrings.aadhaarInput(language)) },
                        placeholder = { Text("12-digit or last 4 digits") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("member_aadhaar_input")
                    )
                    if (aadhaarPreview.isNotBlank()) {
                        Text(
                            text = "Stored Aadhaar: $aadhaarPreview",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    OutlinedTextField(
                        value = rawVoterId,
                        onValueChange = { input ->
                            rawVoterId = input.uppercase().filter { it.isLetterOrDigit() }.take(12)
                        },
                        label = { Text(VillageStrings.voterInput(language)) },
                        placeholder = { Text("e.g. ODJ1234567") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("member_voter_input")
                    )
                    if (voterPreview.isNotBlank()) {
                        Text(
                            text = "Stored Voter ID: $voterPreview",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Record Visibility
            Text(
                text = VillageStrings.visibilityLabel(language),
                style = MaterialTheme.typography.labelLarge
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = selectedVisibility == RecordVisibility.PUBLIC,
                    onClick = { selectedVisibility = RecordVisibility.PUBLIC },
                    leadingIcon = { Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text(VillageStrings.publicRecord(language)) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedVisibility == RecordVisibility.PRIVATE,
                    onClick = { selectedVisibility = RecordVisibility.PRIVATE },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text(VillageStrings.privateRecord(language)) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Isolated Private Details Section
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = VillageStrings.privateDetailsHeader(language),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it.take(20) },
                        label = { Text(VillageStrings.phoneLabel(language)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = fullAddress,
                        onValueChange = { fullAddress = it.take(250) },
                        label = { Text(VillageStrings.addressLabel(language)) },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = emergencyContact,
                        onValueChange = { emergencyContact = it.take(100) },
                        label = { Text(VillageStrings.emergencyLabel(language)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = privateNotes,
                        onValueChange = { privateNotes = it.take(500) },
                        label = { Text(VillageStrings.notesLabel(language)) },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val age = ageText.toLongOrNull()
                    when {
                        fullName.trim().length < 2 -> {
                            validationError = tr(language, "कृपया कम से कम 2 अक्षर का नाम लिखें", "ଦୟାକରି ଅତି କମରେ ୨ ଅକ୍ଷରର ନାମ ଲେଖନ୍ତୁ")
                        }
                        guardianName.trim().isEmpty() -> {
                            validationError = tr(language, "कृपया पिता/पति का नाम लिखें", "ଦୟାକରି ପିତା/ସ୍ୱାମୀଙ୍କ ନାମ ଲେଖନ୍ତୁ")
                        }
                        age == null || age !in 0L..120L -> {
                            validationError = tr(language, "कृपया सही आयु (0-120) लिखें", "ଦୟାକରି ସଠିକ୍ ବୟସ (0-120) ଲେଖନ୍ତୁ")
                        }
                        else -> {
                            validationError = null
                            onSubmit(
                                selectedFamilyId,
                                selectedFamilyCode,
                                fullName,
                                fullNameLocal,
                                guardianName,
                                selectedRelation,
                                age,
                                selectedGender,
                                selectedWard,
                                occupation,
                                rawAadhaar,
                                rawVoterId,
                                selectedVisibility,
                                phoneNumber,
                                fullAddress,
                                emergencyContact,
                                privateNotes
                            )
                        }
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_member_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(VillageStrings.save(language))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FamilyFormScreen(
    language: AppLanguage,
    wards: List<String>,
    editingFamily: Family?,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (
        familyCode: String,
        headName: String,
        headNameLocal: String,
        ward: String,
        houseNumber: String,
        rationCategory: RationCategory,
        memberCount: Long,
        visibility: RecordVisibility
    ) -> Unit
) {
    BackHandler(onBack = onDismiss)

    val safeWards = wards.ifEmpty { VillageConfig.DEFAULT_WARDS }
    var familyCode by remember(editingFamily) { mutableStateOf(editingFamily?.familyCode ?: "FAM-") }
    var headName by remember(editingFamily) { mutableStateOf(editingFamily?.headName ?: "") }
    var headNameLocal by remember(editingFamily) { mutableStateOf(editingFamily?.headNameLocal ?: "") }
    var selectedWard by remember(editingFamily) { mutableStateOf(editingFamily?.ward ?: safeWards.first()) }
    var houseNumber by remember(editingFamily) { mutableStateOf(editingFamily?.houseNumber ?: "") }
    var rationCategory by remember(editingFamily) {
        mutableStateOf(RationCategory.fromRaw(editingFamily?.rationCardCategory))
    }
    var memberCountText by remember(editingFamily) {
        mutableStateOf(editingFamily?.memberCount?.toString() ?: "1")
    }
    var visibility by remember(editingFamily) {
        mutableStateOf(RecordVisibility.fromRaw(editingFamily?.visibility))
    }
    var wardExpanded by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (editingFamily == null) VillageStrings.registerFamily(language)
                        else VillageStrings.editFamily(language)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = VillageStrings.cancel(language))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            validationError?.let { err ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            OutlinedTextField(
                value = familyCode,
                onValueChange = { familyCode = it.uppercase().take(30) },
                label = { Text(tr(language, "परिवार कोड (जैसे FAM-101)", "ପରିବାର କୋଡ୍ (ଯେପରି FAM-101)")) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("family_code_input")
            )
            OutlinedTextField(
                value = headName,
                onValueChange = { headName = it },
                label = { Text(tr(language, "परिवार के मुखिया का नाम (English)", "ପରିବାର ମୁଖିଆଙ୍କ ନାମ (English)")) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("family_head_input")
            )
            OutlinedTextField(
                value = headNameLocal,
                onValueChange = { headNameLocal = it },
                label = { Text(VillageStrings.localName(language)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = wardExpanded,
                onExpandedChange = { wardExpanded = !wardExpanded }
            ) {
                OutlinedTextField(
                    value = selectedWard,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(VillageStrings.ward(language)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = wardExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = wardExpanded,
                    onDismissRequest = { wardExpanded = false }
                ) {
                    safeWards.forEach { w ->
                        DropdownMenuItem(
                            text = { Text(w) },
                            onClick = {
                                selectedWard = w
                                wardExpanded = false
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = houseNumber,
                    onValueChange = { houseNumber = it.take(30) },
                    label = { Text(tr(language, "मकान नं.", "ଘର ନଂ")) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = memberCountText,
                    onValueChange = { memberCountText = it.filter { ch -> ch.isDigit() }.take(2) },
                    label = { Text(tr(language, "सदस्य संख्या", "ସଦସ୍ୟ ସଂଖ୍ୟା")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = tr(language, "राशन कार्ड श्रेणी", "ରାସନ କାର୍ଡ ଶ୍ରେଣୀ"),
                style = MaterialTheme.typography.labelLarge
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                RationCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = rationCategory == cat,
                        onClick = { rationCategory = cat },
                        label = { Text(cat.label(language)) }
                    )
                }
            }

            Text(
                text = VillageStrings.visibilityLabel(language),
                style = MaterialTheme.typography.labelLarge
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = visibility == RecordVisibility.PUBLIC,
                    onClick = { visibility = RecordVisibility.PUBLIC },
                    leadingIcon = { Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text(VillageStrings.publicRecord(language)) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = visibility == RecordVisibility.PRIVATE,
                    onClick = { visibility = RecordVisibility.PRIVATE },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text(VillageStrings.privateRecord(language)) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val count = memberCountText.toLongOrNull()?.coerceIn(1L, 50L)
                    when {
                        familyCode.trim().length < 2 -> {
                            validationError = tr(language, "परिवार कोड कम से कम 2 अक्षर का हो", "ପରିବାର କୋଡ୍ ଅତି କମରେ ୨ ଅକ୍ଷର ହେବା ଉଚିତ")
                        }
                        headName.trim().length < 2 -> {
                            validationError = tr(language, "मुखिया का नाम कम से कम 2 अक्षर का हो", "ମୁଖିଆଙ୍କ ନାମ ଅତି କମରେ ୨ ଅକ୍ଷର ହେବା ଉଚିତ")
                        }
                        count == null -> {
                            validationError = tr(language, "सदस्य संख्या 1 से 50 के बीच लिखें", "ସଦସ୍ୟ ସଂଖ୍ୟା 1 ରୁ 50 ମଧ୍ୟରେ ଲେଖନ୍ତୁ")
                        }
                        else -> {
                            validationError = null
                            onSubmit(
                                familyCode,
                                headName,
                                headNameLocal,
                                selectedWard,
                                houseNumber,
                                rationCategory,
                                count,
                                visibility
                            )
                        }
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_family_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(VillageStrings.save(language))
                }
            }
        }
    }
}

@Composable
fun RequestAccessDialog(
    language: AppLanguage,
    targetName: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (reason: String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(VillageStrings.requestAccess(language)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = targetName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = tr(
                        language,
                        "रिकॉर्ड मालिक या एडमिन से प्राइवेट विवरण देखने का कारण लिखें:",
                        "ରେକର୍ଡ ମାଲିକ କିମ୍ବା ଆଡମିନ୍ ଙ୍କ ଠାରୁ ପ୍ରାଇଭେଟ୍ ବିବରଣୀ ଦେଖିବାର କାରଣ ଲେଖନ୍ତୁ:"
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it.take(250) },
                    label = { Text(tr(language, "कारण (कम से कम 3 अक्षर)", "କାରଣ (ଅତି କମରେ ୩ ଅକ୍ଷର)")) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason) },
                enabled = !isSubmitting && reason.trim().length >= 3
            ) {
                Text(tr(language, "भेजें", "ପଠାନ୍ତୁ"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(VillageStrings.cancel(language))
            }
        }
    )
}
