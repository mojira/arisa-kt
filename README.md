<!-- shields -->
[![](https://img.shields.io/github/issues/mojira/arisa-kt)](https://github.com/mojira/arisa-kt/issues)
[![](https://img.shields.io/github/stars/mojira/arisa-kt)](https://github.com/mojira/arisa-kt/stargazers)
[![](https://img.shields.io/github/license/mojira/arisa-kt)](https://github.com/mojira/arisa-kt/blob/master/LICENSE.md)

# Arisa

<!-- PROJECT LOGO -->
<br/>
<div style="text-align: center">
  <a href="https://bugs.mojang.com/">
    <img src="arisa.png" alt="Arisa" width="80" height="80">
  </a>

  <h3>Arisa</h3>

  <p>
    A JIRA bot for doing various moderation tasks on Mojang's bug tracker <a href="https://bugs.mojang.com/">Mojira</a>.
    <br/>
    <a href="https://bugs.mojang.com/secure/ViewProfile.jspa?name=arisabot">Arisa's bug tracker profile</a>
    ·
    <a href="https://github.com/mojira/arisa-kt/issues">Report Bug</a>
    ·
    <a href="https://github.com/mojira/arisa-kt/issues">Request Feature</a>
  </p>
</div>

## About the project
Arisa is a Bot account on Mojira, Mojang's bug tracker. Its purpose is to run various checks on tickets and execute automated tasks.
For instance, it automatically analyzes newly posted tickets and automatically resolves them in case the game is modified.

Arisa was [originally written in Ruby](https://github.com/mojira/arisa) by Synchunk, a now retired Mojira moderator.
Later, Arisa was [rewritten in JavaScript](https://github.com/mojira/arisa-js) by [Mustek](https://github.com/Mustek).
This Kotlin version of Arisa was written by [urielsalis](https://github.com/urielsalis).

## Installation

If you want to tinker around with the project on your local PC, you can simply go ahead, clone the project and build it with Gradle.

```
git clone https://github.com/mojira/arisa-kt.git
```

```
./gradlew clean build
```

To run the bot with the default configuration, you need to run the following command:
```
./gradlew build installDist
./build/install/arisa-kt/bin/arisa-kt
```

In order to configure Arisa, create a `local.yml` file inside the `config` folder.
There you can set all your credentials and overwrite any configs in `config.yml` for debugging purposes.
There are also some specific debug configs, you can find their documentation in the `ArisaConfig.kt` file.

## Built with

This project depends on the following projects, thanks to every developer who makes their code open-source! :heart:

- [Kotlin](https://kotlinlang.org/)
- [Arrow](https://arrow-kt.io/)
- [jira-client](https://github.com/rcarz/jira-client) by [rcarz](https://github.com/rcarz)
- [logback-discord-appender](https://github.com/napstr/logback-discord-appender) by [napstr](https://github.com/napstr)
- [mc-crash-lib](https://github.com/urielsalis/mc-crash-lib)
- [Apache Commons Imaging](https://commons.apache.org/proper/commons-imaging/)

## Contributing

You're very welcome to contribute to this project! Please note that this project uses [ktlint](https://github.com/pinterest/ktlint) to ensure consistent code.
It runs with `./gradlew clean build`, but you can also run it independently using `./gradlew ktlintCheck`.

## Found a bug in Minecraft?

Please head over to [bugs.mojang.com](https://bugs.mojang.com/), search whether your bug is already reported and if not, create an account and click the red "Create" button on the top of the page.

## License

Distributed under the GNU General Public License v3.0. See `LICENSE.md` for more information.
