package no.nav.sifinnsynapi.common

enum class Søknadstype {
    PP_SYKT_BARN,
    PP_ETTERSENDELSE,
    OMS_ETTERSENDELSE,
    PP_SYKT_BARN_ENDRINGSMELDING;

    fun gjelderPP() = when(this){
        PP_SYKT_BARN, PP_ETTERSENDELSE, PP_SYKT_BARN_ENDRINGSMELDING -> true
        else -> false
    }
}
