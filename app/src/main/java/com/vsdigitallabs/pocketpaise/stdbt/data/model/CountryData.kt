package com.vsdigitallabs.pocketpaise.stdbt.data.model

data class CountryData(
    val name: String,
    val dialCode: String,
    val flag: String,
    val currencySymbol: String,
    val currencyCode: String,
    val currencyName: String
)

object SupportedCountries {
    val list = listOf(
        CountryData("India", "+91", "🇮🇳", "₹", "INR", "Indian Rupee (₹)"),
        CountryData("United States", "+1", "🇺🇸", "$", "USD", "US Dollar ($)"),
        CountryData("United Kingdom", "+44", "🇬🇧", "£", "GBP", "British Pound (£)"),
        CountryData("Canada", "+1", "🇨🇦", "CA$", "CAD", "Canadian Dollar ($)"),
        CountryData("Australia", "+61", "🇦🇺", "A$", "AUD", "Australian Dollar ($)"),
        CountryData("United Arab Emirates", "+971", "🇦🇪", "AED", "AED", "UAE Dirham (AED)"),
        CountryData("Saudi Arabia", "+966", "🇸🇦", "SAR", "SAR", "Saudi Riyal (SAR)"),
        CountryData("Germany (Eurozone)", "+49", "🇪🇺", "€", "EUR", "Euro (€)"),
        CountryData("France (Eurozone)", "+33", "🇫🇷", "€", "EUR", "Euro (€)"),
        CountryData("Singapore", "+65", "🇸🇬", "S$", "SGD", "Singapore Dollar ($)"),
        CountryData("Malaysia", "+60", "🇲🇾", "RM", "MYR", "Malaysian Ringgit (RM)"),
        CountryData("Japan", "+81", "🇯🇵", "¥", "JPY", "Japanese Yen (¥)"),
        CountryData("Nepal", "+977", "🇳🇵", "Rs", "NPR", "Nepalese Rupee (Rs)"),
        CountryData("Bangladesh", "+880", "🇧🇩", "৳", "BDT", "Bangladeshi Taka (৳)"),
        CountryData("Sri Lanka", "+94", "🇱🇰", "Rs", "LKR", "Sri Lankan Rupee (Rs)"),
        CountryData("Pakistan", "+92", "🇵🇰", "Rs", "PKR", "Pakistani Rupee (Rs)"),
        CountryData("South Africa", "+27", "🇿🇦", "R", "ZAR", "South African Rand (R)"),
        CountryData("Other / Global", "+1", "🌐", "$", "USD", "US Dollar ($)")
    )
}
