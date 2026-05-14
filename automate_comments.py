import os
import google.generativeai as genai


genai.configure(api_key="AIzaSyDh8ZRZx-lHKop-2oYsyo7i4ZjpkIVe3gk")

input_dir = "original_java"
output_dir = "ai_commented_java"

if not os.path.exists(output_dir):
    os.makedirs(output_dir)

# Using the fast, free tier model
model = genai.GenerativeModel('gemini-2.5-flash')

def generate_comments(code):
    prompt = f"You are an expert Java developer. Generate a comprehensive Javadoc-style comment for this code. Explain functionality, parameters, and logic:\n\n{code}"
    response = model.generate_content(prompt)
    return response.text

for filename in os.listdir(input_dir):
    if filename.endswith(".java"):
        with open(os.path.join(input_dir, filename), "r", encoding="utf-8") as f:
            code = f.read()
        
        print(f"Processing {filename}...")
        try:
            commented_code = generate_comments(code)
            with open(os.path.join(output_dir, f"AI_{filename}"), "w", encoding="utf-8") as f:
                f.write(commented_code + "\n\n" + code)
        except Exception as e:
            print(f"Error on {filename}: {e}")

print("Experiment Complete! All files commented.")