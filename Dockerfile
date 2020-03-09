FROM gradle AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build installDist --no-daemon

FROM openjdk:8-jre-slim
RUN mkdir /app
COPY --from=build /home/gradle/src/build/install/arisa-kt/bin/arisa-kt /app/arisa-kt
ENTRYPOINT ["/app/arisa-kt"]