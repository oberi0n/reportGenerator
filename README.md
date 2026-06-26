# Lab Report Generator

Worker Java 21 autonome qui surveille `/data/incoming`, verrouille chaque XML, le parse, génère un PDF JasperReports, archive XML/PDF/`metadata.json` dans MinIO, puis déplace le XML vers `/data/processed` ou `/data/error` avec un `.log`.

## Démarrage

```bash
docker compose up --build
```

Déposez un XML, par exemple `ExportMedLogin2605066005.xml`, dans `./data/incoming`. Après traitement, le XML est déplacé dans `./data/processed`. En cas d'erreur, il est déplacé dans `./data/error` avec un fichier `.log`.


## Interface de recherche

Une interface web très simple est exposée par l'application sur <http://localhost:8080>. Elle s’appuie sur un index en mémoire construit depuis les fichiers `metadata.json` archivés dans MinIO et permet de rechercher un rapport avec n’importe quelle métadonnée: nom/prénom patient, ID interne, matricule/SSN, médecin, statut, référence, validateur, etc. Les résultats affichent les informations patient principales et un lien temporaire vers le PDF archivé. En Docker Compose, ces liens utilisent `MINIO_PUBLIC_ENDPOINT=http://localhost:9000` pour être ouvrables depuis le navigateur.

API équivalente :

```bash
curl "http://localhost:8080/api/reports/search?q=TESTER"
```

## MinIO

- API: <http://localhost:9000>
- Console: <http://localhost:9001>
- Identifiants par défaut: `minioadmin` / `minioadmin`
- Bucket par défaut: `lab-results`

Les objets sont écrits sous `patients/{patientInternalId}/{referenceNumber}/` avec `{referenceNumber}.xml`, `{referenceNumber}.pdf` et `metadata.json`. Si l'identifiant patient interne manque, le SSN est utilisé, sinon `UNKNOWN_PATIENT`.

## Templates, images et polices

Les templates Jasper sont dans `./templates` et montés dans `/app/templates`. Changez le template principal avec `APP_TEMPLATE_MAIN`. Les images marketing sont remplaçables dans `./images`, monté dans `/app/images`. Les polices sont remplaçables dans `./fonts`, monté dans `/app/fonts`; ajoutez vos fichiers et référencez-les dans vos `.jrxml`.

## Variables d'environnement

- `APP_INCOMING_DIR`, `APP_PROCESSED_DIR`, `APP_ERROR_DIR`
- `APP_POLL_INTERVAL_SECONDS`, `APP_SEARCH_REFRESH_SECONDS`
- `APP_TEMPLATE_DIR`, `APP_TEMPLATE_MAIN`
- `APP_IMAGE_DIR`, `APP_FONT_DIR`
- `MINIO_ENDPOINT`, `MINIO_PUBLIC_ENDPOINT`, `MINIO_REGION`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET`

## Métadonnées

Chaque upload porte des métadonnées objet: `referenceNumber`, `patientInternalId`, `ssnPatient`, `patientLastName`, `patientFirstName`, `patientBirthdate`, `physicianCode`, `physicianName`, `specimenDate`, `validationDate`, `examinationStatus`, `targetLanguage`, `documentType`, `sourceFilename`, `uploadTimestamp`, `checksumSha256`.

`metadata.json` contient les informations patient, médecin, dates, statut, accréditation, nombre de résultats, nombre de résultats anormaux, sections, validateurs, footer, checksums et chemins objet.

## Limites connues

MinIO/S3 ne fournit pas de recherche native efficace sur les métadonnées objet. L’application maintient un index en mémoire rafraîchi périodiquement depuis les `metadata.json`, ce qui évite de reparcourir et reparsing tous les JSON à chaque recherche. Pour des centaines de milliers ou millions de rapports, il faudra privilégier un index externe durable comme PostgreSQL, OpenSearch ou Elasticsearch.

## Développement

```bash
mvn test
```

Le code ne dépend d'aucune classe propriétaire `lu.labo.*`.
