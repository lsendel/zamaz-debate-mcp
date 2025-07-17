# Installing Docker on a Different Volume (IDEA)

## Option 1: Move Docker Data to IDEA Volume

1. **First, completely uninstall current Docker:**
   ```bash
   # Quit Docker Desktop
   osascript -e 'quit app "Docker"'
   
   # Remove Docker Desktop app
   sudo rm -rf /Applications/Docker.app
   
   # Remove Docker data
   rm -rf ~/Library/Group\ Containers/group.com.docker
   rm -rf ~/Library/Containers/com.docker.docker
   rm -rf ~/.docker
   ```

2. **Create Docker directory on IDEA volume:**
   ```bash
   # Assuming IDEA volume is mounted at /Volumes/IDEA
   sudo mkdir -p /Volumes/IDEA/Docker
   sudo chown -R $(whoami) /Volumes/IDEA/Docker
   ```

3. **Download Docker Desktop:**
   - Go to https://www.docker.com/products/docker-desktop/
   - Download Docker Desktop for Mac (Apple Silicon or Intel)

4. **Install Docker with custom location:**
   ```bash
   # Mount the DMG
   hdiutil attach ~/Downloads/Docker.dmg
   
   # Copy to IDEA volume instead of Applications
   cp -R /Volumes/Docker/Docker.app /Volumes/IDEA/Docker/
   
   # Unmount DMG
   hdiutil detach /Volumes/Docker
   
   # Create symlink in Applications (optional)
   ln -s /Volumes/IDEA/Docker/Docker.app /Applications/Docker.app
   ```

5. **Configure Docker to use IDEA volume for data:**
   
   Create a script to set Docker's data location:
   ```bash
   cat > /Volumes/IDEA/Docker/docker-config.sh << 'EOF'
   #!/bin/bash
   export DOCKER_CONFIG=/Volumes/IDEA/Docker/config
   export DOCKER_DATA_ROOT=/Volumes/IDEA/Docker/data
   mkdir -p $DOCKER_CONFIG $DOCKER_DATA_ROOT
   EOF
   
   chmod +x /Volumes/IDEA/Docker/docker-config.sh
   ```

## Option 2: Use Docker without Docker Desktop

Install Docker Engine directly using Homebrew and configure it to use IDEA volume:

1. **Install Docker using Homebrew:**
   ```bash
   # Install Homebrew if not already installed
   /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
   
   # Install docker and docker-compose
   brew install docker docker-compose colima
   ```

2. **Configure Colima to use IDEA volume:**
   ```bash
   # Start Colima with custom settings
   colima start --cpu 4 --memory 8 --disk 60 --mount /Volumes/IDEA/Docker:/var/lib/docker
   ```

3. **Set Docker context:**
   ```bash
   docker context use colima
   ```

## Option 3: Quick Fix - Use Docker in a VM

If Docker Desktop continues to have issues, use a lightweight VM:

1. **Install Multipass:**
   ```bash
   brew install multipass
   ```

2. **Create Ubuntu VM with Docker:**
   ```bash
   # Create VM with 8GB RAM and 40GB disk
   multipass launch --name docker-vm --cpus 4 --mem 8G --disk 40G
   
   # Install Docker in VM
   multipass exec docker-vm -- bash -c "curl -fsSL https://get.docker.com | sh"
   multipass exec docker-vm -- sudo usermod -aG docker ubuntu
   ```

3. **Mount your project in VM:**
   ```bash
   multipass mount /Users/lsendel/IdeaProjects/zamaz-debate-mcp docker-vm:/home/ubuntu/project
   ```

## Recommended: Fix Current Docker Installation

Since you already have the services partially running, try this first:

1. **Reset Docker Desktop completely:**
   - Open Docker Desktop
   - Click on the bug icon (Troubleshoot)
   - Select "Clean / Purge data"
   - Choose "Clean" (removes all containers/images)
   - Restart Docker Desktop

2. **If that doesn't work, reset preferences:**
   ```bash
   # Backup current settings
   cp ~/Library/Group\ Containers/group.com.docker/settings.json ~/Desktop/docker-settings-backup.json
   
   # Reset Docker Desktop
   osascript -e 'quit app "Docker"'
   rm ~/Library/Group\ Containers/group.com.docker/settings.json
   open -a Docker
   ```

3. **Check Docker logs for errors:**
   ```bash
   # View Docker Desktop logs
   tail -f ~/Library/Containers/com.docker.docker/Data/log/host/com.docker.driver.amd64-linux.log
   ```

Let me know which option you prefer and I'll help you implement it!