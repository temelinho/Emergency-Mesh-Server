import requests

def test_ai():
    url = "http://localhost:8010/predict"
    
    test_cases = [
        "Enkaz altinda bebek var cok acil yardım",
        "Ben iyiyim güvendeyiz sıkıntı yok",
        "Bacağım kırıldı kanama çok fazla"
    ]
    
    for text in test_cases:
        try:
            response = requests.post(url, json={"text": text})
            print(f"TEXT: {text}")
            print(f"RESULT: {response.json()}")
            print("-" * 20)
        except Exception as e:
            print(f"ERROR: {e}")

if __name__ == "__main__":
    test_ai()
