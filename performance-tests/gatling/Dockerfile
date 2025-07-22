FROM openjdk:17-jdk-slim

# Install dependencies
RUN apt-get update && \
    apt-get install -y wget unzip && \
    rm -rf /var/lib/apt/lists/*

# Install Gatling
ENV GATLING_VERSION 3.10.5
RUN wget -q https://repo1.maven.org/maven2/io/gatling/highcharts/gatling-charts-highcharts-bundle/${GATLING_VERSION}/gatling-charts-highcharts-bundle-${GATLING_VERSION}-bundle.zip && \
    unzip gatling-charts-highcharts-bundle-${GATLING_VERSION}-bundle.zip && \
    mv gatling-charts-highcharts-bundle-${GATLING_VERSION} /opt/gatling && \
    rm gatling-charts-highcharts-bundle-${GATLING_VERSION}-bundle.zip

# Set Gatling home
ENV GATLING_HOME /opt/gatling
ENV PATH $GATLING_HOME/bin:$PATH

# Create directories
RUN mkdir -p /opt/gatling/results /opt/gatling/user-files/simulations /opt/gatling/user-files/resources

# Copy simulation files
COPY src/test/scala/ /opt/gatling/user-files/simulations/
COPY src/test/resources/ /opt/gatling/user-files/resources/

WORKDIR /opt/gatling

# Default command
USER 1000

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1

ENTRYPOINT ["gatling.sh"]