package com.lizht.app.model

data class Store(
    val _id: String,
    val name: String,
    val address: String,
    val contact: String,
    val timings: String,
    val discountPercent: Int,
    val images: List<String>,
    val active: Boolean,

    // --- NEW: Razorpay Payment Button fields (nullable + safe defaults) ---
    val paymentEnabled: Boolean = false,
    val paymentProvider: String? = null,   // "razorpay_button"
    val razorpayButtonId: String? = null,
    val razorpayButtonHtml: String? = null
) {
    /** Convenience: true when this store can show the Razorpay button */
    fun canPayWithRazorpay(): Boolean =
        paymentEnabled && paymentProvider == "razorpay_button" &&
                (!razorpayButtonId.isNullOrBlank() || !razorpayButtonHtml.isNullOrBlank())
}
