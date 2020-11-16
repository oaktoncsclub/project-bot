FROM openjdk:11-jdk-slim AS build-env

ADD . /bot/code/
WORKDIR /bot/
RUN cd code && ./gradlew
RUN cd code && ./gradlew shadowJar && mv build/libs/*all.jar ../bot.jar

FROM gcr.io/distroless/java:11
COPY --from=build-env /bot /bot
WORKDIR /bot/
ENTRYPOINT ["java","-jar","bot.jar"]
EXPOSE 5001
CMD [""]
