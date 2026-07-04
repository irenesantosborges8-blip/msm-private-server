# MSM Private Server

Fork of MSM Sandbox com melhorias de segurança, configuração externalizada e cache local.

> [!NOTE]
> This is the code specifically for the sfs2x server. The pregame setup and auth server code is not included in this repository.

## What is MSM Sandbox?
MSM Sandbox is a private server project of My Singing Monsters. It was first created in mid-2024 and became the first private server for the PC version of the game.

In January 2025, I began fully re-coding MSM Sandbox to improve stability, add new features, and make it easier to expand in the future.

The rewrite introduces new systems that make the project much more organized and stable, which includes:

* Custom classes for handling players, islands, monsters, and structures.
* Database support (hosted on PythonAnywhere) for storing player data and game state.
* Cleaner architecture and easier-to-expand codebase compared to the original 2024 build.

However, in July 2025, My Singing Monsters updated its client to connect through an HTTP server using WebSockets. Since the MSM Sandbox recode was written in Java, and no reliable WebSocket modules were available for this setup, the server could no longer function. This forced the shutdown of MSM Sandbox, bringing an end to the project and leaving behind a community of players who had been enjoying it.

## Build

Requirements:

* Java 8+ (JDK)
* SmartFoxServer2X jars (see below)

### Getting SmartFoxServer2X jars

The SFS2X jars are proprietary and must be obtained from a SmartFoxServer 2X installation.
Place them in `MSMSandbox/lib/`:

- `sfs2x.jar`
- `sfs2x-core.jar`
- `sfs2x-lib.jar`
- `sfs2x-util.jar`
- `sfs2x-client-core.jar`
- `commons-lang-2.4.jar`
- `jdom.jar`
- `netty-3.2.2.Final.jar`
- `slf4j-api-1.6.1.jar`
- `slf4j-simple-1.6.1.jar`
- `gson-2.10.1.jar`

### Using Gradle

```bash
# Setup dependencies (H2, json)
.\setup.ps1

# Build
gradlew build
```

The output jar will be at `dist/mainExtension.jar`.

### Using Eclipse

1. Import the project via `File > Import > Existing Projects into Workspace`
2. Add the SFS2X jars to the build path
3. Export as JAR

## Run

1. Install SmartFoxServer2X and configure it to load this extension
2. Copy `config.example.properties` to `config.properties` and edit with your settings
3. Set `db.mode=local` for H2 standalone or `db.mode=remote` for PythonAnywhere API
4. Start SmartFoxServer2X

The server uses relative paths by default. On first run, it creates the necessary directories automatically.

## Database modes

### Local (H2) - Recommended
```
db.mode=local
db.local_path=database
```
H2 auto-creates the database file on first connection. Schema is initialized automatically.

### Remote (PythonAnywhere API) - Original
```
db.mode=remote
db.api_url=https://your-instance.pythonanywhere.com/admin/exec_sql
db.password=your_password
```

## Improvements in this fork

- **Config externalizada**: Credenciais, chaves de criptografia e URLs movidas para `config.properties`
- **SQL injection**: Migração de todas as queries para `PreparedStatement` via `SQLHandler.query()`
- **Cache local**: `MSMClient.saveToCache()` / `loadFromCache()` para funcionar offline
- **Banco H2 local**: `LocalDatabase.java` com suporte a modo standalone (H2) ou remoto (API)
- **Paths relativos**: `Settings.java` usa diretório do projeto, com fallback para `local.properties`
- **Webhook externalizado**: URL do Discord configurável via `discord.webhook_url`
- **.gitignore** adicionado
- **Build Gradle**: `build.gradle` para compilação automatizada

## Contributing

MSM Sandbox was originally shut down in July 2025 due to My Singing Monsters switching to an HTTP + WebSocket client connection. The current codebase is built on SmartFoxServer2X (Java), which has no reliable WebSocket support out of the box.

I personally use Eclipse Java IDE for editing.

If you'd like to contribute, here are a few areas that would greatly help the project:

- [ ] Implementing or integrating WebSocket support into the SmartFoxServer2X backend, fixing the shutdown issue entirely.
- [ ] Fixing bugs or stability issues in the custom classes.
- [ ] Improving database security and reliability, currently the server communicates with a remote database via HTTP requests (structured like: {'password': 'db-password', 'command': 'command'}) and is vulnerable to SQL injection.

### Notes
If you would like to reverse engineer the player data structure, there is these json files: `default_player_data.json`, `default_player_data_from_server.json` and `default_player_data_types.json`. Special thanks to @Zewsic for `default_player_data.json` and `default_player_data_types.json`.
