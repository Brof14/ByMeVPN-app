package com.example.bymevpn.data

import java.util.Locale

/** ISO region flags are encoded as two Unicode regional-indicator symbols. */
object CountryFlags {
    private val regions = Locale.getISOCountries().toSet() + setOf("EU", "XK")
    private const val REGIONAL_A = 0x1F1E6
    private val countryNames by lazy {
        Locale.getISOCountries().flatMap { code ->
            val country = Locale("", code)
            listOf(country.getDisplayCountry(Locale.ENGLISH), country.getDisplayCountry(Locale("ru")))
                .filter { it.length >= 4 }
                .map { it.lowercase(Locale.ROOT) to code }
        }.distinct().sortedByDescending { it.first.length }
    }

    fun normalize(code: String): String? {
        val normalized = code.trim().uppercase(Locale.ROOT)
        return normalized.takeIf { it.length == 2 && it in regions }
    }

    fun emoji(code: String): String? = normalize(code)?.let { region ->
        region.map { Character.toChars(REGIONAL_A + (it - 'A')).concatToString() }.joinToString("")
    }

    fun regionIn(text: String): String? {
        var index = 0
        while (index < text.length) {
            val first = Character.codePointAt(text, index)
            index += Character.charCount(first)
            if (first !in REGIONAL_A..REGIONAL_A + 25 || index >= text.length) continue
            val second = Character.codePointAt(text, index)
            if (second !in REGIONAL_A..REGIONAL_A + 25) continue
            val code = "${('A'.code + first - REGIONAL_A).toChar()}${('A'.code + second - REGIONAL_A).toChar()}"
            normalize(code)?.let { return it }
        }
        return null
    }

    fun regionInCountryName(text: String): String? {
        val source = text.lowercase(Locale.ROOT)
        for ((name, code) in countryNames) {
            var start = source.indexOf(name)
            while (start >= 0) {
                val end = start + name.length
                if ((start == 0 || !source[start - 1].isLetter()) &&
                    (end == source.length || !source[end].isLetter())) return code
                start = source.indexOf(name, start + 1)
            }
        }
        return null
    }
}
