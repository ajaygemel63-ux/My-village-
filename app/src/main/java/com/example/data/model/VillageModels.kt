package com.example.data.model

import androidx.annotation.Keep
import com.example.localization.AppLanguage
import com.example.localization.tr
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class RecordVisibility {
    PUBLIC, PRIVATE;

    fun label(lang: AppLanguage): String = when (this) {
        PUBLIC -> tr(lang, "सार्वजनिक", "ସାର୍ବଜନୀନ")
        PRIVATE -> tr(lang, "प्राइवेट", "ପ୍ରାଇଭେଟ୍")
    }

    companion object {
        fun fromRaw(raw: String?): RecordVisibility =
            entries.firstOrNull { it.name == raw } ?: PUBLIC
    }
}

enum class VerificationStatus {
    PENDING, VERIFIED, REJECTED;

    fun label(lang: AppLanguage): String = when (this) {
        PENDING -> tr(lang, "लंबित", "ବିଚାରାଧୀନ")
        VERIFIED -> tr(lang, "सत्यापित", "ଯାଞ୍ଚିତ")
        REJECTED -> tr(lang, "अस्वीकृत", "ପ୍ରତ୍ୟାଖ୍ୟାତ")
    }

    companion object {
        fun fromRaw(raw: String?): VerificationStatus =
            entries.firstOrNull { it.name == raw } ?: PENDING
    }
}

enum class RelationToHead {
    HEAD, SPOUSE, SON, DAUGHTER, PARENT, OTHER;

    fun label(lang: AppLanguage): String = when (this) {
        HEAD -> tr(lang, "मुखिया", "ମୁଖିଆ")
        SPOUSE -> tr(lang, "जीवनसाथी", "ଜୀବନସାଥୀ")
        SON -> tr(lang, "पुत्र", "ପୁଅ")
        DAUGHTER -> tr(lang, "पुत्री", "ଝିଅ")
        PARENT -> tr(lang, "माता-पिता", "ପିତାମାତା")
        OTHER -> tr(lang, "अन्य", "ଅନ୍ୟ")
    }

    companion object {
        fun fromRaw(raw: String?): RelationToHead =
            entries.firstOrNull { it.name == raw } ?: OTHER
    }
}

enum class Gender {
    MALE, FEMALE, OTHER;

    fun label(lang: AppLanguage): String = when (this) {
        MALE -> tr(lang, "पुरुष", "ପୁରୁଷ")
        FEMALE -> tr(lang, "महिला", "ମହିଳା")
        OTHER -> tr(lang, "अन्य", "ଅନ୍ୟ")
    }

    companion object {
        fun fromRaw(raw: String?): Gender =
            entries.firstOrNull { it.name == raw } ?: OTHER
    }
}

enum class RationCategory {
    APL, BPL, AAY, NONE;

    fun label(lang: AppLanguage): String = when (this) {
        APL -> "APL"
        BPL -> "BPL"
        AAY -> "AAY (Antyodaya)"
        NONE -> tr(lang, "कोई नहीं", "କିଛି ନାହିଁ")
    }

    companion object {
        fun fromRaw(raw: String?): RationCategory =
            entries.firstOrNull { it.name == raw } ?: NONE
    }
}

enum class AccessRequestStatus {
    PENDING, APPROVED, REJECTED, REVOKED;

    fun label(lang: AppLanguage): String = when (this) {
        PENDING -> tr(lang, "लंबित", "ବିଚାରାଧୀନ")
        APPROVED -> tr(lang, "स्वीकृत", "ଅନୁମୋଦିତ")
        REJECTED -> tr(lang, "अस्वीकृत", "ପ୍ରତ୍ୟାଖ୍ୟାତ")
        REVOKED -> tr(lang, "निरस्त", "ପ୍ରତ୍ୟାହୃତ")
    }

    companion object {
        fun fromRaw(raw: String?): AccessRequestStatus =
            entries.firstOrNull { it.name == raw } ?: PENDING
    }
}

enum class AccessTargetType {
    MEMBER, FAMILY;

    companion object {
        fun fromRaw(raw: String?): AccessTargetType =
            entries.firstOrNull { it.name == raw } ?: MEMBER
    }
}

@Keep
@IgnoreExtraProperties
data class VillageConfig(
    @DocumentId val id: String = "",
    val villageNameHi: String = "सुंदरपुर ग्राम",
    val villageNameOr: String = "ସୁନ୍ଦରପୁର ଗାଁ",
    val villageNameEn: String = "My Village",
    val district: String = "",
    val stateName: String = "",
    val wards: List<String> = DEFAULT_WARDS,
    val updatedByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun displayName(lang: AppLanguage): String = when (lang) {
        AppLanguage.HINDI -> villageNameHi.ifBlank { villageNameEn }
        AppLanguage.ODIA -> villageNameOr.ifBlank { villageNameEn }
        AppLanguage.BILINGUAL -> "${villageNameHi.ifBlank { villageNameEn }} • ${villageNameOr.ifBlank { villageNameEn }}"
    }

    companion object {
        val DEFAULT_WARDS = listOf(
            "Ward 1 / वार्ड 1 / ୱାର୍ଡ ୧",
            "Ward 2 / वार्ड 2 / ୱାର୍ଡ ୨",
            "Ward 3 / वार्ड 3 / ୱାର୍ଡ ୩",
            "Ward 4 / वार्ड 4 / ୱାର୍ଡ ୪",
            "Ward 5 / वार्ड 5 / ୱାର୍ଡ ୫"
        )
    }
}

@Keep
@IgnoreExtraProperties
data class AdminRole(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val roleLabel: String = "ADMIN",
    val addedByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val preferredLanguage: String = AppLanguage.BILINGUAL.name,
    val ward: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Family(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val familyCode: String = "",
    val headName: String = "",
    val headNameLocal: String = "",
    val ward: String = "",
    val houseNumber: String = "",
    val rationCardCategory: String = RationCategory.NONE.name,
    val memberCount: Long = 1L,
    val visibility: String = RecordVisibility.PUBLIC.name,
    val authorizedViewerUids: List<String> = emptyList(),
    val verificationStatus: String = VerificationStatus.PENDING.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Member(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val familyId: String = "",
    val familyCode: String = "",
    val fullName: String = "",
    val fullNameLocal: String = "",
    val guardianName: String = "",
    val relationToHead: String = RelationToHead.HEAD.name,
    val age: Long = 0L,
    val gender: String = Gender.MALE.name,
    val ward: String = "",
    val occupation: String = "",
    val aadhaarMasked: String = "",
    val aadhaarLast4: String = "",
    val voterIdMasked: String = "",
    val voterIdLast4: String = "",
    val visibility: String = RecordVisibility.PUBLIC.name,
    val authorizedViewerUids: List<String> = emptyList(),
    val verificationStatus: String = VerificationStatus.PENDING.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PrivateMemberDetail(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val authorizedViewerUids: List<String> = emptyList(),
    val phoneNumber: String = "",
    val fullAddress: String = "",
    val emergencyContact: String = "",
    val privateNotes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AccessRequest(
    @DocumentId val id: String = "",
    val requesterUid: String = "",
    val requesterName: String = "",
    val targetType: String = AccessTargetType.MEMBER.name,
    val targetId: String = "",
    val targetName: String = "",
    val targetOwnerUid: String = "",
    val reason: String = "",
    val status: String = AccessRequestStatus.PENDING.name,
    val reviewedByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

object IdMaskingUtils {
    /**
     * Masks an Aadhaar number (either full 12 digits or already-masked / last-4)
     * into "XXXX-XXXX-1234" and extracts the last 4 digits. Never retains the full 12 digits.
     */
    fun maskAadhaar(rawInput: String): Pair<String, String> {
        val digits = rawInput.filter { it.isDigit() }
        if (digits.length < 4) return "" to ""
        val last4 = digits.takeLast(4)
        return "XXXX-XXXX-$last4" to last4
    }

    /**
     * Masks a Voter ID (EPIC) such as "ODJ1234567" into "ODJ••••567" and last 4 chars.
     */
    fun maskVoterId(rawInput: String): Pair<String, String> {
        val clean = rawInput.trim().uppercase().filter { it.isLetterOrDigit() }
        if (clean.length < 3) return "" to ""
        val last4 = clean.takeLast(4)
        val masked = if (clean.length >= 6) {
            "${clean.take(3)}••••${clean.takeLast(3)}"
        } else {
            "••••${clean.takeLast(3)}"
        }
        return masked to last4
    }
}
