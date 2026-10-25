# Verification de la facturation — 24 octobre 2026

## Builds et tests

- Tous les modules Java compilent ; les huit applications sont empaquetees en JAR executable.
- `mvnw.cmd -B -ntp test` : **BUILD SUCCESS**, 25 tests, aucun echec ni erreur.
- Repartition : Customer 3, EBank 7, Chatbot 2, signatures Discord 1, Gateway 2, Inventory 3, Billing 7.
- `npm ci` et `npm run build` : client Angular compile, templates stricts et TypeScript valides. Bundle initial de production : environ 281 Ko (77 Ko transferes estimes).

## Integration entre services

Les six services de facturation et Angular ont ete lances par `scripts/Start-Billing.ps1 -SkipBuild`.

- Gateway : routes /api/customers, /api/products et /api/bills.
- DiscoveryClient : /inventory-service/api/products et /billing-service/api/bills/{id}.
- Creation d'une facture via Gateway, recuperation des clients et prix par OpenFeign.
- Total exact de 29 470 MAD dans le test HTTP, soumission repetee sans seconde facture et passage au statut PAID.
- Gateway temporaire en profil static sur 8090 : les trois routes fixes repondent avec leurs donnees. Ce processus temporaire a ensuite ete arrete.

## Parcours navigateur avec Playwright

Sur le client Angular relie aux vraies API :

1. Chargement du tableau de bord et des listes.
2. Creation du client fictif Sarah Benali, confirmation et presence dans le carnet.
3. Creation du produit Support ordinateur aluminium, reference SUP-DEMO, prix 249,90 MAD, stock 15.
4. Creation de FAC-2026-00005 pour Sarah : 3 supports a 249,90 MAD et 2 hubs a 490 MAD, total **1 729,70 MAD**.
5. Verification du detail, des coordonnees, des lignes et du total.
6. Marquage comme payee et confirmation du nouveau statut.
7. Verification visuelle de la feuille de style d'impression, avec navigation et commandes masquees.
8. Verification mobile a 390 pixels : largeur du document 390 pixels, sans debordement horizontal de la page. Les tableaux larges restent defilables dans leur conteneur.
9. Console du navigateur : **0 erreur, 0 avertissement** lors du parcours final.

Captures dans output/playwright/ (ignore par Git) : dashboard-desktop.png, invoice-compose.png, invoice-detail.png, invoice-print.png, invoice-mobile.png.

Les donnees creees pendant les tests sont fictives et restent dans les bases H2 en memoire jusqu'au prochain arret. L'application est laissee en fonctionnement pour consultation sur http://localhost:4200. Utiliser `scripts/Stop-Billing.ps1` pour arreter les processus geres.
