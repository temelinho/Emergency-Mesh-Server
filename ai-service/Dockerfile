FROM python:3.10-slim

WORKDIR /app

# Install dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application files
COPY main.py .
COPY train_model.py .
COPY emergency_model.pkl .

# Expose port
EXPOSE 8030

# Run the API
CMD ["python", "main.py", "--host", "0.0.0.0", "--port", "8030"]
