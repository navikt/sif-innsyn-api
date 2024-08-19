package no.nav.sifinnsynapi.soknad

import no.nav.sifinnsynapi.common.AktørId
import no.nav.sifinnsynapi.common.Søknadstype
import no.nav.sifinnsynapi.config.TxConfiguration.Companion.TRANSACTION_MANAGER
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import java.util.*
import java.util.stream.Stream

@Transactional(transactionManager = TRANSACTION_MANAGER, rollbackFor = [Exception::class])
interface SøknadRepository : JpaRepository<SøknadDAO, UUID> {
    fun findAllByAktørId(aktørId: AktørId): List<SøknadDAO>
    fun findByJournalpostId(journalpostId: String): SøknadDAO?
    fun existsSøknadDAOByAktørIdAndJournalpostId(aktørId: AktørId, journalpostId: String): Boolean

    @Query(
            value = "SELECT COUNT(DISTINCT aktør_id) FROM søknad",
            nativeQuery = true
    )
    fun finnAntallUnikeSøkere(): Long

    @Query(
            value = "SELECT COUNT(*) FROM søknad WHERE søknadstype = ?1",
            nativeQuery = true
    )
    fun finnAntallSøknaderGittSøknadstype(søknadstype: String): Long

    /**
     * Henter en distinkt liste med [SøknadDAO] entiteter basert på [Søknadstype] som ble opprettet
     * innen de siste 6 månedene og som ikke har en tilsvarende fødselsnummeroppføring i mikrofrontend-tabellen.
     *
     * Denne metoden utfører en nativ SQL-spørring for å velge unike søknader basert på deres fødselsnummer, filtrering
     * etter søknadstype og et tidsintervall på de siste seks månedene fra dagens dato.
     *
     * @param [Søknadstype] Typen søknadsposter som skal hentes.
     * @param page Etterspurt sideinformasjon som inkluderer antall poster per side og sorteringsrekkefølge.
     * @return En [Slice] av [SøknadDAO] entiteter som matcher spørringskriteriene.
     */
    @Query(
        value = """
        SELECT DISTINCT ON (s.fødselsnummer) s.*
        FROM søknad s
        LEFT JOIN mikrofrontend m ON s.fødselsnummer = m.fødselsnummer
        WHERE s.søknadstype = ?1 AND s.opprettet >= CURRENT_DATE - INTERVAL '6 months'
        AND m.fødselsnummer IS NULL
        ORDER BY s.fødselsnummer, s.opprettet
        LIMIT ?2
        """,
        nativeQuery = true
    )
    fun finnUnikeSøknaderUtenMikrofrontendSisteSeksMåneder(søknadstype: String, limit: Int): List<SøknadDAO>

    @Query(
        value = """
        SELECT DISTINCT ON (s.fødselsnummer) s.*
        FROM søknad s
        WHERE s.søknadstype = ?1 AND s.opprettet < CURRENT_DATE - INTERVAL '6 months'
        ORDER BY s.fødselsnummer, s.id
    """,
        nativeQuery = true
    )
    fun finnAlleSøknaderMedUnikeFødselsnummerForSøknadstypeEldreEnnSeksMåneder(søknadstype: String): Stream<SøknadDAO>

    /**
     * Oppdaterer Aktørid for søker (aktørsplitt/merge)
     * @param gyldigAktørId: Gyldig aktørid
     * @param utgåttAktørId: Utgått aktørId
     * @return antall rader for utgått aktørid
     */
    @Transactional
    @Modifying
    @Query(
        nativeQuery = true,
        value = "UPDATE søknad SET aktør_id = ?1 WHERE aktør_id = ?2"
    )
    fun oppdaterAktørIdForSøker(gyldigAktørId: AktørId, utgåttAktørId: AktørId): Int
}
