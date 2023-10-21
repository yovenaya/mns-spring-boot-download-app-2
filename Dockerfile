FROM openjdk:20 as builder
WORKDIR application
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM openjdk:20
WORKDIR application
COPY --from=builder application/dependencies/ ./
COPY --from=builder application/snapshot-dependencies/ ./
COPY --from=builder application/application/ ./
COPY --from=builder application/spring-boot-loader/ ./
ENTRYPOINT ["java", "-Duser.timezone=Asia/Muscat", "-XX:InitialRAMPercentage=25", "-XX:MaxRAMPercentage=75", "org.springframework.boot.loader.JarLauncher"]