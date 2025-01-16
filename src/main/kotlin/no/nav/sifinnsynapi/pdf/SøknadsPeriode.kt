package no.nav.sifinnsynapi.konsument.pleiepenger.syktbarn

import java.time.LocalDate

data class SøknadsPeriode(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate
)