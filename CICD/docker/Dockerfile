FROM eclipse-temurin:17-jre

WORKDIR /app

COPY build/libs/*.jar app.jar

# 컨테이너에서 JVM 힙을 넉넉하게 사용하도록 기본값 설정
ENV JAVA_TOOL_OPTIONS="-Xms1g -Xmx3g"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
