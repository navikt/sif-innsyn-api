# SIF Innsynstjeneste

![CI / CD](https://github.com/navikt/https://github.com/navikt/sif-innsyn-api/actions/workflows/CI%20/%20CD/badge.svg)
![NAIS Alerts](https://github.com/navikt/https://github.com/navikt/sif-innsyn-api/actions/workflows/Alerts/badge.svg)

# Innholdsoversikt
* [1. Kontekst](#1-kontekst)
* [2. Funksjonelle Krav](#2-funksjonelle-krav)
* [3. Begrensninger](#3-begrensninger)
* [4. Programvarearkitektur](#5-programvarearkitektur)
* [5. Kode](#6-kode)
* [6. Data](#7-data)
* [7. Infrastrukturarkitektur](#8-infrastrukturarkitektur)
* [8. Distribusjon av tjenesten (deployment)](#9-distribusjon-av-tjenesten-deployment)
* [9. Utviklingsmiljø](#10-utviklingsmilj)
* [10. Drift og støtte](#11-drift-og-sttte)

# 1. Kontekst
Kafka konsumer og API-tjeneste for sif-innsynsplatform.

# 2. Funksjonelle Krav
Denne tjenesten understøtter behovet for innsyn for bruker.
Tjenesten lytter etter diverse søknads hendelser som e.g. mottatte søknader, søknader under behandling, etc. og lagrer dem  i database.
Tjenesten eksponerer også api-er for henting av overnevnte data, for å gi bruker innsyn i egne saker.

# 3. Begrensninger
Innsyn i sakene til bruker begrenses til kapittel 9 ytelsene som e.g. omsorgspenger, pleiepenger, etc.

# 4. Programvarearkitektur

# 5. Kode

# 6. Data

# 7. Infrastrukturarkitektur

# 8. Distribusjon av tjenesten (deployment)
Distribusjon av tjenesten er gjort med bruk av Github Actions.
[Sif Innsyn API CI / CD](https://github.com/navikt/sif-innsyn-api/actions)

Push/merge til master branche vil teste, bygge og deploye til produksjonsmiljø og testmiljø.

# 9. Utviklingsmiljø
## Bygge Prosjekt
For å bygge kode, kjør:

```shell script
./gradlew clean build
```

## Kjøre Prosjekt
For å kjøre kode, kjør:

```shell script
docker build --tag sif-innsyn-api-local && docker-compose up --build 
```

### Produsere kafka meldinger
For produsere kafka meldinger, må man først exec inn på kafka kontaineren ved å bruker docker dashbord, eller ved å kjøre følgende kommando:
```shell script
docker exec -it <container-id til kafka> /bin/sh; exit
```

Deretter, kjøre følgende kommando for å koble til kafka instansen:
```shell script
kafka-console-producer --broker-list localhost:9092 --topic privat-sif-innsyn-mottak --producer.config=$CLASSPATH/producer.properties
```

### Henting av data via api-endepunktene
Applikasjonen er konfigurert med en lokal oicd provider stub for å utsending og verifisering av tokens. For å kunne gjøre kall på endepunktene, må man ha et gyldig token.

#### Henting av token
1. Åpne oicd-provider-gui i nettleseren enten ved å bruke docker dashbord, eller ved å gå til http://localhost:5000.
2. Trykk "Token for nivå 4" for å logge inn med ønsket bruker, ved å oppgi fødselsnummer. Tokenet blir da satt som en cookie (selvbetjening-idtoken) i nettleseren.
3. Deretter kan du åpne http://localhost:8080/swagger-ui.html for å teste ut endepunktene.

Om man ønsker å bruke postman må man selv, lage en cookie og sette tokenet manuelt. Eksempel:
selvbetjening-idtoken=eyJhbGciOiJSUzI1NiIsInR5cCI6Ikp.eyJzdWIiOiIwMTAxMDExMjM0NSIsImFjc.FBmVFuHI9d8akrVdAxi1dRg03qKV4EGk; Path=/; Domain=localhost; Expires=Fri, 18 Jun 2021 08:46:13 GMT;

# 10. Drift og støtte
## Logging


## Alarmer
Vi bruker [nais-alerts](https://doc.nais.io/observability/alerts) for å sette opp alarmer. Disse finner man konfigurert i [nais/alerterator.yml](nais/alerterator.yml).

## Metrics
n/a

## Henvendelser
Spørsmål koden eller prosjekttet kan rettes til team dusseldorf på:
* \#sif-brukerdialog
* \#sif-innsynsplattform


