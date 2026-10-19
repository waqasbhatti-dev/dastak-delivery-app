package com.example

import com.example.data.PricingCalculator
import com.example.data.model.Advertisement
import com.example.data.repository.AdvertisementRepository
import org.junit.Assert.*
import org.junit.Test

class AdvertisementTest {

    @Test
    fun testInitialDastakPromotionDefaults() {
        val promo = AdvertisementRepository.getInitialDastakPromotion(null)
        assertEquals("03049457463", promo.phone)
        assertEquals("PUBLISHED", promo.status)
        assertTrue(promo.isActive)
        assertEquals("IMAGE", promo.mediaType)
        assertTrue(promo.title.contains("Dastak") || promo.title.contains("دستک"))
        assertTrue(promo.description.contains("Jhelum City Housing") || promo.description.contains("جہلم"))
        assertTrue(promo.description.contains("App Coming Soon"))
        assertFalse("Should not claim already on Play Store", promo.description.contains("Download on Play Store now"))
    }

    @Test
    fun testScheduleValidation_activeAndPublished() {
        val now = 1_000_000L
        val ad = Advertisement(
            id = "ad_1",
            title = "Special Discount",
            isActive = true,
            status = "PUBLISHED",
            startDate = null,
            endDate = null
        )
        assertTrue("Active and published ad without dates should be valid", ad.isScheduleValid(now))
    }

    @Test
    fun testScheduleValidation_pausedOrDraft() {
        val now = 1_000_000L
        val draftAd = Advertisement(
            id = "ad_draft",
            title = "Draft Ad",
            isActive = true,
            status = "DRAFT"
        )
        assertFalse("Draft ads should not be visible to customers", draftAd.isScheduleValid(now))

        val pausedAd = Advertisement(
            id = "ad_paused",
            title = "Paused Ad",
            isActive = true,
            status = "PAUSED"
        )
        assertFalse("Paused ads should not be visible to customers", pausedAd.isScheduleValid(now))

        val inactiveAd = Advertisement(
            id = "ad_inactive",
            title = "Inactive Ad",
            isActive = false,
            status = "PUBLISHED"
        )
        assertFalse("Inactive ads should not be visible to customers", inactiveAd.isScheduleValid(now))
    }

    @Test
    fun testScheduleValidation_futureSchedule() {
        val now = 1_000_000L
        val futureAd = Advertisement(
            id = "ad_future",
            title = "Upcoming Promo",
            isActive = true,
            status = "PUBLISHED",
            startDate = now + 10_000L, // Starts in future
            endDate = now + 20_000L
        )
        assertFalse("Ad scheduled for future should not be visible yet", futureAd.isScheduleValid(now))
    }

    @Test
    fun testScheduleValidation_withinSchedule() {
        val now = 1_000_000L
        val ongoingAd = Advertisement(
            id = "ad_ongoing",
            title = "Current Promo",
            isActive = true,
            status = "PUBLISHED",
            startDate = now - 5_000L,
            endDate = now + 5_000L
        )
        assertTrue("Ad within date range should be visible", ongoingAd.isScheduleValid(now))
    }

    @Test
    fun testScheduleValidation_expiredSchedule() {
        val now = 1_000_000L
        val expiredAd = Advertisement(
            id = "ad_expired",
            title = "Expired Sale",
            isActive = true,
            status = "PUBLISHED",
            startDate = now - 20_000L,
            endDate = now - 1_000L // Already ended
        )
        assertFalse("Expired ad should not be visible", expiredAd.isScheduleValid(now))
    }

    @Test
    fun testEditableContactNumberForFutureAds() {
        val customAd = Advertisement(
            id = "ad_custom",
            title = "Local Grocery Deal",
            phone = "03001234567" // Different advertiser contact
        )
        assertEquals("03001234567", customAd.phone)
        assertNotEquals(AdvertisementRepository.DEFAULT_DASTAK_PHONE, customAd.phone)
    }

    @Test
    fun testAdPriceFormatting() {
        val formattedPkr = PricingCalculator.formatPkr(499.50)
        assertEquals("PKR 499.50", formattedPkr)

        val formattedUrdu = PricingCalculator.formatPkrUrdu(499.50)
        assertTrue(formattedUrdu.contains("499.50"))
        assertTrue(formattedUrdu.contains("روپے"))
    }

    @Test
    fun testMediaSizeLimits() {
        assertEquals(25 * 1024 * 1024L, AdvertisementRepository.MAX_VIDEO_SIZE_BYTES)
        assertEquals(5 * 1024 * 1024L, AdvertisementRepository.MAX_IMAGE_SIZE_BYTES)
    }

    @Test
    fun testVideoAdvertisementProperties() {
        val videoAd = Advertisement(
            id = "video_promo_1",
            title = "Jhelum City Special Offer Video",
            businessName = "Jhelum Mart",
            mediaType = "VIDEO",
            mediaUrl = "https://firebasestorage.googleapis.com/v0/b/app/o/advertisements/promo_30s.mp4",
            posterUrl = "https://firebasestorage.googleapis.com/v0/b/app/o/advertisements/poster.jpg",
            isActive = true,
            status = "PUBLISHED"
        )
        assertEquals("VIDEO", videoAd.mediaType)
        assertTrue(videoAd.mediaUrl.endsWith(".mp4"))
        assertTrue(videoAd.mediaUrl.startsWith("https://"))
        assertFalse(videoAd.mediaUrl.startsWith("content://"))
        assertFalse(videoAd.mediaUrl.startsWith("file://"))
    }

    @Test
    fun testImageAdvertisementProperties() {
        val imgAd = Advertisement(
            id = "img_promo_1",
            title = "Fresh Dairy Promotion",
            businessName = "Local Dairy",
            mediaType = "IMAGE",
            mediaUrl = "https://firebasestorage.googleapis.com/v0/b/app/o/advertisements/fresh_dairy.jpg",
            isActive = true,
            status = "PUBLISHED"
        )
        assertEquals("IMAGE", imgAd.mediaType)
        assertTrue(imgAd.mediaUrl.startsWith("https://") || imgAd.mediaUrl.startsWith("data:image"))
    }
}
