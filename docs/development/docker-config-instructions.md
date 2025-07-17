# Docker Desktop Configuration for macOS

## Steps to Configure Docker Engine:

1. **Open Docker Desktop**
   - Click on the Docker icon in your menu bar
   - Select "Preferences" or "Settings"

2. **Navigate to Docker Engine**
   - In the settings window, click on "Docker Engine" tab

3. **Replace the content with this configuration:**

```json
{
  "builder": {
    "gc": {
      "defaultKeepStorage": "20GB",
      "enabled": true
    }
  },
  "experimental": false,
  "features": {
    "buildkit": true
  },
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  },
  "dns": ["8.8.8.8", "8.8.4.4"],
  "default-address-pools": [
    {
      "base": "172.80.0.0/16",
      "size": 24
    }
  ],
  "insecure-registries": [],
  "registry-mirrors": [],
  "debug": false,
  "storage-driver": "overlay2"
}
```

## What each setting does:

- **builder.gc**: Enables garbage collection for build cache (keeps it under 20GB)
- **features.buildkit**: Enables BuildKit for better build performance
- **log-driver & log-opts**: Limits container logs to prevent disk space issues
- **dns**: Uses Google's DNS servers for reliability
- **default-address-pools**: Defines the IP range for Docker networks
- **storage-driver**: Uses overlay2 for better performance on macOS

## Additional Recommended Settings:

If you're having resource issues, you can also adjust these in the "Resources" tab:
- **CPUs**: 4-6 (depending on your Mac)
- **Memory**: 8-16 GB (depending on available RAM)
- **Swap**: 1-2 GB
- **Disk image size**: 60-100 GB

## After Making Changes:

1. Click "Apply & Restart"
2. Wait for Docker to restart
3. Verify it's working: `docker info`

## For Your Project Specifically:

Since you're running multiple MCP services, you might want to increase resources:
- Memory: At least 8GB
- CPUs: At least 4
- Ensure you have enough disk space for PostgreSQL, Redis, and Qdrant data