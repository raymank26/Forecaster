#!/bin/bash

su postgres -c "psql --command \"CREATE USER pguser WITH SUPERUSER PASSWORD 'pguser';\""

su postgres -c "createdb -O pguser forecast"