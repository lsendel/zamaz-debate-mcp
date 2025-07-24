#!/usr/bin/env node

const fs = require('fs');
const yaml = require('js-yaml');
const { execSync } = require('child_process');

class EnvironmentProvisioner {
  constructor(options) {
    this.configFile = options.config;
    this.environmentName = options.environment;
    this.namespace = options.namespace;
    this.cleanupToken = options.cleanupToken;
    
    // Load configuration
    this.config = yaml.load(fs.readFileSync(this.configFile, 'utf8'));
    this.environment = this.config.environments[this.environmentName];
    
    if (!this.environment) {
      throw new Error(`Environment '${this.environmentName}' not found in configuration`);
    }
    
    this.endpoints = {};
    this.containers = [];
  }

  async provision() {
    console.log(`🚀 Provisioning ${this.environmentName} environment in namespace ${this.namespace}`);
    
    try {
      // Create Docker network
      await this.createNetwork();
      
      // Provision services
      await this.provisionServices();
      
      // Run initialization scripts
      await this.runInitialization();
      
      // Save endpoints
      this.saveEndpoints();
      
      // Save provisioning metadata
      this.saveMetadata();
      
      console.log('✅ Environment provisioned successfully!');
      
    } catch (error) {
      console.error('❌ Provisioning failed:', error);
      await this.cleanup();
      throw error;
    }
  }

  async createNetwork() {
    const networkName = `${this.namespace}-network`;
    
    try {
      execSync(`docker network create ${networkName}`, { stdio: 'pipe' });
      console.log(`📡 Created network: ${networkName}`);
    } catch (error) {
      // Network might already exist
      console.log(`📡 Using existing network: ${networkName}`);
    }
    
    this.networkName = networkName;
  }

  async provisionServices() {
    const services = this.environment.services || {};
    
    for (const [serviceName, serviceConfig] of Object.entries(services)) {
      await this.provisionService(serviceName, serviceConfig);
    }
  }

  async provisionService(name, config) {
    console.log(`  🔧 Provisioning ${name}...`);
    
    const containerName = `${this.namespace}-${name}`;
    const image = config.image || name;
    
    // Build docker run command
    let dockerCmd = `docker run -d --name ${containerName} --network ${this.networkName}`;
    
    // Add environment variables
    if (config.env) {
      for (const [key, value] of Object.entries(config.env)) {
        dockerCmd += ` -e ${key}="${value}"`;
      }
    }
    
    // Add port mappings and capture assigned ports
    const portMappings = {};
    if (config.ports) {
      for (const port of config.ports) {
        const hostPort = this.getRandomPort();
        dockerCmd += ` -p ${hostPort}:${port}`;
        portMappings[port] = hostPort;
      }
    }
    
    // Add volumes
    if (config.volumes) {
      for (const volume of config.volumes) {
        dockerCmd += ` -v ${volume}`;
      }
    }
    
    // Add command
    if (config.command) {
      dockerCmd += ` ${image} ${config.command}`;
    } else {
      dockerCmd += ` ${image}`;
    }
    
    // Run container
    try {
      const containerId = execSync(dockerCmd, { stdio: 'pipe' }).toString().trim();
      console.log(`    ✅ Started container: ${containerName}`);
      
      this.containers.push({
        name: containerName,
        id: containerId,
        service: name
      });
      
      // Wait for health check if defined
      if (config.health_check) {
        await this.waitForHealth(containerName, config.health_check);
      }
      
      // Store endpoint information
      if (Object.keys(portMappings).length > 0) {
        this.endpoints[name] = {
          host: 'localhost',
          ports: portMappings,
          internal_host: containerName
        };
      }
      
    } catch (error) {
      console.error(`    ❌ Failed to start ${name}:`, error.message);
      throw error;
    }
  }

  async waitForHealth(containerName, healthCheck) {
    console.log(`    ⏳ Waiting for ${containerName} to be healthy...`);
    
    const maxRetries = healthCheck.retries || 30;
    const interval = this.parseInterval(healthCheck.interval || '10s');
    
    for (let i = 0; i < maxRetries; i++) {
      try {
        // Execute health check command
        const checkCmd = `docker exec ${containerName} ${healthCheck.test.join(' ')}`;
        execSync(checkCmd, { stdio: 'pipe' });
        console.log(`    ✅ ${containerName} is healthy`);
        return;
      } catch (error) {
        if (i === maxRetries - 1) {
          throw new Error(`Health check failed for ${containerName}`);
        }
        await this.sleep(interval);
      }
    }
  }

  async runInitialization() {
    if (!this.environment.initialization) {
      return;
    }
    
    console.log('🔧 Running initialization scripts...');
    
    for (const init of this.environment.initialization) {
      console.log(`  Running: ${init.name}`);
      
      try {
        // Replace environment variables in script
        let script = init.script;
        for (const [service, endpoint] of Object.entries(this.endpoints)) {
          script = script.replace(new RegExp(`\\$\\{${service}_HOST\\}`, 'g'), endpoint.host);
          for (const [internal, external] of Object.entries(endpoint.ports)) {
            script = script.replace(new RegExp(`\\$\\{${service}_PORT_${internal}\\}`, 'g'), external);
          }
        }
        
        execSync(script, { stdio: 'inherit' });
        console.log(`  ✅ Completed: ${init.name}`);
      } catch (error) {
        console.error(`  ❌ Failed: ${init.name}`);
        throw error;
      }
    }
  }

  saveEndpoints() {
    const endpointsFile = 'environment-endpoints.json';
    fs.writeFileSync(endpointsFile, JSON.stringify(this.endpoints, null, 2));
    console.log(`📝 Saved endpoints to ${endpointsFile}`);
  }

  saveMetadata() {
    const metadata = {
      environment: this.environmentName,
      namespace: this.namespace,
      network: this.networkName,
      containers: this.containers,
      cleanupToken: this.cleanupToken,
      createdAt: new Date().toISOString(),
      resources: this.environment.resources
    };
    
    fs.writeFileSync(`environment-${this.namespace}.json`, JSON.stringify(metadata, null, 2));
  }

  async cleanup() {
    console.log('🧹 Cleaning up failed provisioning...');
    
    // Stop and remove containers
    for (const container of this.containers) {
      try {
        execSync(`docker stop ${container.name}`, { stdio: 'pipe' });
        execSync(`docker rm ${container.name}`, { stdio: 'pipe' });
      } catch (error) {
        // Ignore errors during cleanup
      }
    }
    
    // Remove network
    if (this.networkName) {
      try {
        execSync(`docker network rm ${this.networkName}`, { stdio: 'pipe' });
      } catch (error) {
        // Ignore errors during cleanup
      }
    }
  }

  getRandomPort() {
    return Math.floor(Math.random() * 10000) + 20000;
  }

  parseInterval(interval) {
    const match = interval.match(/(\d+)([sm])/);
    if (!match) return 10000;
    
    const value = parseInt(match[1]);
    const unit = match[2];
    
    return unit === 's' ? value * 1000 : value * 60 * 1000;
  }

  sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }
}

// Parse command line arguments
const args = process.argv.slice(2);
const options = {
  config: '',
  environment: '',
  namespace: '',
  cleanupToken: ''
};

for (let i = 0; i < args.length; i += 2) {
  const key = args[i].replace('--', '');
  options[key] = args[i + 1];
}

// Run provisioner
const provisioner = new EnvironmentProvisioner(options);
provisioner.provision().catch(error => {
  console.error('Provisioning failed:', error);
  process.exit(1);
});