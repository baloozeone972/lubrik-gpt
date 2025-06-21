# services/video-gen/app.py
from fastapi import FastAPI, HTTPException, BackgroundTasks
from pydantic import BaseModel
import torch
from diffusers import StableVideoDiffusionPipeline
from PIL import Image
import io
import base64
import uuid
import os
from typing import Optional
import asyncio
from concurrent.futures import ThreadPoolExecutor

app = FastAPI(title="Video Generation Service")

# Configuration
MODEL_PATH = os.environ.get("MODEL_PATH", "/models/stable-video-diffusion")
OUTPUT_PATH = os.environ.get("OUTPUT_PATH", "/output")
DEVICE = "cuda" if torch.cuda.is_available() else "cpu"

# Thread pool pour les tâches lourdes
executor = ThreadPoolExecutor(max_workers=2)

# Modèle global (chargé au démarrage)
video_pipeline = None

class VideoRequest(BaseModel):
    image_url: str  # URL ou base64 de l'image source
    motion: str = "default"  # Type de mouvement
    duration: int = 4  # Durée en secondes
    fps: int = 8
    seed: Optional[int] = None

class VideoResponse(BaseModel):
    video_id: str
    video_url: str
    thumbnail_url: str
    duration: int
    status: str

@app.on_event("startup")
async def load_model():
    global video_pipeline
    try:
        print(f"Chargement du modèle depuis {MODEL_PATH}...")
        video_pipeline = StableVideoDiffusionPipeline.from_pretrained(
            MODEL_PATH,
            torch_dtype=torch.float16 if DEVICE == "cuda" else torch.float32,
            variant="fp16" if DEVICE == "cuda" else None
        )
        video_pipeline = video_pipeline.to(DEVICE)
        print("Modèle chargé avec succès!")
    except Exception as e:
        print(f"Erreur chargement modèle: {e}")
        # Fallback sur un modèle plus simple si nécessaire
        
@app.get("/health")
async def health_check():
    return {
        "status": "healthy",
        "model_loaded": video_pipeline is not None,
        "device": DEVICE
    }

@app.post("/generate", response_model=VideoResponse)
async def generate_video(request: VideoRequest, background_tasks: BackgroundTasks):
    if not video_pipeline:
        raise HTTPException(status_code=503, detail="Model not loaded")
    
    video_id = str(uuid.uuid4())
    
    # Lancer la génération en arrière-plan
    background_tasks.add_task(process_video_generation, video_id, request)
    
    return VideoResponse(
        video_id=video_id,
        video_url=f"/videos/{video_id}.mp4",
        thumbnail_url=f"/videos/{video_id}_thumb.jpg",
        duration=request.duration,
        status="processing"
    )

async def process_video_generation(video_id: str, request: VideoRequest):
    try:
        # Charger l'image source
        if request.image_url.startswith("data:"):
            # Base64
            image_data = base64.b64decode(request.image_url.split(",")[1])
            image = Image.open(io.BytesIO(image_data))
        else:
            # URL - à implémenter avec requests
            pass
        
        # Redimensionner si nécessaire
        image = image.resize((512, 512))
        
        # Générer la vidéo
        loop = asyncio.get_event_loop()
        frames = await loop.run_in_executor(
            executor,
            generate_video_frames,
            image,
            request
        )
        
        # Sauvegarder la vidéo
        output_path = os.path.join(OUTPUT_PATH, f"{video_id}.mp4")
        save_frames_as_video(frames, output_path, request.fps)
        
        # Générer thumbnail
        thumbnail_path = os.path.join(OUTPUT_PATH, f"{video_id}_thumb.jpg")
        frames[0].save(thumbnail_path)
        
    except Exception as e:
        print(f"Erreur génération vidéo {video_id}: {e}")
        # Enregistrer l'erreur dans Redis ou base de données

def generate_video_frames(image, request):
    """Génération des frames avec le modèle"""
    generator = torch.manual_seed(request.seed) if request.seed else None
    
    # Configuration selon le type de mouvement
    motion_configs = {
        "default": {"motion_bucket_id": 127, "noise_aug_strength": 0.02},
        "subtle": {"motion_bucket_id": 100, "noise_aug_strength": 0.01},
        "dynamic": {"motion_bucket_id": 150, "noise_aug_strength": 0.03}
    }
    
    config = motion_configs.get(request.motion, motion_configs["default"])
    
    # Génération
    frames = video_pipeline(
        image,
        num_frames=request.duration * request.fps,
        num_inference_steps=25,
        generator=generator,
        **config
    ).frames
    
    return frames

def save_frames_as_video(frames, output_path, fps):
    """Convertir les frames en vidéo MP4"""
    import cv2
    import numpy as np
    
    height, width = frames[0].size
    fourcc = cv2.VideoWriter_fourcc(*'mp4v')
    out = cv2.VideoWriter(output_path, fourcc, fps, (width, height))
    
    for frame in frames:
        # Convertir PIL en numpy array
        frame_np = np.array(frame)
        # BGR pour OpenCV
        frame_bgr = cv2.cvtColor(frame_np, cv2.COLOR_RGB2BGR)
        out.write(frame_bgr)
    
    out.release()

# Alternatives pour des animations simples sans GPU
class SimpleAnimationService:
    """Service de fallback pour animations basiques sans GPU"""
    
    @staticmethod
    def create_talking_animation(image_path, audio_path=None):
        """Créer une animation de personnage qui parle"""
        # Utiliser des techniques simples comme:
        # - Morphing de la bouche
        # - Mouvements subtils de la tête
        # - Clignement des yeux
        pass
    
    @staticmethod
    def create_idle_animation(image_path):
        """Animation idle (respiration, clignements)"""
        # Techniques légères:
        # - Transformation affine pour respiration
        # - Overlay pour clignements
        # - Perlin noise pour mouvements subtils
        pass

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=5003)