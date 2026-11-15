package com.example.bymevpn.data.payment

/** Returns the 30-day equivalent from a server-confirmed price and exact term, rounded up consistently. */
internal fun roundedMonthlyPrice(priceRub: Int, days: Int): Int =
    if (priceRub <= 0 || days <= 0) 0 else ((priceRub.toLong() * 30 + days - 1) / days).toInt()
