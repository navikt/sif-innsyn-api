package no.nav.sifinnsynapi.common

enum class Søknadstype {
    OMP_UTVIDET_RETT,
    OMP_UTBETALING_SNF,
    OMP_UTBETALING_ARBEIDSTAKER,
    OMP_ETTERSENDING,
    PP_ETTERSENDING,
    PP_SYKT_BARN,
    OMD_OVERFØRING;

    fun erEttersending(): Boolean = when(this){
            OMP_ETTERSENDING -> true
            PP_ETTERSENDING -> true
            else -> false
        }

}