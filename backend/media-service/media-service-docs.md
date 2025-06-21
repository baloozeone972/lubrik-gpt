# Documentation Module Media Service

## Vue d'ensemble

Le Media Service gère tous les aspects liés aux médias dans l'application : upload d'images/vidéos, streaming vidéo en temps réel (WebRTC), génération de voix par IA, et traitement des médias. Il assure également la modération du contenu et l'optimisation pour la diffusion.

## Architecture du Module

### Technologies Utilisées
- **Spring WebFlux** pour les opérations réactives
- **Kurento Media Server** pour WebRTC
- **FFmpeg** pour le traitement vidéo/audio
- **MinIO** pour le stockage objet (S3-compatible)
- **Redis** pour le cache et les sessions
- **TTS Providers** (ElevenLabs, Azure, Google)
- **AI Models** pour la génération de contenu

### Pipeline de Traitement
```
Upload → Validation → Processing → Moderation → Storage → CDN
                           ↓
                     Génération Variants
                     (Thumbnails, Résolutions)
```

## Installation et Configuration

### Prérequis
1. **Kurento Media Server** pour WebRTC
2. **FFmpeg** pour le traitement média
3. **MinIO** pour le stockage
4. **GPU** (optionnel) pour l'accélération

### Installation Kurento
```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install -y gnupg

# Ajouter le repository Kurento
sudo apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 5AFA7A83
sudo tee /etc/apt/sources.list.d/kurento.list << EOF
deb [arch=amd64] http://ubuntu.openvidu.io/7.0.0 focal main
EOF

# Installer Kurento
sudo apt-get update
sudo apt-get install -y kurento-media-server

# Démarrer le service
sudo systemctl start kurento-media-server
sudo systemctl enable kurento-media-server
```

### Configuration FFmpeg
```bash
# Installer FFmpeg avec support complet
sudo apt-get install -y ffmpeg \
  libavcodec-extra \
  libavformat-dev \
  libavutil-dev \
  libswscale-dev
```

## API Endpoints

### Upload de Médias

#### Upload d'Image
```http
POST /api/v1/media/upload/image
Authorization: Bearer {token}
Content-Type: multipart/form-data

file: [image file]
purpose: avatar|character|conversation
isPublic: false
```

Réponse :
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "url": "https://cdn.virtualcompanion.app/images/...",
  "thumbnails": {
    "small": "https://cdn.virtualcompanion.app/thumbs/small/...",
    "medium": "https://cdn.virtualcompanion.app/thumbs/medium/...",
    "large": "https://cdn.virtualcompanion.app/thumbs/large/..."
  },
  "metadata": {
    "width": 1920,
    "height": 1080,
    "format": "jpeg",
    "size": 245632
  }
}
```

#### Upload de Vidéo
```http
POST /api/v1/media/upload/video
Authorization: Bearer {token}
Content-Type: multipart/form-data

file: [video file]
generateThumbnail: true
resolutions: ["720p", "1080p"]
```

#### Upload Audio
```http
POST /api/v1/media/upload/audio
Authorization: Bearer {token}
Content-Type: multipart/form-data

file: [audio file]
purpose: voice_sample|background_music
```

### Streaming Vidéo (WebRTC)

#### Initier une Session
```http
POST /api/v1/streaming/sessions
Authorization: Bearer {token}
Content-Type: application/json

{
  "characterId": "character-uuid",
  "conversationId": "conversation-uuid",
  "streamType": "VIDEO_CHAT",
  "videoResolution": "1280x720",
  "enableRecording": false
}
```

#### Échanger les SDP
```http
POST /api/v1/streaming/sessions/{sessionId}/offer
Authorization: Bearer {token}
Content-Type: application/json

{
  "sdp": "v=0\r\no=- ... (SDP offer)"
}
```

#### Ajouter des Candidats ICE
```http
POST /api/v1/streaming/sessions/{sessionId}/ice-candidates
Authorization: Bearer {token}
Content-Type: application/json

{
  "candidate": "candidate:...",
  "sdpMLineIndex": 0,
  "sdpMid": "0"
}
```

### Génération de Voix

#### Synthèse Vocale
```http
POST /api/v1/voice/generate
Authorization: Bearer {token}
Content-Type: application/json

{
  "text": "Bonjour, comment puis-je vous aider aujourd'hui?",
  "characterId": "character-uuid",
  "voiceId": "voice-id",
  "emotion": "friendly",
  "speed": 1.0,
  "emphasis": {
    "words": ["Bonjour", "aider"],
    "level": "strong"
  }
}
```

#### Obtenir les Voix Disponibles
```http
GET /api/v1/voice/available
Query Parameters:
  - languageCode: fr
  - gender: female
  - provider: elevenlabs
```

### Gestion des Médias

#### Obtenir un Média
```http
GET /api/v1/media/{mediaId}
Authorization: Bearer {token}
```

#### Supprimer un Média
```http
DELETE /api/v1/media/{mediaId}
Authorization: Bearer {token}
```

#### Obtenir mes Médias
```http
GET /api/v1/media/my-media
Authorization: Bearer {token}
Query Parameters:
  - type: IMAGE|VIDEO|AUDIO
  - page: 0
  - size: 20
```

### Modération

#### Vérifier un Média
```http
POST /api/v1/media/{mediaId}/moderate
Authorization: Bearer {token}
```

Réponse :
```json
{
  "isApproved": true,
  "moderationScore": 0.95,
  "labels": [
    {
      "name": "Safe",
      "confidence": 0.95
    }
  ],
  "warnings": []
}
```

## Configuration WebRTC

### Architecture WebRTC
```
Client Browser
    ↓ (WebSocket)
Media Service
    ↓ (Control)
Kurento Media Server
    ↓ (Media Streams)
Client Browser
```

### Configuration STUN/TURN
```yaml
kurento:
  ice-servers:
    - url: stun:stun.l.google.com:19302
    - url: turn:turn.example.com:3478
      username: user
      credential: pass
```

### Exemple Client JavaScript
```javascript
// Initialisation WebRTC
const pc = new RTCPeerConnection({
  iceServers: [
    { urls: 'stun:stun.l.google.com:19302' },
    { urls: 'turn:turn.example.com:3478', 
      username: 'user', 
      credential: 'pass' }
  ]
});

// Obtenir le stream local
const stream = await navigator.mediaDevices.getUserMedia({
  video: { width: 1280, height: 720 },
  audio: true
});

// Ajouter les tracks
stream.getTracks().forEach(track => {
  pc.addTrack(track, stream);
});

// Créer l'offre
const offer = await pc.createOffer();
await pc.setLocalDescription(offer);

// Envoyer l'offre au serveur
const response = await fetch('/api/v1/streaming/sessions/123/offer', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer ' + token
  },
  body: JSON.stringify({ sdp: offer.sdp })
});

const answer = await response.json();
await pc.setRemoteDescription(new RTCSessionDescription(answer));
```

## Traitement des Médias

### Pipeline d'Images
1. **Validation** : Format, taille, contenu
2. **Redimensionnement** : Création des variants
3. **Optimisation** : Compression WebP
4. **Modération** : Détection contenu inapproprié
5. **Stockage** : MinIO + CDN

### Pipeline Vidéo
1. **Validation** : Codec, durée, résolution
2. **Transcodage** : H.264/H.265, WebM
3. **Génération** : Thumbnails, preview
4. **Optimisation** : Bitrate adaptatif
5. **Stockage** : Chunked upload to MinIO

### Configuration de Traitement
```yaml
media:
  processing:
    image:
      max-width: 4096
      max-height: 4096
      compression-quality: 0.85
      formats: [webp, jpg, png]
    
    video:
      max-duration-seconds: 600
      max-file-size-mb: 500
      output-formats: [mp4, webm]
      presets:
        - name: mobile
          width: 480
          bitrate: 500k
        - name: sd
          width: 720
          bitrate: 1500k
        - name: hd
          width: 1080
          bitrate: 3000k
```

## Génération de Voix IA

### Providers Supportés
1. **ElevenLabs** : Voix ultra-réalistes
2. **Azure TTS** : Multi-langues, émotions
3. **Google Cloud TTS** : Rapide, abordable
4. **Local Models** : Tacotron2, WaveGlow

### Configuration des Voix
```json
{
  "voiceId": "21m00Tcm4TlvDq8ikWAM",
  "provider": "elevenlabs",
  "language": "fr-FR",
  "gender": "female",
  "age": "young_adult",
  "accent": "neutral",
  "characteristics": {
    "pitch": "medium",
    "speed": "normal",
    "emotion_range": "expressive"
  }
}
```

### Optimisation des Coûts
- Cache des générations fréquentes
- Batch processing pour réduire les appels API
- Fallback sur modèles locaux si quota dépassé
- Compression audio intelligente

## Sécurité et Modération

### Validation des Uploads
```java
// Limites par type
IMAGE: 10MB, formats: jpg/png/webp
VIDEO: 100MB, durée max: 10min
AUDIO: 20MB, durée max: 5min

// Validation du contenu
- Hash checking (contenu dupliqué)
- Virus scanning
- Format verification
- Metadata stripping
```

### Pipeline de Modération
1. **Pré-filtrage** : Détection rapide (hash, taille)
2. **IA Modération** : AWS Rekognition, Google Vision
3. **Scoring** : Calcul score de sécurité
4. **Review Manuel** : Queue pour cas limites
5. **Action** : Approuver/Rejeter/Flouter

## Performance

### Optimisations Appliquées
1. **Streaming Chunks** : Upload/Download par morceaux
2. **Processing Queue** : Traitement asynchrone
3. **CDN Integration** : Cache edge mondial
4. **Lazy Thumbnails** : Génération à la demande
5. **WebP Auto** : Conversion automatique

### Métriques de Performance
```
Upload Image: < 2s (10MB)
Process Video: < 30s/minute
Generate Voice: < 500ms (100 mots)
Stream Latency: < 100ms
```

## Monitoring

### Métriques Exposées
```
media_uploads_total{type, status}
media_processing_duration{type, operation}
streaming_sessions_active
voice_generations_total{provider}
storage_usage_bytes{bucket}
moderation_actions_total{result}
```

### Alertes Configurées
- Espace disque > 80%
- Échecs de traitement > 5%
- Latence streaming > 200ms
- Coûts API voice > seuil

## Troubleshooting

### Upload échoue
```bash
# Vérifier les limites
curl http://localhost:8084/actuator/health

# Logs d'erreur
grep "upload failed" logs/media-service.log

# Vérifier MinIO
mc admin info minio/
```

### Streaming ne fonctionne pas
```bash
# Vérifier Kurento
curl -X POST http://localhost:8888/kurento \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"ping","id":1}'

# Tester STUN/TURN
npm install -g stun
stun stun.l.google.com:19302
```

### Génération voix lente
```bash
# Vérifier le cache
redis-cli
> KEYS voice:*
> TTL voice:hash

# Status des providers
curl http://localhost:8084/api/v1/voice/providers/status
```

## Scripts Utiles

### Migration de Stockage
```bash
./scripts/migrate-storage.sh \
  --from s3://old-bucket \
  --to minio://new-bucket \
  --parallel 10
```

### Nettoyage des Médias
```bash
./scripts/cleanup-media.sh \
  --older-than 90d \
  --unused \
  --dry-run
```

### Benchmark Performance
```bash
./scripts/benchmark-media.sh \
  --operation upload \
  --file-size 10MB \
  --concurrent 50
```