# AuthFlux

**AuthFlux** is a Minecraft server plugin built for **Spigot 1.21**, designed to provide robust authentication and authorization for players. It supports both **premium (official)** and **offline (pirate)** players, ensuring secure access to your server.

The plugin freezes players upon joining, teleports them to a configurable spawn point, and requires registration or login before allowing gameplay. Player data, including passwords and locations, is securely stored in a **PostgreSQL database**.

---

## Features

- **Secure Authentication**: Players must register (`/reg`) or login (`/log`) with a password (4–24 characters).
- **Player Freezing**: Unauthenticated players are frozen (cannot move, jump, or rotate) until they log in.
- **Location Management**: Saves players' initial locations, teleports them to a spawn point during authentication, and returns them to their original location after login.
- **PostgreSQL Integration**: Stores player data (UUID, username, hashed password, login status, location) in a PostgreSQL database.
- **Configurable**: Customize spawn point, database settings, and messages via `config.yml` and `messages.yml`.
- **Secure Passwords**: Uses **BCrypt** for password hashing to ensure security.
- **Cross-Platform**: Supports both premium and offline players using UUID-based identification.

---

## Requirements

- **Minecraft Server**: Spigot 1.21  
- **Java**: Version 21  
- **PostgreSQL**: Version 15 or later  
- **Maven**: For building the plugin  

### Dependencies

- PostgreSQL JDBC Driver (included in shaded JAR)  
- jBCrypt (included in shaded JAR)  

---

## Installation

### Clone the Repository

```bash
git clone https://github.com/alexander-yermolenko/authflux.git
cd authflux
```

### Build the Plugin
```bash
mvn clean package
```

The compiled JAR will be located at target/AuthFlux-1.0.jar.


### Set Up PostgreSQL
Install PostgreSQL if not already installed.

Create a database:

```bash
createdb -U postgres_username authflux
```

Note the database host, port, name, username, and password.

Configure the Plugin
Copy the JAR to your server:

```bash
cp target/AuthFlux-1.0.jar /path/to/spigot/plugins/
```

Start the server to generate default config files: config.yml and messages.yml in plugins/AuthFlux/.

### Edit config.yml:

```yaml
spawn-point:
  world: "world"
  x: 0.0
  y: 64.0
  z: 0.0
  yaw: 0.0
  pitch: 0.0

database:
  host: "localhost"
  port: 5432
  name: "authflux"
  username: "postgres"
  password: "your_password"
```

### Customize messages.yml for player-facing messages (supports & color codes).

```yaml
only-players: "&cThis command can only be used by players!"
reg-usage: "&cUsage: /reg [password]"
reg-already-registered: "&cYou are already registered! Use /log [password] to login."
reg-success: "&aSuccessfully registered! Welcome to the server!"
log-usage: "&cUsage: /log [password]"
log-not-registered: "&cYou need to register first using /reg [password]"
log-already-logged-in: "&cYou are already logged in!"
log-success: "&aSuccessfully logged in! Welcome back!"
log-wrong-password: "&cIncorrect password!"
join-new-player: "&eWelcome! Please register using /reg [password]"
join-returning-player: "&eWelcome back! Please login using /log [password]"
move-not-registered: "&cPlease register first using /reg [password]"
move-not-logged-in: "&cPlease login using /log [password]"
password-length-invalid: "&cPassword must be between %min% and %max% characters!"
```

### Restart the Server
```bash
./start.sh
```

Check console logs for successful database connection and table creation.

## Usage
### Commands
/reg <password> — Register a new account (password must be 4–24 characters).

/log <password> — Log in to an existing account.

### Player Flow
Player joins → teleported to spawn → frozen.

New players use /reg <password> to register.

Returning players use /log <password>.

After login → unfrozen → teleported back to saved location.

On disconnect → location is saved → login required next time.