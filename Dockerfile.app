From java:8

COPY ./stage/ /app

WORKDIR /app

CMD /app/bin/forecaster
