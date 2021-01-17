FROM amazoncorretto:11.0.8

WORKDIR /app

COPY target/isucon-9-final-1.2.jar isucon-9-final.jar

EXPOSE 8000

ENV TZ=Asia/Tokyo

CMD ["java", "-jar", "isucon-9-final.jar"]
