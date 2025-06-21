package com.virtualcompanion.tools;

public class ProjectReconstructor {

    private static final String PROJECT_ROOT = "virtual-companion";
    private final Path outputDirectory;
    private final Map<String, Integer> statistics = new HashMap<>();
    
    // Patterns pour identifier les différents types de fichiers
    private static final Pattern FILE_HEADER_PATTERN = Pattern.compile(
        "^\\s*(?://|#|--|/\\*)?\\s*([\\w/\\-]+(?:/[\\w\\-]+)*\\.[\\w\\.]+)\\s*$", 
        Pattern.MULTILINE
    );
    
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile(
        "```(\\w+)?\\s*\\n([\\s\\S]*?)\\n```", 
        Pattern.MULTILINE
    );
    
    // Mapping des extensions vers les types de fichiers
    private static final Map<String, FileType> EXTENSION_MAP = Map.of(
        "java", FileType.JAVA,
        "ts", FileType.TYPESCRIPT,
        "tsx", FileType.TYPESCRIPT,
        "js", FileType.JAVASCRIPT,
        "jsx", FileType.JAVASCRIPT,
        "yml", FileType.YAML,
        "yaml", FileType.YAML,
        "xml", FileType.XML,
        "md", FileType.MARKDOWN,
        "sql", FileType.SQL,
        "properties", FileType.PROPERTIES
    );

    public enum FileType {
        JAVA("backend"),
        TYPESCRIPT("frontend"),
        JAVASCRIPT("frontend"),
        YAML("infrastructure"),
        XML("backend"),
        MARKDOWN("docs"),
        SQL("backend/migrations"),
        PROPERTIES("backend/resources"),
        OTHER("")
    }

    public ProjectReconstructor(String outputPath) {
        this.outputDirectory = Paths.get(outputPath, PROJECT_ROOT);
    }

    /**
     * Point d'entrée principal pour reconstituer le projet
     */
    public void reconstructProject(List<Path> sourceFiles) throws IOException {
        System.out.println("🚀 Démarrage de la reconstruction du projet Virtual Companion");
        System.out.println("📁 Répertoire de sortie: " + outputDirectory.toAbsolutePath());
        
        // Créer le répertoire racine
        Files.createDirectories(outputDirectory);
        
        // Créer la structure de base
        createProjectStructure();
        
        // Traiter chaque fichier source
        for (Path sourceFile : sourceFiles) {
            System.out.println("\n📄 Traitement de: " + sourceFile.getFileName());
            processSourceFile(sourceFile);
        }
        
        // Créer les fichiers de configuration racine
        createRootConfigFiles();
        
        // Afficher les statistiques
        printStatistics();
    }

    /**
     * Crée la structure de base du projet
     */
    private void createProjectStructure() throws IOException {
        List<String> directories = Arrays.asList(
            // Backend
            "backend/user-service/src/main/java/com/virtualcompanion/user",
            "backend/user-service/src/main/resources",
            "backend/user-service/src/test/java/com/virtualcompanion/user",
            "backend/character-service/src/main/java/com/virtualcompanion/character",
            "backend/character-service/src/main/resources",
            "backend/conversation-service/src/main/java/com/virtualcompanion/conversation",
            "backend/conversation-service/src/main/resources",
            "backend/media-service/src/main/java/com/virtualcompanion/media",
            "backend/billing-service/src/main/java/com/virtualcompanion/billing",
            "backend/moderation-service/src/main/java/com/virtualcompanion/moderation",
            "backend/gateway/src/main/java/com/virtualcompanion/gateway",
            "backend/common/src/main/java/com/virtualcompanion/common",
            
            // Frontend
            "frontend/web-app/src/components",
            "frontend/web-app/src/pages",
            "frontend/web-app/src/hooks",
            "frontend/web-app/src/services",
            "frontend/web-app/src/store",
            "frontend/web-app/src/styles",
            "frontend/web-app/src/types",
            "frontend/web-app/public",
            "frontend/mobile-app/src",
            
            // Infrastructure
            "infrastructure/docker",
            "infrastructure/kubernetes",
            "infrastructure/terraform",
            "infrastructure/scripts",
            
            // Documentation
            "docs/api",
            "docs/architecture",
            "docs/deployment",
            
            // Tests
            "tests/integration",
            "tests/e2e",
            "tests/performance"
        );
        
        for (String dir : directories) {
            Files.createDirectories(outputDirectory.resolve(dir));
        }
        
        System.out.println("✅ Structure de base créée");
    }

    /**
     * Traite un fichier source et extrait son contenu
     */
    private void processSourceFile(Path sourceFile) throws IOException {
        String content = Files.readString(sourceFile, StandardCharsets.UTF_8);
        
        // Détecter le type de contenu
        if (sourceFile.toString().endsWith(".md")) {
            processMarkdownFile(content);
        } else if (content.contains("```")) {
            processCodeBlockFile(content);
        } else {
            processPlainFile(sourceFile, content);
        }
    }

    /**
     * Traite un fichier Markdown contenant des blocs de code
     */
    private void processMarkdownFile(String content) throws IOException {
        // Extraire les blocs de code
        Matcher matcher = CODE_BLOCK_PATTERN.matcher(content);
        
        while (matcher.find()) {
            String language = matcher.group(1);
            String codeContent = matcher.group(2);
            
            // Chercher le nom du fichier dans le code
            String fileName = extractFileName(codeContent);
            if (fileName != null) {
                writeFile(fileName, codeContent, language);
            }
        }
        
        // Traiter aussi les sections de code inline
        processInlineCode(content);
    }

    /**
     * Traite les sections de code inline dans le format:
     * // path/to/file.ext
     * code content
     */
    private void processInlineCode(String content) throws IOException {
        String[] lines = content.split("\n");
        String currentFile = null;
        StringBuilder currentContent = new StringBuilder();
        
        for (String line : lines) {
            // Détecter un nouveau fichier
            Matcher fileMatcher = FILE_HEADER_PATTERN.matcher(line);
            if (fileMatcher.find() && line.contains("/") && line.contains(".")) {
                // Sauvegarder le fichier précédent
                if (currentFile != null && currentContent.length() > 0) {
                    writeFile(currentFile, currentContent.toString(), null);
                }
                
                // Nouveau fichier
                currentFile = fileMatcher.group(1).trim();
                currentContent = new StringBuilder();
            } else if (currentFile != null && !line.trim().isEmpty()) {
                // Ajouter le contenu au fichier courant
                currentContent.append(line).append("\n");
            }
        }
        
        // Sauvegarder le dernier fichier
        if (currentFile != null && currentContent.length() > 0) {
            writeFile(currentFile, currentContent.toString(), null);
        }
    }

    /**
     * Extrait le nom du fichier depuis le contenu
     */
    private String extractFileName(String content) {
        // Rechercher les patterns de nom de fichier
        String[] patterns = {
            "^\\s*//\\s*([\\w/\\-]+\\.[\\w\\.]+)",  // // filename.ext
            "^\\s*#\\s*([\\w/\\-]+\\.[\\w\\.]+)",    // # filename.ext
            "^\\s*/\\*\\s*([\\w/\\-]+\\.[\\w\\.]+)", // /* filename.ext
            "@file\\s+([\\w/\\-]+\\.[\\w\\.]+)"      // @file filename.ext
        };
        
        for (String pattern : patterns) {
            Pattern p = Pattern.compile(pattern, Pattern.MULTILINE);
            Matcher m = p.matcher(content);
            if (m.find()) {
                return m.group(1);
            }
        }
        
        return null;
    }

    /**
     * Écrit un fichier dans la structure du projet
     */
    private void writeFile(String relativePath, String content, String language) throws IOException {
        // Nettoyer le chemin
        relativePath = cleanPath(relativePath);
        
        // Déterminer le chemin complet basé sur le type de fichier
        Path fullPath = determineFullPath(relativePath, language);
        
        // Créer les répertoires parents
        Files.createDirectories(fullPath.getParent());
        
        // Nettoyer le contenu
        String cleanedContent = cleanContent(content, relativePath);
        
        // Écrire le fichier
        Files.writeString(fullPath, cleanedContent, StandardCharsets.UTF_8);
        
        // Mettre à jour les statistiques
        String extension = getFileExtension(relativePath);
        statistics.merge(extension, 1, Integer::sum);
        
        System.out.println("  ✓ Créé: " + fullPath.toString().replace(outputDirectory.toString(), ""));
    }

    /**
     * Détermine le chemin complet basé sur le type de fichier
     */
    private Path determineFullPath(String relativePath, String language) {
        // Si le chemin contient déjà la structure complète
        if (relativePath.contains("backend/") || relativePath.contains("frontend/") || 
            relativePath.contains("infrastructure/") || relativePath.contains("docs/")) {
            return outputDirectory.resolve(relativePath);
        }
        
        // Sinon, déterminer basé sur l'extension ou le langage
        String extension = getFileExtension(relativePath);
        FileType fileType = EXTENSION_MAP.getOrDefault(extension, FileType.OTHER);
        
        // Pour les fichiers Java, déterminer le service basé sur le package
        if (fileType == FileType.JAVA) {
            String service = determineJavaService(relativePath);
            return outputDirectory.resolve("backend").resolve(service).resolve("src/main/java").resolve(relativePath);
        }
        
        // Pour les autres types
        String baseDir = fileType.toString().toLowerCase();
        if (!baseDir.isEmpty()) {
            return outputDirectory.resolve(baseDir).resolve(relativePath);
        }
        
        return outputDirectory.resolve(relativePath);
    }

    /**
     * Détermine le service Java basé sur le nom du fichier
     */
    private String determineJavaService(String fileName) {
        if (fileName.contains("User") || fileName.contains("Auth")) return "user-service";
        if (fileName.contains("Character")) return "character-service";
        if (fileName.contains("Conversation") || fileName.contains("Message")) return "conversation-service";
        if (fileName.contains("Media")) return "media-service";
        if (fileName.contains("Billing") || fileName.contains("Subscription")) return "billing-service";
        if (fileName.contains("Moderation")) return "moderation-service";
        if (fileName.contains("Gateway")) return "gateway";
        return "common";
    }

    /**
     * Nettoie le chemin du fichier
     */
    private String cleanPath(String path) {
        return path.trim()
            .replaceAll("^[/\\\\]+", "")
            .replaceAll("[/\\\\]+", "/")
            .replaceAll("\\s+", "");
    }

    /**
     * Nettoie le contenu du fichier
     */
    private String cleanContent(String content, String fileName) {
        // Supprimer les marqueurs de fichier
        content = content.replaceAll("^\\s*//\\s*" + Pattern.quote(fileName) + "\\s*$", "");
        
        // Supprimer les lignes vides excessives
        content = content.replaceAll("\\n{3,}", "\n\n");
        
        // Trim
        return content.trim();
    }

    /**
     * Obtient l'extension d'un fichier
     */
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1).toLowerCase() : "";
    }

    /**
     * Crée les fichiers de configuration racine
     */
    private void createRootConfigFiles() throws IOException {
        // pom.xml racine
        writeFile("pom.xml", generateRootPom(), "xml");
        
        // docker-compose.yml
        writeFile("docker-compose.yml", generateDockerCompose(), "yaml");
        
        // .gitignore
        writeFile(".gitignore", generateGitignore(), null);
        
        // README.md
        writeFile("README.md", generateReadme(), "markdown");
        
        // .env.example
        writeFile(".env.example", generateEnvExample(), null);
    }

    /**
     * Génère le pom.xml racine
     */
    private String generateRootPom() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                     http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                
                <groupId>com.virtualcompanion</groupId>
                <artifactId>virtual-companion-parent</artifactId>
                <version>1.0.0-SNAPSHOT</version>
                <packaging>pom</packaging>
                
                <name>Virtual Companion Parent</name>
                <description>Parent POM for Virtual Companion microservices</description>
                
                <modules>
                    <module>backend/user-service</module>
                    <module>backend/character-service</module>
                    <module>backend/conversation-service</module>
                    <module>backend/media-service</module>
                    <module>backend/billing-service</module>
                    <module>backend/moderation-service</module>
                    <module>backend/gateway</module>
                    <module>backend/common</module>
                </modules>
                
                <properties>
                    <java.version>21</java.version>
                    <spring-boot.version>3.2.0</spring-boot.version>
                    <spring-cloud.version>2023.0.0</spring-cloud.version>
                    <maven.compiler.source>21</maven.compiler.source>
                    <maven.compiler.target>21</maven.compiler.target>
                    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                </properties>
            </project>
            """;
    }

    /**
     * Génère le docker-compose.yml
     */
    private String generateDockerCompose() {
        return """
            version: '3.8'
            
            services:
              postgres:
                image: postgres:15-alpine
                environment:
                  POSTGRES_DB: virtualcompanion
                  POSTGRES_USER: postgres
                  POSTGRES_PASSWORD: postgres
                ports:
                  - "5432:5432"
                volumes:
                  - postgres_data:/var/lib/postgresql/data
              
              redis:
                image: redis:7-alpine
                ports:
                  - "6379:6379"
                command: redis-server --appendonly yes
                volumes:
                  - redis_data:/data
              
              zookeeper:
                image: confluentinc/cp-zookeeper:latest
                environment:
                  ZOOKEEPER_CLIENT_PORT: 2181
                  ZOOKEEPER_TICK_TIME: 2000
              
              kafka:
                image: confluentinc/cp-kafka:latest
                depends_on:
                  - zookeeper
                ports:
                  - "9092:9092"
                environment:
                  KAFKA_BROKER_ID: 1
                  KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
                  KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
                  KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
              
              ollama:
                image: ollama/ollama:latest
                ports:
                  - "11434:11434"
                volumes:
                  - ollama_data:/root/.ollama
                deploy:
                  resources:
                    reservations:
                      devices:
                        - driver: nvidia
                          count: all
                          capabilities: [gpu]
            
            volumes:
              postgres_data:
              redis_data:
              ollama_data:
            """;
    }

    /**
     * Génère le .gitignore
     */
    private String generateGitignore() {
        return """
            # Java
            target/
            *.class
            *.jar
            *.war
            *.ear
            
            # IDE
            .idea/
            *.iml
            .vscode/
            .settings/
            .project
            .classpath
            
            # Node
            node_modules/
            dist/
            build/
            .next/
            
            # Environment
            .env
            .env.local
            .env.*.local
            
            # Logs
            *.log
            logs/
            
            # OS
            .DS_Store
            Thumbs.db
            
            # Test
            coverage/
            .nyc_output/
            
            # Temp
            *.tmp
            *.temp
            .cache/
            """;
    }

    /**
     * Génère le README.md
     */
    private String generateReadme() {
        return """
            # Virtual Companion
            
            Application de compagnon virtuel basée sur l'IA avec architecture microservices.
            
            ## 🚀 Quick Start
            
            ```bash
            # Cloner le repository
            git clone https://github.com/your-org/virtual-companion.git
            cd virtual-companion
            
            # Démarrer l'infrastructure
            docker-compose up -d
            
            # Installer les dépendances backend
            mvn clean install
            
            # Installer les dépendances frontend
            cd frontend/web-app
            npm install
            
            # Démarrer les services
            ./scripts/start-all.sh
            ```
            
            ## 📁 Structure du Projet
            
            ```
            virtual-companion/
            ├── backend/           # Services Java Spring Boot
            ├── frontend/          # Applications React/Next.js
            ├── infrastructure/    # Docker, K8s, Terraform
            ├── docs/             # Documentation
            └── tests/            # Tests E2E et performance
            ```
            
            ## 🛠️ Technologies
            
            - Backend: Java 21, Spring Boot 3.2
            - Frontend: Next.js 14, React 18, TypeScript
            - Database: PostgreSQL 15, Redis 7
            - IA: Ollama, Stable Diffusion, TTS
            - Infrastructure: Docker, Kubernetes
            
            ## 📖 Documentation
            
            Voir le dossier [docs/](./docs) pour la documentation complète.
            """;
    }

    /**
     * Génère le .env.example
     */
    private String generateEnvExample() {
        return """
            # Database
            DATABASE_URL=postgresql://postgres:postgres@localhost:5432/virtualcompanion
            REDIS_URL=redis://localhost:6379
            
            # Security
            JWT_SECRET=your-secret-key-change-this
            ENCRYPTION_KEY=your-encryption-key
            
            # External Services
            STRIPE_API_KEY=sk_test_...
            STRIPE_WEBHOOK_SECRET=whsec_...
            AWS_ACCESS_KEY_ID=AKIA...
            AWS_SECRET_ACCESS_KEY=...
            
            # AI Services
            OLLAMA_API_URL=http://localhost:11434
            STABLE_DIFFUSION_URL=http://localhost:7860
            
            # Application
            NODE_ENV=development
            API_PORT=8080
            FRONTEND_URL=http://localhost:3000
            """;
    }

    /**
     * Affiche les statistiques de reconstruction
     */
    private void printStatistics() {
        System.out.println("\n📊 Statistiques de Reconstruction:");
        System.out.println("==================================");
        
        int total = 0;
        for (Map.Entry<String, Integer> entry : statistics.entrySet()) {
            System.out.printf("  %s: %d fichiers\n", 
                entry.getKey().isEmpty() ? "autres" : entry.getKey(), 
                entry.getValue());
            total += entry.getValue();
        }
        
        System.out.println("----------------------------------");
        System.out.printf("  Total: %d fichiers créés\n", total);
        System.out.println("\n✅ Reconstruction terminée avec succès!");
        System.out.println("📁 Projet disponible dans: " + outputDirectory.toAbsolutePath());
    }

    /**
     * Traite un fichier contenant des blocs de code
     */
    private void processCodeBlockFile(String content) throws IOException {
        processMarkdownFile(content);
    }

    /**
     * Traite un fichier simple
     */
    private void processPlainFile(Path sourceFile, String content) throws IOException {
        String fileName = sourceFile.getFileName().toString();
        writeFile(fileName, content, null);
    }

    /**
     * Main method pour tester la reconstruction
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java ProjectReconstructor <output-dir> <source-files...>");
            System.exit(1);
        }
        
        try {
            ProjectReconstructor reconstructor = new ProjectReconstructor(args[0]);
            
            List<Path> sourceFiles = new ArrayList<>();
            for (int i = 1; i < args.length; i++) {
                Path sourcePath = Paths.get(args[i]);
                if (Files.isDirectory(sourcePath)) {
                    // Traiter tous les fichiers du répertoire
                    Files.walk(sourcePath)
                        .filter(Files::isRegularFile)
                        .forEach(sourceFiles::add);
                } else {
                    sourceFiles.add(sourcePath);
                }
            }
            
            reconstructor.reconstructProject(sourceFiles);
            
        } catch (IOException e) {
            System.err.println("Erreur lors de la reconstruction: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
