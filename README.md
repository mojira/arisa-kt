<!-- shields -->
[![](https://img.shields.io/github/issues/mojira/arisa-kt)](https://github.com/mojira/arisa-kt/issues)
[![](https://img.shields.io/github/stars/mojira/arisa-kt)](https://github.com/mojira/arisa-kt/stargazers)
[![](https://img.shields.io/github/license/mojira/arisa-kt)](https://github.com/mojira/arisa-kt/blob/master/LICENSE)

# Mojira Discord Bot

<!-- PROJECT LOGO -->
<br/>
<p align="center">
  <a href="https://bugs.mojang.com/">
    <img src="arisa.png" alt="Arisa" width="80" height="80">
  </a>

  <h3 align="center">Arisa</h3>

  <p align="center">
    A Jira bot for doing various moderation tasks.
    <br/>
    <a href="https://github.com/mojira/arisa-kt/issues">Report Bug</a>
    ·
    <a href="https://github.com/mojira/arisa-kt/issues">Request Feature</a>
  </p>
</p>

## About the project
MojiraBot was written by [urielsalis](https://github.com/urielsalis). Its purpose is to run various checks on tickets and do automated tasks. This project is based on [arisa-js](https://github.com/mojira/arisa-js) by [Mustek](https://github.com/mustek)

## Installation

If you want to tinker around with the project on your local PC, you can simply go ahead, clone the project and build it with Gradle.

```
git clone https://github.com/mojira/arisa-kt.git
```

```
./gradlew build
```

To run the bot, you need to run the following command and it will use the default configuration:
```
./gradlew build installDist
./build/install/arisa-kt/bin/arisa-kt  (add .bat in windows)
```

## Built with

This project depends on the following projects, thanks to every developer who makes their code open-source! :heart:

- [Kotlin](https://kotlinlang.org/)
- [Arrow](https://arrow-kt.io/)
- [jira-client by rcarz](https://github.com/rcarz/jira-client)

## Contributing

You're very welcome to contribute to this project! Please note that this project uses [Ktlint](https://github.com/pinterest/ktlint) to ensure consistent code, it runs with ./gradlew build but you can run it independetly using ./gradlew ktlintCheck

## Found a bug in Minecraft?

Please head over to [bugs.mojang.com](https://bugs.mojang.com), search whether your bug is already reported and if not, create an account and click the red "Create" button on the top of the page.

## License

Distributed under the GNU General Public License v3.0. See `LICENSE.md` for more information.
