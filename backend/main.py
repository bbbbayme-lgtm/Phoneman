from fastapi import FastAPI, Request
import groq
import os

app = FastAPI()
client = groq.Client(api_key=os.environ.get("GROQ_API_KEY"))

@app.post("/reason")
async def reason_about_screen(request: Request):
    data = await request.json()
    screen_content = data.get("screen_text")

    chat_completion = client.chat.completions.create(
        messages=[
            {"role": "system", "content": "You are a phone bot controller. Decide the next action based on screen content. Return JSON: {type: click|scroll|type|swipe, target: ..., value: ...}"},
            {"role": "user", "content": f"Screen content: {screen_content}"}
        ],
        model="llama3-70b-8192",
    )
    return {"action": chat_completion.choices[0].message.content}

@app.post("/vision")
async def reason_about_screenshot(request: Request):
    data = await request.json()
    image_b64 = data.get("image_base64")
    context = data.get("context", "")

    chat_completion = client.chat.completions.create(
        messages=[
            {"role": "system", "content": "You are a phone bot with vision. Analyze the screenshot and decide the next action."},
            {"role": "user", "content": f"Context: {context}
Image (base64): {image_b64}"}
        ],
        model="llama3-70b-8192",
    )
    return {"action": chat_completion.choices[0].message.content}
