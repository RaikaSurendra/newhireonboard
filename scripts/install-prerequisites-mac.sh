#!/bin/bash

# Onboarding Buddy Application - Prerequisites Installation Script
# For macOS using Homebrew

set -e  # Exit on error

echo "🍺 Installing Prerequisites using Homebrew"
echo "==========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# Check if Homebrew is installed
if ! command -v brew &> /dev/null; then
    print_info "Homebrew not found. Installing Homebrew..."
    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
    
    # Add Homebrew to PATH for Apple Silicon Macs
    if [[ $(uname -m) == 'arm64' ]]; then
        echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> ~/.zprofile
        eval "$(/opt/homebrew/bin/brew shellenv)"
    fi
    
    print_success "Homebrew installed"
else
    print_success "Homebrew already installed"
    print_info "Updating Homebrew..."
    brew update
fi

echo ""
echo "Installing required packages..."
echo ""

# ============================================================
# 1. Install Java 11 (OpenJDK)
# ============================================================

echo "1️⃣  Installing Java 11 (OpenJDK)..."
if brew list openjdk@11 &>/dev/null; then
    print_success "Java 11 already installed"
else
    brew install openjdk@11
    
    # Create symlink for system Java wrappers
    sudo ln -sfn $(brew --prefix)/opt/openjdk@11/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-11.jdk
    
    # Add to PATH
    echo 'export PATH="$(brew --prefix)/opt/openjdk@11/bin:$PATH"' >> ~/.zshrc
    export PATH="$(brew --prefix)/opt/openjdk@11/bin:$PATH"
    
    print_success "Java 11 installed"
fi

# Verify Java installation
java -version 2>&1 | head -n 1
echo ""

# ============================================================
# 2. Install Node.js 18
# ============================================================

echo "2️⃣  Installing Node.js 18..."
if brew list node@18 &>/dev/null; then
    print_success "Node.js 18 already installed"
else
    brew install node@18
    
    # Link Node 18
    brew link node@18
    
    print_success "Node.js 18 installed"
fi

# Verify Node installation
node -v
npm -v
echo ""

# ============================================================
# 3. Install Maven
# ============================================================

echo "3️⃣  Installing Maven..."
if brew list maven &>/dev/null; then
    print_success "Maven already installed"
else
    brew install maven
    print_success "Maven installed"
fi

# Verify Maven installation
mvn -v | head -n 1
echo ""

# ============================================================
# 4. Install MySQL 8.0
# ============================================================

echo "4️⃣  Installing MySQL 8.0..."
if brew list mysql &>/dev/null; then
    print_success "MySQL already installed"
else
    brew install mysql
    
    # Start MySQL service
    brew services start mysql
    
    print_success "MySQL installed and started"
    print_warning "MySQL root password is empty by default"
    print_info "Run 'mysql_secure_installation' to set root password"
fi

# Check MySQL status
brew services list | grep mysql
echo ""

# ============================================================
# 5. Install Git (if not already installed)
# ============================================================

echo "5️⃣  Checking Git..."
if command -v git &> /dev/null; then
    print_success "Git already installed"
else
    brew install git
    print_success "Git installed"
fi

git --version
echo ""

# ============================================================
# 6. Install Docker Desktop (Optional)
# ============================================================

echo "6️⃣  Docker Desktop (Optional)..."
read -p "Do you want to install Docker Desktop? (y/n): " install_docker

if [ "$install_docker" = "y" ] || [ "$install_docker" = "Y" ]; then
    if brew list --cask docker &>/dev/null; then
        print_success "Docker Desktop already installed"
    else
        brew install --cask docker
        print_success "Docker Desktop installed"
        print_info "Please open Docker Desktop from Applications to complete setup"
    fi
else
    print_info "Skipping Docker Desktop installation"
fi

echo ""

# ============================================================
# 7. Install Visual Studio Code (Optional)
# ============================================================

echo "7️⃣  Visual Studio Code (Optional)..."
read -p "Do you want to install VS Code? (y/n): " install_vscode

if [ "$install_vscode" = "y" ] || [ "$install_vscode" = "Y" ]; then
    if brew list --cask visual-studio-code &>/dev/null; then
        print_success "VS Code already installed"
    else
        brew install --cask visual-studio-code
        print_success "VS Code installed"
    fi
else
    print_info "Skipping VS Code installation"
fi

echo ""

# ============================================================
# 8. Install IntelliJ IDEA Community Edition (Optional)
# ============================================================

echo "8️⃣  IntelliJ IDEA Community (Optional)..."
read -p "Do you want to install IntelliJ IDEA Community? (y/n): " install_intellij

if [ "$install_intellij" = "y" ] || [ "$install_intellij" = "Y" ]; then
    if brew list --cask intellij-idea-ce &>/dev/null; then
        print_success "IntelliJ IDEA already installed"
    else
        brew install --cask intellij-idea-ce
        print_success "IntelliJ IDEA Community installed"
    fi
else
    print_info "Skipping IntelliJ IDEA installation"
fi

echo ""
echo "==========================================="
echo -e "${GREEN}✅ Installation Complete!${NC}"
echo "==========================================="
echo ""

# Display installed versions
echo "📦 Installed Software Versions:"
echo "--------------------------------"
echo -n "Java:    " && java -version 2>&1 | head -n 1
echo -n "Node:    " && node -v
echo -n "npm:     " && npm -v
echo -n "Maven:   " && mvn -v | head -n 1
echo -n "MySQL:   " && mysql --version
echo -n "Git:     " && git --version

echo ""
echo "🔧 Next Steps:"
echo "1. Restart your terminal to apply PATH changes"
echo "2. Run: source ~/.zshrc"
echo "3. Secure MySQL: mysql_secure_installation"
echo "4. Run the setup script: ./scripts/setup.sh"
echo ""
echo "Happy coding! 🚀"
