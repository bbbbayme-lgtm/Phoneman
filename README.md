# 🤖 Phoneman

Autonomous phone bot. Reads the screen, reasons with Groq, executes actions. No rules.

## Stack
- **Android (Kotlin):** Accessibility Service — reads screen, clicks, types, scrolls, swipes
- **Backend (Python/FastAPI):** Groq `llama3-70b-8192` as the brain
- **Deploy:** Render (backend) + Android APK (device)

## Structure
```
/android     - Kotlin Accessibility Service app
/backend     - Python FastAPI + Groq reasoning
/scripts     - ADB fallback & setup
```

## Setup

### Backend
```bash
cd backend
pip install -r requirements.txt
export GROQ_API_KEY=your_key_here
uvicorn main:app --reload
```

### Android
1. Open `/android` in Android Studio
2. Set `backendUrl` in `BotAccessibilityService.kt` to your Render URL
3. Build + install APK
4. Enable the Accessibility Service in Android Settings → Accessibility

## Flow
1. Accessibility event fires → screen text captured
2. Text sent to FastAPI `/reason`
3. Groq decides: click / type / scroll / swipe
4. Kotlin executes the action on device
