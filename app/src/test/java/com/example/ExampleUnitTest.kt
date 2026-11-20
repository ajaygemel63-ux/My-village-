package com.example

import com.example.data.model.IdMaskingUtils
import com.example.localization.AppLanguage
import com.example.localization.tr
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun maskAadhaar_masksFull12Digits() {
        val (masked, last4) = IdMaskingUtils.maskAadhaar("1234 5678 9012")
        assertEquals("XXXX-XXXX-9012", masked)
        assertEquals("9012", last4)
    }

    @Test
    fun maskAadhaar_acceptsLast4Digits() {
        val (masked, last4) = IdMaskingUtils.maskAadhaar("4829")
        assertEquals("XXXX-XXXX-4829", masked)
        assertEquals("4829", last4)
    }

    @Test
    fun maskAadhaar_rejectsShortInput() {
        val (masked, last4) = IdMaskingUtils.maskAadhaar("123")
        assertEquals("", masked)
        assertEquals("", last4)
    }

    @Test
    fun maskVoterId_masksMiddleCharacters() {
        val (masked, last4) = IdMaskingUtils.maskVoterId("odj1234567")
        assertEquals("ODJ••••567", masked)
        assertEquals("4567", last4)
    }

    @Test
    fun bilingualLocalization_formatsHindiOdiaAndBilingual() {
        assertEquals("सदस्य", tr(AppLanguage.HINDI, "सदस्य", "ସଦସ୍ୟ"))
        assertEquals("ସଦସ୍ୟ", tr(AppLanguage.ODIA, "सदस्य", "ସଦସ୍ୟ"))
        assertEquals("सदस्य • ସଦସ୍ୟ", tr(AppLanguage.BILINGUAL, "सदस्य", "ସଦସ୍ୟ"))
    }
}
