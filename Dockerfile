FROM openjdk:17-jdk-slim

ARG JAR_FILE=target/*.jar

COPY ${JAR_FILE} app.jar

# best security practice, run apps with only user privileges not root.
# if someone somehow messes w the app they get root access and can
# break/compromise system. So we only run with user permissions.
# Also give spring user ownership of the file with chown
RUN addgroup --system spring && \
    adduser --system --ingroup spring spring && \
    chown spring:spring app.jar

USER spring:spring

EXPOSE 8080

ENTRYPOINT [ "java","-jar","/app.jar" ]