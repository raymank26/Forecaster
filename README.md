# Forecaster

This is the [telegram bot](https://core.telegram.org/bots/api) which provides information about current and ahead forecast with pictures from nearest webcams based on the user's location.

As for now bot is primary used by me. But it's great example of real world combination of Akka, Akka-HTTP, Akka-Testkit,
ScalikeJDBC, Docker, Flyway migrations and so on.

# Commands

The list is below:

1. `\help` - shows help message
2. `\current` - shows current forecast and webcam previews
3. `\today` - shows ahead forecast
4. `\clear` - removes settings
5. `\settings` - sets or overwrites settings

# Dependencies

Docker and sbt (yeah, build staff can be moved to separate build container) 

# Development and deployment

Repository has dockerfiles for database and app containers. So running is simply and requires two steps:

1. Filling `src/main/application.conf` with template in the same folder.
2. Running `docker-compose up` (for local development I use `docker-compose up db` only)

The bot uses [webhooks](https://core.telegram.org/bots/api#setwebhook). Thus the Telegram's `setWebhook` method should be used to point remote server to web application.

For example POST-request can be:

`https://api.telegram.org/bot-id/setWebhook?url=https://remote-server:8443/test`

(Don't forget to provide self-signed SSL certificate if needed)

As for me I use nginx and ssh proxy for development purposes. The scheme is:

```
telegram server -> nginx(takes off SSL and proxies to 7777) -> ssh proxy(from 7777 to localhost) -> laptop localhost
```

SSH proxy command:

`ssh -R <remote_port>:localhost:<local_port> your-remote-server`

Stdout logging provided is out of the box. Thus `docker logs forecast-app` should print startup information
and logged incoming HTTP-requests.

The information provided by https://forecast.io and http://webcams.travel

