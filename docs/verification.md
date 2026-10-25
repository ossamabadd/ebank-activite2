# Verification — 20 octobre 2026

Environnement : Windows, Java 25.0.1, compilation cible Java 17, Maven Wrapper 3.9.12.

- `mvnw.cmd -B -ntp verify` : **BUILD SUCCESS**, 8 modules, 13 tests, aucun echec ni erreur.
- Customer : 3 tests couvrant creation, recherche, modification, unicite des emails, validation, 404, initialisation MCP, liste et appel d'un outil reel.
- EBank : 7 tests couvrant enrichissement client, fallback, ouverture du circuit, exclusion des 404, idempotence, decouvert, precision des montants et credits concurrents.
- Chatbot : 2 tests avec l'adaptateur Spring AI Ollama reel et un serveur de modele simule ; execution d'outil, contexte, isolation, reset et erreur 503.
- Discord : 1 test de signature Ed25519, alteration du corps, signature invalide et horodatage expire.

Les six processus ont aussi ete lances ensemble avec `scripts/Start-Stack.ps1 -SkipBuild` :

- Enregistrement des cinq clients dans Eureka.
- Configuration native servie par Config Service et chargee par les clients.
- Routes Gateway clients, comptes et outils operationnelles.
- Enrichissement des comptes par OpenFeign.
- Cinq outils MCP distants decouverts par le chatbot.
- Credit repete avec la meme cle : une seule operation ; debit compensatoire : solde initial retrouve.
- Documents OpenAPI 3.1.0 disponibles pour Customer, EBank et Chatbot.
- Parcours REST → Spring AI → MCP → EBank → OpenFeign → Customer verifie avec le simulateur explicite du modele : **12 500.00 MAD**, lus dans le compte de demonstration de Hassan.

Le simulateur ne produit pas de langage naturel : il demande un outil et construit sa reponse depuis le resultat MCP reel. Son rapport est dans `.runtime/agent-smoke.json`. Aucun vrai modele Ollama ni client Telegram/Discord n'a ete utilise : Ollama n'est pas installe et aucun identifiant de bot n'a ete fourni.

Les processus Java et le simulateur Python ont ete arretes apres verification. Les fichiers JAR sont dans les repertoires target des six services ; les journaux de build et de lancement restent dans .runtime (ignore par Git).
