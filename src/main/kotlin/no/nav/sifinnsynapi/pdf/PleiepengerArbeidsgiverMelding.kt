package no.nav.sifinnsynapi.konsument.pleiepenger.syktbarn

data class PleiepengerArbeidsgiverMelding(
    val arbeidstakernavn: String,
    val arbeidsgivernavn: String? = null,
    val søknadsperiode: SøknadsPeriode
)