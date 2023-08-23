# Build stage
FROM adoptopenjdk/openjdk11:latest
RUN apt-get update
RUN apt-get install -y maven
RUN apt-get install -y git
WORKDIR /home/app
RUN git clone https://github.com/Maciej-Wilczyk/SumFromFile.git .

RUN mvn package
CMD ["java","-cp","target/SumFromFile-test.jar","SumFromFile"]
