package com.example.localization

enum class AppLanguage(val labelHi: String, val labelOr: String, val shortTag: String) {
    BILINGUAL("हिन्दी + ଓଡ଼ିଆ", "ହିନ୍ଦୀ + ଓଡ଼ିଆ", "हि + ଓ"),
    HINDI("हिन्दी", "ହିନ୍ଦୀ", "हिन्दी"),
    ODIA("ओड़िया", "ଓଡ଼ିଆ", "ଓଡ଼ିଆ");

    companion object {
        fun fromStored(raw: String?): AppLanguage {
            return entries.firstOrNull { it.name == raw } ?: BILINGUAL
        }
    }
}

fun tr(lang: AppLanguage, hi: String, or: String): String = when (lang) {
    AppLanguage.HINDI -> hi
    AppLanguage.ODIA -> or
    AppLanguage.BILINGUAL -> if (hi == or) hi else "$hi • $or"
}

fun trLines(lang: AppLanguage, hi: String, or: String): String = when (lang) {
    AppLanguage.HINDI -> hi
    AppLanguage.ODIA -> or
    AppLanguage.BILINGUAL -> if (hi == or) hi else "$hi\n$or"
}

object VillageStrings {
    fun appTitle(lang: AppLanguage) = tr(lang, "मेरा गाँव ऐप", "ମୋ ଗାଁ ଆପ୍")
    fun villageDirectory(lang: AppLanguage) = tr(lang, "गाँव निर्देशिका", "ଗ୍ରାମ ନିର୍ଦ୍ଦେଶିକା")
    fun families(lang: AppLanguage) = tr(lang, "परिवार", "ପରିବାର")
    fun permissions(lang: AppLanguage) = tr(lang, "अनुमतियाँ", "ଅନୁମତି")
    fun admin(lang: AppLanguage) = tr(lang, "एडमिन", "ଆଡମିନ୍")

    fun villagerLoginTitle(lang: AppLanguage) = tr(lang, "ग्रामीण लॉगिन", "ଗ୍ରାମବାସୀ ଲଗଇନ୍")
    fun villagerLoginDesc(lang: AppLanguage) = tr(
        lang,
        "गाँव के सदस्य पंजीकरण, पारिवारिक रिकॉर्ड और सुरक्षित खोज के लिए Google से साइन-इन करें।",
        "ଗ୍ରାମ ସଦସ୍ୟ ପଞ୍ଜୀକରଣ, ପାରିବାରିକ ରେକର୍ଡ ଏବଂ ସୁରକ୍ଷିତ ଖୋଜ ପାଇଁ Google ରେ ସାଇନ୍-ଇନ୍ କରନ୍ତୁ।"
    )
    fun adminLoginTitle(lang: AppLanguage) = tr(lang, "एडमिन लॉगिन", "ଆଡମିନ୍ ଲଗଇନ୍")
    fun adminLoginDesc(lang: AppLanguage) = tr(
        lang,
        "सरपंच / ग्राम सचिव / एडमिन सत्यापन, गाँव का नाम और अनुमति प्रबंधन के लिए।",
        "ସରପଞ୍ଚ / ଗ୍ରାମ ସଚିବ / ଆଡମିନ୍ ଯାଞ୍ଚ, ଗାଁର ନାମ ଏବଂ ଅନୁମତି ପରିଚାଳନା ପାଇଁ।"
    )
    fun signInWithGoogle(lang: AppLanguage) = tr(lang, "Google से साइन-इन करें", "Google ରେ ସାଇନ୍-ଇନ୍ କରନ୍ତୁ")
    fun adminSignInButton(lang: AppLanguage) = tr(lang, "एडमिन के रूप में साइन-इन", "ଆଡମିନ୍ ଭାବରେ ସାଇନ୍-ଇନ୍")

    fun searchHint(lang: AppLanguage) = tr(
        lang,
        "नाम, वार्ड, परिवार कोड, Aadhaar/Voter खोजें...",
        "ନାମ, ୱାର୍ଡ, ପରିବାର କୋଡ୍, Aadhaar/Voter ଖୋଜନ୍ତୁ..."
    )
    fun registerMember(lang: AppLanguage) = tr(lang, "नया सदस्य जोड़ें", "ନୂତନ ସଦସ୍ୟ ଯୋଡନ୍ତୁ")
    fun registerFamily(lang: AppLanguage) = tr(lang, "नया परिवार जोड़ें", "ନୂତନ ପରିବାର ଯୋଡନ୍ତୁ")
    fun editMember(lang: AppLanguage) = tr(lang, "सदस्य संपादित करें", "ସଦସ୍ୟ ସମ୍ପାଦନ କରନ୍ତୁ")
    fun editFamily(lang: AppLanguage) = tr(lang, "परिवार संपादित करें", "ପରିବାର ସମ୍ପାଦନ କରନ୍ତୁ")

    fun fullName(lang: AppLanguage) = tr(lang, "पूरा नाम (English)", "ପୂର୍ଣ୍ଣ ନାମ (English)")
    fun localName(lang: AppLanguage) = tr(lang, "स्थानीय भाषा में नाम (हिन्दी/ଓଡ଼ିଆ)", "ଆଞ୍ଚଳିକ ନାମ (ହିନ୍ଦୀ/ଓଡ଼ିଆ)")
    fun guardianName(lang: AppLanguage) = tr(lang, "पिता / पति का नाम", "ପିତା / ସ୍ୱାମୀଙ୍କ ନାମ")
    fun age(lang: AppLanguage) = tr(lang, "आयु (वर्ष)", "ବୟସ (ବର୍ଷ)")
    fun gender(lang: AppLanguage) = tr(lang, "लिंग", "ଲିଙ୍ଗ")
    fun ward(lang: AppLanguage) = tr(lang, "वार्ड / साही", "ୱାର୍ଡ / ସାହି")
    fun occupation(lang: AppLanguage) = tr(lang, "व्यवसाय", "ବୃତ୍ତି")
    fun relationToHead(lang: AppLanguage) = tr(lang, "मुखिया से संबंध", "ମୁଖିଆଙ୍କ ସହ ସମ୍ପର୍କ")
    fun selectFamily(lang: AppLanguage) = tr(lang, "परिवार चुनें (वैकल्पिक)", "ପରିବାର ବାଛନ୍ତୁ (ଇଚ୍ଛାଧୀନ)")

    fun aadhaarInput(lang: AppLanguage) = tr(
        lang,
        "आधार नंबर (स्वतः मास्क होगा)",
        "ଆଧାର ନମ୍ବର (ସ୍ୱୟଂଚାଳିତ ମାସ୍କ ହେବ)"
    )
    fun voterInput(lang: AppLanguage) = tr(
        lang,
        "वोटर आईडी / EPIC (स्वतः मास्क होगा)",
        "ଭୋଟର ଆଇଡି / EPIC (ସ୍ୱୟଂଚାଳିତ ମାସ୍କ ହେବ)"
    )
    fun maskingNotice(lang: AppLanguage) = tr(
        lang,
        "सुरक्षा नियम: पूरा आधार और वोटर आईडी कभी सहेजा नहीं जाता—केवल मास्क रूप (XXXX-XXXX-1234) सहेजा जाता है।",
        "ସୁରକ୍ଷା ନିୟମ: ସମ୍ପୂର୍ଣ୍ଣ ଆଧାର ଏବଂ ଭୋଟର ଆଇଡି କେବେବି ସେଭ୍ ହୁଏ ନାହିଁ—କେବଳ ମାସ୍କ ରୂପ (XXXX-XXXX-1234) ସେଭ୍ ହୁଏ।"
    )

    fun visibilityLabel(lang: AppLanguage) = tr(lang, "रिकॉर्ड गोपनीयता", "ରେକର୍ଡ ଗୋପନୀୟତା")
    fun publicRecord(lang: AppLanguage) = tr(lang, "सार्वजनिक (गाँव सूची)", "ସାର୍ବଜନୀନ (ଗ୍ରାମ ତାଲିକା)")
    fun privateRecord(lang: AppLanguage) = tr(lang, "प्राइवेट (केवल अनुमति प्राप्त)", "ପ୍ରାଇଭେଟ୍ (କେବଳ ଅନୁମତିପ୍ରାପ୍ତ)")
    fun sharedRecord(lang: AppLanguage) = tr(lang, "मेरे साथ साझा", "ମୋ ସହ ସେୟାର ହୋଇଥିବା")

    fun privateDetailsHeader(lang: AppLanguage) = tr(
        lang,
        "प्राइवेट संपर्क विवरण (सुरक्षित)",
        "ପ୍ରାଇଭେଟ୍ ଯୋଗାଯୋଗ ବିବରଣୀ (ସୁରକ୍ଷିତ)"
    )
    fun phoneLabel(lang: AppLanguage) = tr(lang, "मोबाइल नंबर", "ମୋବାଇଲ୍ ନମ୍ବର")
    fun addressLabel(lang: AppLanguage) = tr(lang, "पूरा पता", "ସମ୍ପୂର୍ଣ୍ଣ ଠିକଣା")
    fun emergencyLabel(lang: AppLanguage) = tr(lang, "आपातकालीन संपर्क", "ଜରୁରୀକାଳୀନ ଯୋଗାଯୋଗ")
    fun notesLabel(lang: AppLanguage) = tr(lang, "निजी टिप्पणी", "ବ୍ୟକ୍ତିଗତ ଟିପ୍ପଣୀ")

    fun save(lang: AppLanguage) = tr(lang, "सहेजें", "ସେଭ୍ କରନ୍ତୁ")
    fun cancel(lang: AppLanguage) = tr(lang, "रद्द करें", "ବାତିଲ୍ କରନ୍ତୁ")
    fun delete(lang: AppLanguage) = tr(lang, "हटाएँ", "ବିଲୋପ କରନ୍ତୁ")
    fun approve(lang: AppLanguage) = tr(lang, "अनुमति दें", "ଅନୁମତି ଦିଅନ୍ତୁ")
    fun reject(lang: AppLanguage) = tr(lang, "अस्वीकार", "ପ୍ରତ୍ୟାଖ୍ୟାନ")
    fun revoke(lang: AppLanguage) = tr(lang, "अनुमति वापस लें", "ଅନୁମତି ପ୍ରତ୍ୟାହାର")
    fun verify(lang: AppLanguage) = tr(lang, "सत्यापित करें", "ଯାଞ୍ଚ କରନ୍ତୁ")
    fun requestAccess(lang: AppLanguage) = tr(lang, "विवरण देखने की अनुमति माँगें", "ବିବରଣୀ ଦେଖିବାକୁ ଅନୁମତି ମାଗନ୍ତୁ")
    fun signOut(lang: AppLanguage) = tr(lang, "साइन आउट", "ସାଇନ୍ ଆଉଟ୍")

    fun allFilter(lang: AppLanguage) = tr(lang, "सभी", "ସମସ୍ତ")
    fun myRecordsFilter(lang: AppLanguage) = tr(lang, "मेरे रिकॉर्ड", "ମୋ ରେକର୍ଡ")
    fun verifiedLabel(lang: AppLanguage) = tr(lang, "सत्यापित", "ଯାଞ୍ଚିତ")
    fun pendingLabel(lang: AppLanguage) = tr(lang, "लंबित", "ବିଚାରାଧୀନ")
    fun rejectedLabel(lang: AppLanguage) = tr(lang, "अस्वीकृत", "ପ୍ରତ୍ୟାଖ୍ୟାତ")
    fun revokedLabel(lang: AppLanguage) = tr(lang, "निरस्त", "ପ୍ରତ୍ୟାହୃତ")

    fun incomingRequests(lang: AppLanguage) = tr(lang, "आए हुए अनुमति अनुरोध", "ଆସିଥିବା ଅନୁମତି ଅନୁରୋଧ")
    fun outgoingRequests(lang: AppLanguage) = tr(lang, "मेरे भेजे गए अनुरोध", "ମୋର ପଠାଯାଇଥିବା ଅନୁରୋଧ")
    fun villageSettings(lang: AppLanguage) = tr(lang, "गाँव की जानकारी व नाम बदलें", "ଗାଁର ନାମ ଓ ବିବରଣୀ ବଦଳାନ୍ତୁ")
    fun pendingApprovals(lang: AppLanguage) = tr(lang, "लंबित सत्यापन", "ବିଚାରାଧୀନ ଯାଞ୍ଚ")
    fun manageAdmins(lang: AppLanguage) = tr(lang, "एडमिन प्रबंधन", "ଆଡମିନ୍ ପରିଚାଳନା")
}
