package no.nav.sifinnsynapi.k8s

import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.net.InetAddress

@Service
class LeaderService(@Qualifier("leaderRestTemplate") private val restTemplate: RestTemplate) {
    private companion object {
        private val logger = LoggerFactory.getLogger(LeaderService::class.java)
    }

    fun executeAsLeader(action: () -> Unit) {
        val caller = action.javaClass.name
        if (isLeader()) {
            logger.info("$caller kjøres av leder ${leader()}")
            action.invoke()
        } else {
            logger.info("Pod er ikke leder. Skipper $caller.")
        }
    }

    private fun isLeader(): Boolean {
        val leader = leader()
        val hostname = InetAddress.getLocalHost().hostName
        return hostname == leader
    }

    private fun leader(): String {
        val response = restTemplate.getForObject("/", String::class.java)
        return JSONObject(response).getString("name")
    }
}
