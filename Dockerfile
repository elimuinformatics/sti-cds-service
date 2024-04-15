FROM maven:3.9.3-amazoncorretto-17 as build
WORKDIR /usr/sti-cds-service/

COPY *.xml /usr/sti-cds-service/
# COPY settings.xml .
# #RUN mvn -ntp dependency:go-offline

COPY src/ /usr/sti-cds-service/src
RUN mvn clean package

FROM amazoncorretto-17

ENV LANGUAGE='en_US:en'

# We make four distinct layers so if there are application changes the library layers can be re-used
COPY --from=build /usr/sti-cds-service/target/quarkus-app/lib/ deployments/lib/
COPY --from=build /usr/sti-cds-service/target/quarkus-app/*.jar deployments/
COPY --from=build /usr/sti-cds-service/target/quarkus-app/app/ deployments/app/
COPY --from=build /usr/sti-cds-service/target/quarkus-app/quarkus/ deployments/quarkus/


EXPOSE 8080

ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="deployments/quarkus-run.jar"

ENTRYPOINT java ${JAVA_OPTS} -jar ${JAVA_APP_JAR}
