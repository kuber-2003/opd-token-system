# Setup and Troubleshooting Guide

## Prerequisites Installation

### 1. Install Java 17

#### Windows
1. Download OpenJDK 17 from: https://adoptium.net/
2. Run the installer
3. Verify installation:
   ```cmd
   java -version
   ```
   Should show: `openjdk version "17.x.x"`

#### macOS
```bash
brew install openjdk@17
```

#### Linux (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install openjdk-17-jdk
```

### 2. Install Maven

#### Windows
1. Download Maven from: https://maven.apache.org/download.cgi
2. Extract to `C:\Program Files\Apache\maven`
3. Add to PATH:
   - System Properties → Environment Variables
   - Add `C:\Program Files\Apache\maven\bin` to PATH
4. Verify:
   ```cmd
   mvn -version
   ```

#### macOS
```bash
brew install maven
```

#### Linux (Ubuntu/Debian)
```bash
sudo apt install maven
```

---

## Running the Project

### Method 1: Using the Quick Start Scripts

#### On Linux/macOS:
```bash
./run.sh
```

#### On Windows:
```cmd
run.bat
```

### Method 2: Using Maven Directly

#### Start the API Server:
```bash
mvn spring-boot:run
```

Then open: http://localhost:8080/swagger-ui.html

#### Run the Simulation:
```bash
mvn compile exec:java -Dexec.mainClass="com.medoc.opd.OPDSimulation"
```

### Method 3: Using IDE (IntelliJ IDEA)

1. **Import Project**:
   - File → Open → Select `opd-token-system` folder
   - Wait for Maven to download dependencies

2. **Run API**:
   - Open `OPDTokenSystemApplication.java`
   - Click green play button next to `main` method
   - Or right-click file → Run

3. **Run Simulation**:
   - Open `OPDSimulation.java`
   - Click green play button next to `main` method

---

## Common Issues and Solutions

### Issue 1: "Maven command not found"

**Problem**: Maven is not installed or not in PATH

**Solution**:
```bash
# Check if Maven is installed
mvn -version

# If not found, install Maven (see Prerequisites section above)
```

### Issue 2: "Java version mismatch"

**Problem**: Wrong Java version installed

**Solution**:
```bash
# Check Java version
java -version

# Should show version 17 or higher
# If not, install Java 17 (see Prerequisites section)

# If multiple Java versions, set JAVA_HOME
export JAVA_HOME=/path/to/java17  # Linux/macOS
set JAVA_HOME=C:\path\to\java17   # Windows
```

### Issue 3: "Port 8080 already in use"

**Problem**: Another application is using port 8080

**Solution 1** - Change port:
Edit `src/main/resources/application.properties`:
```properties
server.port=8081
```

**Solution 2** - Stop the other application:
```bash
# Linux/macOS - Find and kill process on port 8080
lsof -i :8080
kill -9 <PID>

# Windows - Find and kill process
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

### Issue 4: "Dependencies not downloading"

**Problem**: Maven cannot download dependencies

**Solution**:
```bash
# Clear Maven cache and retry
mvn clean install -U

# Or manually delete .m2 repository
rm -rf ~/.m2/repository  # Linux/macOS
rmdir /s %USERPROFILE%\.m2\repository  # Windows

# Then rebuild
mvn clean install
```

### Issue 5: "Lombok not working in IDE"

**Problem**: Annotations like @Data, @Slf4j not recognized

**Solution for IntelliJ IDEA**:
1. File → Settings → Plugins
2. Search for "Lombok"
3. Install Lombok plugin
4. Restart IDE
5. Settings → Build → Compiler → Annotation Processors
6. Enable "Enable annotation processing"

**Solution for Eclipse**:
1. Download lombok.jar from https://projectlombok.org/
2. Run: `java -jar lombok.jar`
3. Select Eclipse installation
4. Restart Eclipse

### Issue 6: "Application starts but Swagger UI not working"

**Problem**: Swagger UI page not loading

**Solution**:
1. Make sure application started successfully (check console logs)
2. Try different URL formats:
   - http://localhost:8080/swagger-ui.html
   - http://localhost:8080/swagger-ui/index.html
3. Check browser console for errors
4. Clear browser cache and retry

### Issue 7: "Simulation not showing output"

**Problem**: Simulation runs but no logs visible

**Solution**:
```bash
# Run with Maven output
mvn compile exec:java -Dexec.mainClass="com.medoc.opd.OPDSimulation"

# Check if SLF4J logging is configured
# Logs should appear in console with timestamps
```

---

## Verifying Installation

### 1. Test Java Installation
```bash
java -version
# Should show: openjdk version "17.x.x" or higher
```

### 2. Test Maven Installation
```bash
mvn -version
# Should show: Apache Maven 3.8.x or higher
```

### 3. Test Project Build
```bash
cd opd-token-system
mvn clean install
# Should complete with "BUILD SUCCESS"
```

### 4. Test Application Startup
```bash
mvn spring-boot:run
# Should show: "Started OPDTokenSystemApplication"
# Visit: http://localhost:8080/swagger-ui.html
```

---

## IDE-Specific Setup

### IntelliJ IDEA (Recommended)

1. **Import Project**:
   - File → Open → Select project folder
   - Choose "Import as Maven project"
   - Wait for indexing to complete

2. **Configure JDK**:
   - File → Project Structure → Project
   - Set SDK to Java 17
   - Set language level to 17

3. **Enable Lombok**:
   - Settings → Plugins → Install "Lombok"
   - Settings → Build → Annotation Processors → Enable

4. **Run Configuration**:
   - Run → Edit Configurations → Add New → Spring Boot
   - Main class: `com.medoc.opd.OPDTokenSystemApplication`
   - Module: opd-token-system

### Visual Studio Code

1. **Install Extensions**:
   - Java Extension Pack
   - Spring Boot Extension Pack
   - Lombok Annotations Support

2. **Open Project**:
   - File → Open Folder → Select project folder
   - VS Code will detect Maven project

3. **Run**:
   - Open `OPDTokenSystemApplication.java`
   - Click "Run" above main method
   - Or use terminal: `mvn spring-boot:run`

### Eclipse

1. **Import Project**:
   - File → Import → Existing Maven Projects
   - Select project folder
   - Finish

2. **Install Lombok**:
   - Download lombok.jar
   - Run: `java -jar lombok.jar`
   - Point to Eclipse installation

3. **Run**:
   - Right-click project → Run As → Spring Boot App

---

## Testing Checklist

After setup, verify everything works:

- [ ] Maven builds successfully: `mvn clean install`
- [ ] Application starts: `mvn spring-boot:run`
- [ ] Swagger UI loads: http://localhost:8080/swagger-ui.html
- [ ] Simulation runs: `mvn compile exec:java -Dexec.mainClass="com.medoc.opd.OPDSimulation"`
- [ ] Can create slot via API (test in Swagger)
- [ ] Can allocate token via API
- [ ] Can view queue via API

---

## Performance Tips

### 1. Increase Maven Memory
```bash
export MAVEN_OPTS="-Xmx1024m"  # Linux/macOS
set MAVEN_OPTS=-Xmx1024m       # Windows
```

### 2. Parallel Builds
```bash
mvn clean install -T 4  # Use 4 threads
```

### 3. Skip Tests (for faster builds)
```bash
mvn clean install -DskipTests
```

---

## Getting Help

If you encounter issues not covered here:

1. **Check the logs** - Most errors are clearly explained in console output
2. **Review README.md** - Contains usage examples
3. **Check DOCUMENTATION.md** - Detailed technical explanations
4. **Search error message** - Often others have faced similar issues
5. **Verify prerequisites** - Ensure Java 17+ and Maven 3.8+ are installed

---

## Next Steps

Once setup is complete:

1. **Run the simulation** to see the system in action
2. **Explore Swagger UI** to test API endpoints interactively
3. **Read DOCUMENTATION.md** to understand the algorithms
4. **Review the code** to see implementation details
5. **Customize and extend** as needed

---

**Ready to Start!** 🚀

Run `./run.sh` (Linux/Mac) or `run.bat` (Windows) to begin!
