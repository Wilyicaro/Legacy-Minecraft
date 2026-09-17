# Contributing Guidelines

The Legacy4J repo has some small contribution requirements.

### Commit Tagging
Please adhere to the [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) handbook for writing your commits.

### Adding large features
Please talk with maintainers and developers before submitting a large Pull Request for the mod. This is to prevent issues from arising if it cannot be merged due to any reason.

### Identifying incompatible mods
If you find a mod that doesn't work with Legacy4J:
- If the mod keeps crashing the game, especially on startup, add the mod to "breaks" in fabric.mod.json, then add it to "The following mods are incompatible with Legacy4J due to causing crashes:" in README.md. If the mod is listed but is not in "breaks", please add it.
- If the mod causes important bugs that we can't fix, add the mod to "conflicts" in fabric.mod.json, then add it to "The following mods cause bugs when used with Legacy4J:" in README.md. If the mod is listed but is not in "conflicts", please add it.
