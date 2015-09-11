FROM 1science/sbt

COPY ./target/universal/stage/ /app

WORKDIR /app

CMD ./bin/forecaster
