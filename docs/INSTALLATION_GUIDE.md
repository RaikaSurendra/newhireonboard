# 🚀 Installation Guide - Onboarding Buddy Application

## Quick Install (macOS with Homebrew)

### One-Command Installation

```bash
# Make script executable and run
chmod +x scripts/install-prerequisites-mac.sh
./scripts/install-prerequisites-mac.sh
```

This will install:
- ☕ Java 11 (OpenJDK)
- 📦 Node.js 18
- 🔨 Maven
- 🗄️ MySQL 8.0
- 📝 Git
- 🐳 Docker Desktop (optional)
- 💻 VS Code (optional)
- 🧠 IntelliJ IDEA (optional)

---

## Manual Installation (macOS)

### 1. Install Homebrew (if not installed)

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

For Apple Silicon Macs, add to PATH:
```bash
echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> ~/.zprofile
eval "$(/opt/homebrew/bin/brew shellenv)"
```

### 2. Install Java 11

```bash
brew install openjdk@11

# Create symlink
sudo ln -sfn $(brew --prefix)/opt/openjdk@11/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-11.jdk

# Add to PATH
echo 'export PATH="$(brew --prefix)/opt/openjdk@11/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc

# Verify
java -version
```

### 3. Install Node.js 18

```bash
brew install node@18
brew link node@18

# Verify
node -v
npm -v
```

### 4. Install Maven

```bash
brew install maven

# Verify
mvn -v
```

### 5. Install MySQL 8.0

```bash
brew install mysql

# Start MySQL service
brew services start mysql

# Secure installation (set root password)
mysql_secure_installation

# Verify
mysql --version
```

### 6. Install Git

```bash
brew install git

# Verify
git --version
```

### 7. Install Docker Desktop (Optional)

```bash
brew install --cask docker

# Open Docker Desktop from Applications
open /Applications/Docker.app
```

### 8. Install IDEs (Optional)

**Visual Studio Code:**
```bash
brew install --cask visual-studio-code
```

**IntelliJ IDEA Community:**
```bash
brew install --cask intellij-idea-ce
```

---

## Verification

Run this command to verify all installations:

```bash
echo "Java:    $(java -version 2>&1 | head -n 1)"
echo "Node:    $(node -v)"
echo "npm:     $(npm -v)"
echo "Maven:   $(mvn -v | head -n 1)"
echo "MySQL:   $(mysql --version)"
echo "Git:     $(git --version)"
```

Expected output:
```
Java:    openjdk version "11.0.x"
Node:    v18.x.x
npm:     9.x.x
Maven:   Apache Maven 3.x.x
MySQL:   mysql  Ver 8.0.x
Git:     git version 2.x.x
```

---

## Post-Installation Setup

### 1. Configure MySQL

```bash
# Login to MySQL
mysql -u root -p

# Create database and user
CREATE DATABASE onboard_buddy;
CREATE USER 'appuser'@'localhost' IDENTIFIED BY 'apppassword';
GRANT ALL PRIVILEGES ON onboard_buddy.* TO 'appuser'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

### 2. Set Environment Variables

Add to `~/.zshrc`:

```bash
# Java
export JAVA_HOME=$(/usr/libexec/java_home -v 11)
export PATH="$JAVA_HOME/bin:$PATH"

# Maven
export M2_HOME=$(brew --prefix)/opt/maven
export PATH="$M2_HOME/bin:$PATH"

# Node
export PATH="$(brew --prefix)/opt/node@18/bin:$PATH"
```

Apply changes:
```bash
source ~/.zshrc
```

### 3. Configure Git

```bash
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"
```

---

## Troubleshooting

### Issue: Homebrew command not found

**Solution:**
```bash
# For Intel Macs
echo 'eval "$(/usr/local/bin/brew shellenv)"' >> ~/.zshrc

# For Apple Silicon Macs
echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> ~/.zshrc

source ~/.zshrc
```

### Issue: Java not found after installation

**Solution:**
```bash
# Set JAVA_HOME
export JAVA_HOME=$(/usr/libexec/java_home -v 11)
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 11)' >> ~/.zshrc
source ~/.zshrc
```

### Issue: MySQL connection refused

**Solution:**
```bash
# Start MySQL service
brew services start mysql

# Check status
brew services list | grep mysql

# If still not working, restart
brew services restart mysql
```

### Issue: Port 8080 already in use

**Solution:**
```bash
# Find process using port 8080
lsof -i :8080

# Kill the process
kill -9 <PID>
```

### Issue: Permission denied for MySQL

**Solution:**
```bash
# Reset MySQL root password
mysql.server stop
mysqld_safe --skip-grant-tables &
mysql -u root

# In MySQL prompt:
FLUSH PRIVILEGES;
ALTER USER 'root'@'localhost' IDENTIFIED BY 'newpassword';
EXIT;

# Restart MySQL
mysql.server restart
```

---

## Alternative Installation Methods

### Using SDKMAN (for Java and Maven)

```bash
# Install SDKMAN
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# Install Java
sdk install java 11.0.20-tem

# Install Maven
sdk install maven 3.9.5
```

### Using NVM (for Node.js)

```bash
# Install NVM
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash

# Install Node 18
nvm install 18
nvm use 18
nvm alias default 18
```

---

## System Requirements

### Minimum Requirements
- **OS**: macOS 10.15 (Catalina) or later
- **RAM**: 8 GB
- **Disk Space**: 10 GB free
- **Processor**: Intel Core i5 or Apple Silicon M1/M2

### Recommended Requirements
- **OS**: macOS 12 (Monterey) or later
- **RAM**: 16 GB
- **Disk Space**: 20 GB free
- **Processor**: Intel Core i7 or Apple Silicon M1 Pro/M2 Pro

---

## Next Steps

After installing all prerequisites:

1. **Run the project setup:**
   ```bash
   ./scripts/setup.sh
   ```

2. **Or follow manual setup:**
   - See [PROJECT_SETUP.md](PROJECT_SETUP.md)

3. **Start development:**
   ```bash
   # Backend
   cd backend && mvn spring-boot:run
   
   # Frontend (in new terminal)
   cd frontend && npm run dev
   ```

---

## Useful Commands

### Homebrew
```bash
brew update              # Update Homebrew
brew upgrade             # Upgrade all packages
brew list                # List installed packages
brew info <package>      # Get package info
brew uninstall <package> # Uninstall package
```

### MySQL
```bash
brew services start mysql    # Start MySQL
brew services stop mysql     # Stop MySQL
brew services restart mysql  # Restart MySQL
mysql -u root -p            # Login to MySQL
```

### Maven
```bash
mvn clean install       # Build project
mvn clean package       # Create JAR
mvn dependency:tree     # Show dependencies
```

### Node/npm
```bash
npm install            # Install dependencies
npm run dev           # Run dev server
npm run build         # Build for production
npm list              # List installed packages
```

---

## Support

If you encounter any issues:
1. Check the troubleshooting section above
2. Review [PROJECT_SETUP.md](PROJECT_SETUP.md)
3. Create an issue on GitHub

---

**Installation complete! Ready to build! 🎉**
