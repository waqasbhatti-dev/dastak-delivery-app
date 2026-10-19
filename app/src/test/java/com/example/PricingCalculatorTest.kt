package com.example

import com.example.data.PricingCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class PricingCalculatorTest {

    @Test
    fun testDeliveryFeeCalculations() {
        // Examples from requirement:
        // 1 km = PKR 70.
        // 2 km = PKR 140.
        // 2.5 km = PKR 175.
        // 4 km = PKR 280.
        assertEquals(70.0, PricingCalculator.calculateDeliveryCharge(1.0), 0.001)
        assertEquals(140.0, PricingCalculator.calculateDeliveryCharge(2.0), 0.001)
        assertEquals(175.0, PricingCalculator.calculateDeliveryCharge(2.5), 0.001)
        assertEquals(280.0, PricingCalculator.calculateDeliveryCharge(4.0), 0.001)
    }

    @Test
    fun testFractionalDeliveryFeeRounding() {
        // 1.234 km * 70 = 86.38 -> rounds cleanly to 2 decimal places
        val fee = PricingCalculator.calculateDeliveryCharge(1.234)
        assertEquals(86.38, fee, 0.01)
    }

    @Test
    fun testServiceFeeBelow300() {
        // Below PKR 300 -> 7%
        val fee = PricingCalculator.calculateServiceCharge(200.0)
        assertEquals(14.0, fee, 0.01)

        val feeZero = PricingCalculator.calculateServiceCharge(0.0)
        assertEquals(0.0, feeZero, 0.01)
    }

    @Test
    fun testServiceFeeAtOrAbove300() {
        // At or above PKR 300 -> 5%
        val fee300 = PricingCalculator.calculateServiceCharge(300.0)
        assertEquals(15.0, fee300, 0.01)

        val fee1000 = PricingCalculator.calculateServiceCharge(1000.0)
        assertEquals(50.0, fee1000, 0.01)
    }

    @Test
    fun testPlatformFee() {
        assertEquals(4.0, PricingCalculator.PLATFORM_FEE, 0.001)
    }

    @Test
    fun testTotalCalculation() {
        // 2.5 km delivery fee = 175.0, est item cost = 500.0, service fee (5%) = 25.0, platform fee = 4.0
        // Total = 500 + 175 + 25 + 4 = 704.0
        val total = PricingCalculator.calculateTotal(billAmount = 500.0, distanceKm = 2.5)
        assertEquals(704.0, total, 0.001)
    }
}
