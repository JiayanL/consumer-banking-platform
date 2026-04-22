FROM python:3.11-slim

WORKDIR /app

COPY pyproject.toml ./
COPY src ./src

RUN pip install --no-cache-dir .

EXPOSE 8000
ENV FLASK_APP=reporting_aggregator.app:app
CMD ["flask", "run", "--host", "0.0.0.0", "--port", "8000"]
