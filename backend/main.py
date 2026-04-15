from fastapi import FastAPI, Request
import groq
import os
import subprocess

app = FastAPI()
client = groq.Client(api_key=os.environ.get("GROQ_API_KEY"))

@app.post("/reason")
async def reason_about_screen(request: Request):
    data = await request.json()
    screen_content = data.get("screen_text")
    user_task = data.get("task", "Diagnose and fix the current issue")

    chat_completion = client.chat.completions.create(
        messages=[
            {"role": "system", "content": "You are bizzy, the Phone Doctor. Analyze the screen and return JSON action: {type: tap/swipe/type/call/sms/adb, params: {x, y, text, number, message, command}}. Be deterministic."},
            {"role": "user", "content": f"Task: {user_task}
Screen:
{screen_content}"}
        ],
        model="llama3-70b-8192",
    )
    return {"action": chat_completion.choices[0].message.content}

@app.post("/execute")
async def execute_authority_command(request: Request):
    data = await request.json()
    command = data.get("command")
    result = subprocess.run(["adb", "shell", command], capture_output=True, text=True)
    return {"stdout": result.stdout, "stderr": result.stderr}

@app.post("/command/call")
async def queue_call(request: Request):
    data = await request.json()
    number = data.get("number")
    if not number: return {"error": "No number"}
    result = subprocess.run([
        "adb", "shell", "am", "broadcast", "-a", "com.bot.COMMAND",
        "--es", "command", "call", "--es", "number", number
    ], capture_output=True, text=True)
    return {"status": "dialing", "out": result.stdout}

@app.post("/command/sms")
async def queue_sms(request: Request):
    data = await request.json()
    number = data.get("number")
    msg = data.get("message")
    if not number or not msg: return {"error": "Missing params"}
    result = subprocess.run([
        "adb", "shell", "am", "broadcast", "-a", "com.bot.COMMAND",
        "--es", "command", "sms", "--es", "number", number, "--es", "message", msg
    ], capture_output=True, text=True)
    return {"status": "sent", "out": result.stdout}

@app.post("/command/clean")
async def deep_clean(request: Request):
    # Manufacturer-level autonomous healing sequence
    commands = [
        "pm trim-caches 999G",
        "rm -rf /data/local/tmp/*",
        "sm fstrim"
    ]
    outs = []
    for cmd in commands:
        r = subprocess.run(["adb", "shell", cmd], capture_output=True, text=True)
        outs.append(r.stdout)
    return {"status": "cleaned", "details": outs}