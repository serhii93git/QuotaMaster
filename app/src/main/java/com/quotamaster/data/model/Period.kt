package com.quotamaster.data.model

/**
 * Supported quota periods.
 * [labelResName] maps to a string resource name in strings.xml
 * via a when-expression in the UI layer (Compose stringResource).
 */
enum class Period(val key: String) {
    DAILY("daily"),
    WEEKLY("weekly"),
    MONTHLY("monthly"),
    YEARLY("yearly")
}
