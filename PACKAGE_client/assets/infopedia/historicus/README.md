# Historicus Infopedia Entries

This directory is the client-facing home for Historicus world-lore information.

Historicus material is in-universe/player-facing lore and should be packaged with the client as infopedia content. It should not live in root `docs/`, which is reserved for project operations, engineering standards, command references, release process, audit reports, milestones, and development history.

Future Historicus entries should be stored here using the infopedia format adopted by the client.

Suggested future structure:

```text
PACKAGE_client/assets/infopedia/historicus/
  index.json
  eras/
  institutions/
  factions/
  places/
  people/
  artifacts/
```

Until the infopedia loader format is finalized, this README acts as the location marker and classification rule.
