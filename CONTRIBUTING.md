You're very welcome to contribute to this project! 

Please note that this project uses [Ktlint](https://github.com/pinterest/ktlint) to ensure consistent code, it runs with ```./gradlew build``` but you can run it independetly using ```./gradlew ktlintCheck```

- If you are working on a issue, please assign it to yourself or leave a comment and include it in your PR
- Make sure your code passes all tests and code style checks using ```./gradlew clean build```
- Include tests, we try to use real examples when possible (in the crash module for example)
- You are encouraged to do mini refactors when you can see a improvement, but try to keep them to a separate PR if they get too big
- Separate complex topics in different PRs, for example: 1 module per PR, 1 big refactor separated from a module
- Test it on bugs.mojang.com in MCTEST or a private MC ticket(and let a mod know beforehand to trash it after you are done)
