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

    fun isLeader(): Boolean {
        val leader = leader()
        logger.info("$leader er leder.")
        val hostname = InetAddress.getLocalHost().hostName
        return hostname == leader
    }

    private fun leader(): String {
        val response = restTemplate.getForObject("/", String::class.java)
        return JSONObject(response).getString("name")
    }
}
