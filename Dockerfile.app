From java:8

COPY ./stage/ /app

COPY ./prod/ /app

WORKDIR /app

CMD ["/app/bin/forecaster", "-Dconfig.file=/app/application.conf"]

#CMD /app/bin/forecaster
