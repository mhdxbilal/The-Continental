import asyncio
import json
import os
import re
import time
import secrets
from fastapi import FastAPI, WebSocket, WebSocketDisconnect, HTTPException, Depends, Header
from fastapi.responses import HTMLResponse, StreamingResponse
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel
import yt_dlp
import httpx # Required for QoS testing
from urllib.parse import urlparse

app = FastAPI()

SETTINGS_FILE = 'settings.json'
HISTORY_FILE = 'history.json'

def load_settings():
    if os.path.exists(SETTINGS_FILE):
        with open(SETTINGS_FILE, 'r') as f:
            return json.load(f)
    return {
        "preferred_download_directory": "/storage/emulated/0/Download", 
        "default_quality": "bestvideo+bestaudio/best",
        "max_concurrent_downloads": 5,
        "network_reconnect_attempts": 3
    }

def save_settings(settings):
    with open(SETTINGS_FILE, 'w') as f:
        json.dump(settings, f)
        
def load_history():
    if os.path.exists(HISTORY_FILE):
        with open(HISTORY_FILE, 'r') as f:
            return json.load(f)
    return []

def save_history(history):
    with open(HISTORY_FILE, 'w') as f:
        json.dump(history, f)
        
class SettingsRequest(BaseModel):
    preferred_download_directory: str
    download_profile: str = "Always Best Quality"
    max_concurrent_downloads: int = 5
    network_reconnect_attempts: int = 3
    
class URLRequest(BaseModel):
    url: str

class DownloadRequest(BaseModel):
    urls: list[str]
    format: str = 'bestvideo+bestaudio/best'
    download_to_server: bool = False

class ConnectionManager:
    def __init__(self):
        self.active_connections: list[WebSocket] = []

    async def connect(self, websocket: WebSocket):
        await websocket.accept()
        self.active_connections.append(websocket)

    def disconnect(self, websocket: WebSocket):
        if websocket in self.active_connections:
            self.active_connections.remove(websocket)

    async def broadcast(self, message: str):
        valid_connections = []
        for connection in self.active_connections:
            try:
                await connection.send_text(message)
                valid_connections.append(connection)
            except Exception:
                pass
        self.active_connections = valid_connections

manager = ConnectionManager()

active_downloads = {} # url -> is_cancelled flag

def get_progress_hook(url):
    def progress_hook(d):
        status = d.get('status')
        if active_downloads.get(url):
            raise Exception("DownloadCancelledByUser")
            
        if status == 'downloading':
            percent = d.get('_percent_str', '0.0%')
            speed = d.get('_speed_str', 'N/A')
            eta = d.get('_eta_str', 'N/A')
            filename = d.get('filename', 'Unknown')
            msg = {"type": "progress", "url": url, "filename": filename, "percent": percent, "speed": speed, "eta": eta}
            asyncio.run(manager.broadcast(json.dumps(msg)))
        elif status == 'finished':
            msg = {"type": "finished", "url": url, "filename": d.get('filename', 'Unknown')}
            asyncio.run(manager.broadcast(json.dumps(msg)))
    return progress_hook

@app.post("/settings/")
async def update_settings(req: SettingsRequest):
    settings = load_settings()
    settings['preferred_download_directory'] = req.preferred_download_directory
    settings['download_profile'] = req.download_profile
    settings['max_concurrent_downloads'] = req.max_concurrent_downloads
    settings['network_reconnect_attempts'] = req.network_reconnect_attempts
    save_settings(settings)
    return {"status": "success"}

@app.get("/settings/")
async def get_settings():
    return load_settings()

@app.get("/history/")
async def get_history():
    return load_history()

@app.post("/cancel/")
async def cancel_download(req: URLRequest):
    if req.url in active_downloads:
        active_downloads[req.url] = True
        return {"status": "success"}
    return {"status": "not found"}

def sanitize_filename(title):
    return re.sub(r'[\\/*?:"<>|]', "", title)

async def measure_qos():
    # Simple QOS measuring throughput using a fast server
    start = time.time()
    try:
        async with httpx.AsyncClient() as client:
            resp = await client.get("https://cloudflare.com/cdn-cgi/trace", timeout=2.0)
            elapsed = time.time() - start
            rtt = elapsed * 1000 # ms
            # Adjust threads based on RTT. Simple heuristic.
            if rtt < 50:
                return 10 # High speed
            elif rtt < 150:
                return 5
            else:
                return 2
    except:
        return 2

@app.post("/formats/")
async def get_formats(req: URLRequest):
    ydl_opts = {
        'quiet': True,
        'no_warnings': True,
        'noplaylist': True,
        'user_agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36'
    }
    
    def extract():
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            return ydl.extract_info(req.url, download=False)
            
    try:
        info = await asyncio.get_event_loop().run_in_executor(None, extract)
    except Exception:
        return {"error": "Could not fetch metadata"}
        
    formats = []
    
    for f in info.get('formats', []):
        height = f.get('height')
        if height and f.get('vcodec') != 'none':
            ext = f.get('ext', 'mp4')
            fid = f.get('format_id')
            formats.append({
                "format_id": f"{fid}+bestaudio/best",
                "label": f"{height}p - {ext}"
            })
            
    # Deduplicate and sort
    seen = set()
    unique_formats = []
    for fmt in formats:
        if fmt['label'] not in seen:
            seen.add(fmt['label'])
            unique_formats.append(fmt)
            
    # sort by height descending
    def get_height(label):
        match = re.search(r'(\d+)p', label)
        return int(match.group(1)) if match else 0
        
    unique_formats.sort(key=lambda x: get_height(x['label']), reverse=True)
    
    unique_formats.insert(0, {
        "format_id": "bestvideo+bestaudio/best",
        "label": "Best Quality (Default)"
    })
    unique_formats.append({
        "format_id": "bestaudio/best",
        "label": "Audio Only"
    })
    
    title = info.get('title', 'Unknown')
    sanitized_title = sanitize_filename(title)
    
    return {"title": sanitized_title, "formats": unique_formats}

@app.post("/download/")
async def start_download(req: DownloadRequest):
    settings = load_settings()
    download_dir = settings.get('preferred_download_directory', 'downloads')
    
    # Ensure absolute pathing within volume
    abs_dir = os.path.abspath(download_dir)
    if not os.path.exists(abs_dir):
        os.makedirs(abs_dir, exist_ok=True)
        
    postprocessors = []
    if 'bestaudio' in req.format and 'bestvideo' not in req.format and 'p' not in req.format:
        postprocessors.append({
            'key': 'FFmpegExtractAudio',
            'preferredcodec': 'mp3',
            'preferredquality': '192',
        })
        
    is_video = not ('bestaudio' in req.format and 'bestvideo' not in req.format and '+' not in req.format)

    qos_threads = await measure_qos()

    ydl_opts = {
        'format': req.format,
        'outtmpl': os.path.join(abs_dir, '%(uploader|UnknownChannel)s', '%(upload_date|UnknownDate)s', '%(title).200s.%(ext)s'),
        'merge_output_format': 'mkv' if is_video else None,
        'postprocessors': postprocessors,
        'http_chunk_size': 10485760,
        'concurrent_fragment_downloads': qos_threads,
        'user_agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36',
        'noplaylist': True,
        'restrictfilenames': True, # Sanitize filename further using yt-dlp internal
    }
    
    def run_ytdlp(url):
        active_downloads[url] = False
        
        # Make a copy of ydl_opts to attach the specific hook
        url_ydl_opts = ydl_opts.copy()
        url_ydl_opts['progress_hooks'] = [get_progress_hook(url)]

        settings = load_settings()
        max_retries = settings.get("network_reconnect_attempts", 3)
        for attempt in range(max_retries):
            try:
                if active_downloads.get(url):
                    break
                with yt_dlp.YoutubeDL(url_ydl_opts) as ydl:
                    # Pre-fetch to get original title
                    info = ydl.extract_info(url, download=False)
                    title = info.get('title', 'Unknown')
                    sanitized_title = sanitize_filename(title)
                    ydl.params['outtmpl'] = {'default': os.path.join(abs_dir, f"{sanitized_title}.%(ext)s")}
                    
                    hist = load_history()
                    hist.insert(0, {"title": title, "url": url, "timestamp": time.time(), "status": "Started"})
                    save_history(hist[:50]) # keep last 50
                    
                    ydl.download([url])
                    
                    hist = load_history()
                    for h in hist:
                        if h["url"] == url and h["timestamp"] > time.time() - 86400:
                            h["status"] = "Completed"
                    save_history(hist)
                    break # Success, break out of retry loop
            except Exception as e:
                err_msg = str(e)
                if "DownloadCancelledByUser" in err_msg:
                    hist = load_history()
                    for h in hist:
                        if h["url"] == url and h["timestamp"] > time.time() - 86400:
                            h["status"] = f"Failed/Cancelled: {err_msg}"
                    save_history(hist)
                    break # Do not retry on cancel
                
                if attempt < max_retries - 1:
                    time.sleep(2) # Wait before retry
                    msg = {"type": "error", "message": f"Network issue/timeout. Retrying ({attempt+1}/{max_retries})...", "url": url}
                    asyncio.run(manager.broadcast(json.dumps(msg)))
                    continue
                else:
                    hist = load_history()
                    for h in hist:
                        if h["url"] == url and h["timestamp"] > time.time() - 86400:
                            h["status"] = f"Failed after {max_retries} retries: {err_msg}"
                    save_history(hist)
                    msg = {"type": "error", "message": err_msg, "url": url}
                    asyncio.run(manager.broadcast(json.dumps(msg)))
        
        if url in active_downloads:
            del active_downloads[url]

    for url in req.urls:
        asyncio.get_event_loop().run_in_executor(None, run_ytdlp, url)
        
    return {"status": "Download(s) queued", "qos_threads": qos_threads}

VALID_STREAM_TOKEN = os.getenv("STREAM_TOKEN", "default_secret_token_123")

def verify_token(x_stream_token: str = Header(None), token: str = None):
    if x_stream_token == VALID_STREAM_TOKEN or token == VALID_STREAM_TOKEN:
        return True
    raise HTTPException(status_code=401, detail="Invalid token")

@app.get("/stream/")
async def stream_video(path: str, auth: bool = Depends(verify_token)):
    settings = load_settings()
    base_dir = os.path.abspath(settings.get('preferred_download_directory', 'downloads'))
    file_path = os.path.abspath(os.path.join(base_dir, path))
    
    if not file_path.startswith(base_dir) or not os.path.exists(file_path):
        raise HTTPException(status_code=404, detail="File not found")
        
    def file_iterator(file_path, chunk_size=8192):
        with open(file_path, "rb") as f:
            while chunk := f.read(chunk_size):
                yield chunk
                
    return StreamingResponse(file_iterator(file_path), media_type="video/mp4")

@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    await manager.connect(websocket)
    try:
        while True:
            await websocket.receive_text()
    except WebSocketDisconnect:
        manager.disconnect(websocket)


app.mount("/", StaticFiles(directory="static", html=True), name="static")
