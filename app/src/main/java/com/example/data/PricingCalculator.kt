package com.example.data

import java.util.Locale

object PricingCalculator {
    const val PLATFORM_FEE: Double = 4.0
    const val DELIVERY_RATE_PER_KM: Double = 70.0
    const val STANDARD_SERVICE_CHARGE_RATE: Double = 0.05 // 5% for bills >= 300 PKR
    const val LOW_BILL_SERVICE_CHARGE_RATE: Double = 0.07 // 7% for bills < 300 PKR
    const val LOW_BILL_THRESHOLD: Double = 300.0

    fun getServiceChargeRate(billAmount: Double): Double {
        return if (billAmount < LOW_BILL_THRESHOLD) LOW_BILL_SERVICE_CHARGE_RATE else STANDARD_SERVICE_CHARGE_RATE
    }

    fun calculateServiceCharge(billAmount: Double): Double {
        if (billAmount <= 0.0) return 0.0
        val rate = getServiceChargeRate(billAmount)
        return Math.round((billAmount * rate) * 100.0) / 100.0
    }

    fun calculateDeliveryCharge(distanceKm: Double): Double {
        if (distanceKm <= 0.0) return 0.0
        return Math.round((distanceKm * DELIVERY_RATE_PER_KM) * 100.0) / 100.0
    }

    fun calculateTotal(billAmount: Double, distanceKm: Double): Double {
        val sc = calculateServiceCharge(billAmount)
        val dc = calculateDeliveryCharge(distanceKm)
        return Math.round((billAmount + sc + dc + PLATFORM_FEE) * 100.0) / 100.0
    }

    fun formatPkr(amount: Double): String {
        return "PKR " + String.format(Locale.US, "%.2f", amount)
    }

    fun formatPkrUrdu(amount: Double): String {
        return String.format(Locale.US, "%.2f", amount) + " روپے"
    }
}
