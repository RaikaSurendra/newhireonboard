.PHONY: help install build clean test run dev stop logs docker-up docker-down db-setup db-migrate frontend backend all rebuild deploy

# Default target
.DEFAULT_GOAL := help

# Colors for output
BLUE := \033[0;34m
GREEN := \033[0;32m
YELLOW := \033[0;33m
RED := \033[0;31m
NC := \033[0m # No Color

# Project variables
PROJECT_NAME := onboard-buddy
BACKEND_DIR := backend
FRONTEND_DIR := frontend
DATABASE_DIR := database
DOCKER_DIR := docker

# Java/Maven variables
MAVEN := mvn
JAVA := java
JAR_FILE := $(BACKEND_DIR)/target/onboard-buddy-1.0.0.jar

# Node/NPM variables
NPM := npm
NODE := node

# Database variables
MYSQL := mysql
DB_HOST := localhost
DB_PORT := 3306
DB_NAME := onboard_buddy
DB_USER := appuser

##@ Help

help: ## Display this help message
	@echo "$(BLUE)$(PROJECT_NAME) - Build System$(NC)"
	@echo ""
	@awk 'BEGIN {FS = ":.*##"; printf "\nUsage:\n  make $(YELLOW)<target>$(NC)\n"} /^[a-zA-Z_0-9-]+:.*?##/ { printf "  $(GREEN)%-20s$(NC) %s\n", $$1, $$2 } /^##@/ { printf "\n$(BLUE)%s$(NC)\n", substr($$0, 5) } ' $(MAKEFILE_LIST)

##@ Installation

install: ## Install all dependencies (backend + frontend)
	@echo "$(BLUE)Installing dependencies...$(NC)"
	@$(MAKE) install-backend
	@$(MAKE) install-frontend
	@echo "$(GREEN)✓ All dependencies installed$(NC)"

install-backend: ## Install backend dependencies
	@echo "$(BLUE)Installing backend dependencies...$(NC)"
	@cd $(BACKEND_DIR) && $(MAVEN) dependency:resolve
	@echo "$(GREEN)✓ Backend dependencies installed$(NC)"

install-frontend: ## Install frontend dependencies
	@echo "$(BLUE)Installing frontend dependencies...$(NC)"
	@cd $(FRONTEND_DIR) && $(NPM) install
	@echo "$(GREEN)✓ Frontend dependencies installed$(NC)"

##@ Build

build: ## Build entire application (backend + frontend)
	@echo "$(BLUE)Building application...$(NC)"
	@$(MAKE) build-frontend
	@$(MAKE) build-backend
	@echo "$(GREEN)✓ Build complete$(NC)"

build-backend: ## Build backend only
	@echo "$(BLUE)Building backend...$(NC)"
	@cd $(BACKEND_DIR) && $(MAVEN) clean package -DskipTests
	@echo "$(GREEN)✓ Backend built: $(JAR_FILE)$(NC)"

build-frontend: ## Build frontend only
	@echo "$(BLUE)Building frontend...$(NC)"
	@cd $(FRONTEND_DIR) && $(NPM) run build
	@echo "$(GREEN)✓ Frontend built$(NC)"

rebuild: clean build ## Clean and rebuild everything

##@ Development

dev: ## Start development servers (backend + frontend in parallel)
	@echo "$(BLUE)Starting development servers...$(NC)"
	@trap 'kill 0' EXIT; \
	$(MAKE) dev-backend & \
	$(MAKE) dev-frontend & \
	wait

dev-backend: ## Start backend in development mode
	@echo "$(BLUE)Starting backend dev server on port 8080...$(NC)"
	@cd $(BACKEND_DIR) && $(MAVEN) exec:java -Dexec.mainClass="com.onboardbuddy.Application"

dev-frontend: ## Start frontend in development mode
	@echo "$(BLUE)Starting frontend dev server on port 3000...$(NC)"
	@cd $(FRONTEND_DIR) && $(NPM) run dev

##@ Run

run: build ## Build and run the application
	@echo "$(BLUE)Starting application...$(NC)"
	@$(JAVA) -jar $(JAR_FILE)

run-jar: ## Run the application from existing JAR
	@echo "$(BLUE)Running application...$(NC)"
	@if [ -f "$(JAR_FILE)" ]; then \
		$(JAVA) -jar $(JAR_FILE); \
	else \
		echo "$(RED)✗ JAR file not found. Run 'make build' first.$(NC)"; \
		exit 1; \
	fi

##@ Testing

test: ## Run all tests
	@echo "$(BLUE)Running tests...$(NC)"
	@$(MAKE) test-backend
	@$(MAKE) test-frontend
	@echo "$(GREEN)✓ All tests passed$(NC)"

test-backend: ## Run backend tests
	@echo "$(BLUE)Running backend tests...$(NC)"
	@cd $(BACKEND_DIR) && $(MAVEN) test

test-frontend: ## Run frontend tests
	@echo "$(BLUE)Running frontend tests...$(NC)"
	@cd $(FRONTEND_DIR) && $(NPM) test || echo "$(YELLOW)⚠ No frontend tests configured$(NC)"

test-integration: ## Run integration tests
	@echo "$(BLUE)Running integration tests...$(NC)"
	@cd $(BACKEND_DIR) && $(MAVEN) verify

test-load: ## Run load tests with Gatling
	@echo "$(BLUE)Running load tests...$(NC)"
	@cd $(BACKEND_DIR) && $(MAVEN) gatling:test

##@ Database

db-setup: ## Setup database and create schema
	@echo "$(BLUE)Setting up database...$(NC)"
	@if ! command -v $(MYSQL) &> /dev/null; then \
		echo "$(RED)✗ MySQL not found. Please install MySQL first.$(NC)"; \
		exit 1; \
	fi
	@echo "$(YELLOW)Please enter MySQL root password:$(NC)"
	@$(MYSQL) -u root -p -e "CREATE DATABASE IF NOT EXISTS $(DB_NAME);"
	@$(MYSQL) -u root -p -e "CREATE USER IF NOT EXISTS '$(DB_USER)'@'localhost' IDENTIFIED BY 'apppassword';"
	@$(MYSQL) -u root -p -e "GRANT ALL PRIVILEGES ON $(DB_NAME).* TO '$(DB_USER)'@'localhost';"
	@$(MYSQL) -u root -p -e "FLUSH PRIVILEGES;"
	@$(MAKE) db-migrate
	@echo "$(GREEN)✓ Database setup complete$(NC)"

db-migrate: ## Run database migrations
	@echo "$(BLUE)Running database migrations...$(NC)"
	@$(MYSQL) -u $(DB_USER) -p $(DB_NAME) < $(DATABASE_DIR)/schema.sql
	@echo "$(GREEN)✓ Database migrated$(NC)"

db-reset: ## Reset database (drop and recreate)
	@echo "$(RED)⚠ This will delete all data!$(NC)"
	@echo "$(YELLOW)Please enter MySQL root password:$(NC)"
	@$(MYSQL) -u root -p -e "DROP DATABASE IF EXISTS $(DB_NAME);"
	@$(MAKE) db-setup

db-backup: ## Backup database
	@echo "$(BLUE)Backing up database...$(NC)"
	@mkdir -p backups
	@mysqldump -u $(DB_USER) -p $(DB_NAME) > backups/$(DB_NAME)_$$(date +%Y%m%d_%H%M%S).sql
	@echo "$(GREEN)✓ Database backed up$(NC)"

##@ Docker

docker-build: ## Build Docker images
	@echo "$(BLUE)Building Docker images...$(NC)"
	@docker-compose build
	@echo "$(GREEN)✓ Docker images built$(NC)"

docker-up: ## Start application with Docker Compose
	@echo "$(BLUE)Starting Docker containers...$(NC)"
	@docker-compose up -d
	@echo "$(GREEN)✓ Containers started$(NC)"
	@echo "$(BLUE)Application: http://localhost:8080$(NC)"

docker-down: ## Stop Docker containers
	@echo "$(BLUE)Stopping Docker containers...$(NC)"
	@docker-compose down
	@echo "$(GREEN)✓ Containers stopped$(NC)"

docker-logs: ## View Docker logs
	@docker-compose logs -f

docker-clean: ## Remove Docker containers and volumes
	@echo "$(BLUE)Cleaning Docker resources...$(NC)"
	@docker-compose down -v
	@echo "$(GREEN)✓ Docker resources cleaned$(NC)"

##@ Cleanup

clean: ## Clean build artifacts
	@echo "$(BLUE)Cleaning build artifacts...$(NC)"
	@cd $(BACKEND_DIR) && $(MAVEN) clean
	@cd $(FRONTEND_DIR) && rm -rf dist node_modules/.vite
	@rm -rf logs/*.log
	@echo "$(GREEN)✓ Cleaned$(NC)"

clean-all: clean ## Deep clean (including dependencies)
	@echo "$(BLUE)Deep cleaning...$(NC)"
	@cd $(FRONTEND_DIR) && rm -rf node_modules
	@rm -rf $(BACKEND_DIR)/target
	@rm -rf logs
	@echo "$(GREEN)✓ Deep clean complete$(NC)"

##@ Logs

logs: ## Tail application logs
	@echo "$(BLUE)Tailing logs...$(NC)"
	@tail -f logs/onboard-buddy.log

logs-error: ## Tail error logs
	@echo "$(BLUE)Tailing error logs...$(NC)"
	@tail -f logs/error.log

##@ Utilities

lint: ## Run linters
	@echo "$(BLUE)Running linters...$(NC)"
	@cd $(FRONTEND_DIR) && $(NPM) run lint || echo "$(YELLOW)⚠ Linting issues found$(NC)"

format: ## Format code
	@echo "$(BLUE)Formatting code...$(NC)"
	@cd $(BACKEND_DIR) && $(MAVEN) formatter:format || echo "$(YELLOW)⚠ No formatter configured$(NC)"
	@cd $(FRONTEND_DIR) && $(NPM) run format || echo "$(YELLOW)⚠ No formatter configured$(NC)"

check: ## Run all checks (lint, test)
	@$(MAKE) lint
	@$(MAKE) test

version: ## Display version information
	@echo "$(BLUE)Version Information:$(NC)"
	@echo "Project: $(PROJECT_NAME) v1.0.0"
	@echo "Java: $$(java -version 2>&1 | head -n 1)"
	@echo "Maven: $$(mvn -version | head -n 1)"
	@echo "Node: $$(node --version)"
	@echo "NPM: $$(npm --version)"

status: ## Check application status
	@echo "$(BLUE)Application Status:$(NC)"
	@if lsof -Pi :8080 -sTCP:LISTEN -t >/dev/null ; then \
		echo "$(GREEN)✓ Backend running on port 8080$(NC)"; \
	else \
		echo "$(RED)✗ Backend not running$(NC)"; \
	fi
	@if lsof -Pi :3000 -sTCP:LISTEN -t >/dev/null ; then \
		echo "$(GREEN)✓ Frontend running on port 3000$(NC)"; \
	else \
		echo "$(RED)✗ Frontend not running$(NC)"; \
	fi

stop: ## Stop all running processes
	@echo "$(BLUE)Stopping processes...$(NC)"
	@-lsof -ti:8080 | xargs kill -9 2>/dev/null || true
	@-lsof -ti:3000 | xargs kill -9 2>/dev/null || true
	@echo "$(GREEN)✓ Processes stopped$(NC)"

##@ Quick Start

quickstart: install db-setup build run ## Complete setup and run (first time)

all: clean install build test ## Build everything from scratch
