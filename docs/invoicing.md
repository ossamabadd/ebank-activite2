# Application de facturation

Application de gestion des clients, des produits et des factures reposant sur une architecture microservices. Le mode BillingOnly lance les six services de facturation. Les modules EBank et chatbot sont disponibles independamment.

| Fonction | Implementation |
| --- | --- |
| Clients | customer-service : recherche, consultation, creation, modification |
| Produits | inventory-service : catalogue JPA/H2, CRUD, SKU unique, prix et quantite |
| Gateway | gateway-service, routes publiques /api/customers, /api/products, /api/bills |
| Routage statique | Profil Spring static, destinations HTTP explicites |
| Annuaire Eureka | discovery-service et enregistrement des services |
| Routage dynamique | Aliases par DiscoveryClient et destinations lb:// des routes /api |
| Facturation et Feign | billing-service appelle Customer et Inventory a la creation |
| Configuration | config-service, fichiers natifs communs et specifiques |
| Client Angular | frontend, Angular 21, interface responsive en francais |

## Lancer

Prerequis : Java 17+, Node.js 24 et npm. Le client utilise Angular 21.

```powershell
.\scripts\Start-Billing.ps1
```

Si les dependances et les builds sont deja disponibles :

```powershell
.\scripts\Start-Billing.ps1 -SkipBuild
```

Verification des echanges et du routage :

```powershell
.\scripts\Test-Billing.ps1
.\scripts\Test-StaticRouting.ps1
```

Arret de l'application :

```powershell
.\scripts\Stop-Billing.ps1
```

Interface : http://localhost:4200. Gateway : http://localhost:8080. Eureka : http://localhost:8761.
Swagger : Customer 8081, Inventory 8084, Billing 8085, chemin /swagger-ui/index.html.

Les journaux et identifiants des processus lances sont dans .runtime. Les scripts ne terminent que leurs propres processus, apres verification de la commande et de l'heure de demarrage.

Pour un lancement manuel : `mvnw.cmd verify`, `scripts/Start-Stack.ps1 -SkipBuild -BillingOnly`, puis `npm ci` et `npm start` dans frontend. Le proxy Angular envoie /api au Gateway local ; le navigateur n'appelle pas directement les services.

## Fonctionnement

Le tableau de bord calcule ses indicateurs a partir des factures et des clients effectivement charges. Les filtres et recherches portent sur toutes les pages retournees par l'API. Les listes affichent dix elements par page.

- Clients : creer, modifier, rechercher et ouvrir leurs factures.
- Produits : creer, modifier, supprimer, rechercher par nom ou reference, renseigner prix et stock.
- Factures : composer une facture multi-produits pour un client, consulter ses lignes et son total, filtrer les listes, marquer payee ou annuler.
- Impression : bouton Imprimer / PDF, qui ouvre le dialogue natif du navigateur avec une feuille de style dediee.

Les produits factures et les coordonnees client sont des **copies historiques**. Une modification du catalogue ou d'un client, ou la suppression d'un produit, ne modifie aucune facture existante. Billing recupere prix et coordonnees via Feign et ignore les prix fournis dans une requete navigateur. BigDecimal calcule les totaux sans erreur binaire. La facture utilise MAD ; aucune TVA n'est ajoutee automatiquement. Le stock est une information de catalogue : emettre une facture ne decremente pas les quantites.

Une facture est initialement UNPAID. Elle peut devenir PAID ou CANCELLED ; ces deux etats sont definitifs. Annuler conserve la facture consultable. Un en-tete Idempotency-Key empeche deux creations successives pour une meme soumission ; reutiliser la cle avec d'autres lignes renvoie 409. Les soumissions simultanees sont protegees par une contrainte unique et peuvent produire un 409 transitoire pour la requete concurrente.

Si Customer ou Inventory est indisponible pendant la creation, Billing renvoie 503 et aucune facture partielle n'est enregistree. Une reference inconnue renvoie 404. Les anciennes factures restent lisibles sans ces services grace aux copies historiques.

## Routes statiques et dynamiques

Par defaut, les routes /api utilisent la decouverte Eureka :

- /api/customers → lb://customer-service
- /api/products → lb://inventory-service
- /api/bills → lb://billing-service

Le DiscoveryClient cree egalement des routes a partir des services enregistres :

- /customer-service/api/customers
- /inventory-service/api/products
- /billing-service/api/bills

Le prefixe service est retire avant transmission.

Pour demontrer la configuration statique, lancer uniquement le Gateway avec le profil static (arreter le Gateway existant ou choisir un autre port) :

```powershell
java -jar gateway-service/target/gateway-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=static --server.port=8090 --spring.cloud.config.enabled=false
```

Les routes /api ciblent alors http://localhost:8081, :8084 et :8085 directement. Le client Eureka du Gateway et son DiscoveryClient Route Locator sont desactives. Les trois services metier doivent etre demarres ; ils restent independants du mode choisi pour le Gateway. Le profil static ne concerne que les trois routes de facturation.

Variables : INVENTORY_PORT (8084), BILLING_PORT (8085), INVENTORY_SERVICE_URL (vide pour Feign/Eureka, ou URL explicite), CUSTOMER_SERVICE_URL, INVENTORY_DB_URL et BILLING_DB_URL. Les parametres communs CONFIG_URL, DISCOVERY_URL et SERVICE_HOST restent applicables. .env.example est indicatif et n'est pas charge automatiquement.

H2 fonctionne en memoire par defaut. Un redemarrage recree les donnees de demonstration. Pour persister : creer .data puis definir CUSTOMER_DB_URL=jdbc:h2:file:./.data/customers, INVENTORY_DB_URL=jdbc:h2:file:./.data/inventory, BILLING_DB_URL=jdbc:h2:file:./.data/billing. Configurer les trois ensemble pour conserver des references coherentes. SEED_DATA=false desactive les exemples.

L'application est une demonstration locale avec donnees fictives, sans authentification. Elle ne vise pas les exigences legales d'un logiciel de facturation de production.

## Verification et ressources

`mvnw.cmd verify` execute les tests des anciens modules et les nouveaux tests Inventory/Billing. `npm run build` dans frontend verifie TypeScript et les templates Angular stricts. Les tests Billing couvrent les prix Feign, totaux decimaux, copies historiques, idempotence, validation, indisponibilite des services et transitions de statut.

La collection [postman/Invoicing.postman_collection.json](../postman/Invoicing.postman_collection.json) contient les appels de l'exercice.

Le [compte rendu de verification](invoicing-verification.md) detaille les 25 tests, les deux modes de routage et le parcours navigateur.

References : [depot de Mohamed Youssfi](https://github.com/mohamedYoussfi/micro-services-app), [compatibilite Angular](https://angular.dev/reference/versions), [routage DiscoveryClient](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/the-discoveryclient-route-definition-locator.html).
