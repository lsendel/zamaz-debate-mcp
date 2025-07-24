#!/usr/bin/env node

const fs = require('fs');
const path = require('path');

class EfficiencyAnalyzer {
  constructor(workspace, duration, workers) {
    this.workspace = workspace;
    this.totalDuration = parseInt(duration);
    this.workers = parseInt(workers);
  }

  analyze() {
    console.log('📊 Analyzing parallel execution efficiency...');
    
    // Load execution summary
    const summaryPath = path.join(this.workspace, 'execution-summary.json');
    const summary = JSON.parse(fs.readFileSync(summaryPath, 'utf8'));
    
    // Calculate metrics
    const workerDurations = summary.results.map(r => r.duration);
    const maxDuration = Math.max(...workerDurations);
    const totalWork = workerDurations.reduce((a, b) => a + b, 0);
    const avgDuration = totalWork / workerDurations.length;
    
    // Efficiency = (total work / workers) / actual duration
    const idealDuration = totalWork / this.workers;
    const efficiency = (idealDuration / maxDuration * 100).toFixed(2);
    
    // Load imbalance factor
    const imbalance = ((maxDuration - avgDuration) / avgDuration * 100).toFixed(2);
    
    // Worker utilization
    const utilization = workerDurations.map(d => (d / maxDuration * 100).toFixed(2));
    
    // Generate report
    const report = {
      efficiency: parseFloat(efficiency),
      metrics: {
        totalDuration: this.totalDuration,
        maxWorkerDuration: maxDuration,
        avgWorkerDuration: Math.round(avgDuration),
        totalWork,
        idealDuration: Math.round(idealDuration),
        imbalancePercent: parseFloat(imbalance),
        workerUtilization: utilization
      },
      analysis: this.generateAnalysis(efficiency, imbalance, utilization),
      recommendations: this.generateRecommendations(efficiency, imbalance)
    };
    
    // Save report
    const reportPath = path.join(this.workspace, 'efficiency-report.json');
    fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
    
    // Output for GitHub Actions
    console.log(`::set-output name=efficiency::${efficiency}`);
    
    // Print summary
    console.log(`\n✅ Efficiency Analysis Complete`);
    console.log(`  Parallel Efficiency: ${efficiency}%`);
    console.log(`  Load Imbalance: ${imbalance}%`);
    console.log(`  Speedup: ${(totalWork / maxDuration).toFixed(2)}x`);
  }

  generateAnalysis(efficiency, imbalance, utilization) {
    const analysis = [];
    
    if (efficiency > 90) {
      analysis.push('Excellent parallel efficiency - tests are well distributed');
    } else if (efficiency > 75) {
      analysis.push('Good parallel efficiency with room for improvement');
    } else if (efficiency > 50) {
      analysis.push('Moderate efficiency - significant optimization potential');
    } else {
      analysis.push('Poor efficiency - tests need rebalancing');
    }
    
    if (imbalance > 30) {
      analysis.push('High load imbalance detected between workers');
    }
    
    // Check for underutilized workers
    const underutilized = utilization.filter(u => parseFloat(u) < 50).length;
    if (underutilized > 0) {
      analysis.push(`${underutilized} workers were underutilized (<50%)`);
    }
    
    return analysis;
  }

  generateRecommendations(efficiency, imbalance) {
    const recommendations = [];
    
    if (efficiency < 80) {
      recommendations.push('Consider splitting large test files into smaller units');
      recommendations.push('Review test grouping algorithm for better distribution');
    }
    
    if (imbalance > 30) {
      recommendations.push('Some test groups are taking significantly longer than others');
      recommendations.push('Profile slow tests and optimize or split them');
    }
    
    if (this.workers > 8 && efficiency < 70) {
      recommendations.push('Too many workers may be causing overhead - try reducing worker count');
    }
    
    return recommendations;
  }

  generateResourceReport() {
    // Analyze resource usage if available
    const resourcePath = path.join(this.workspace, 'resource-usage.json');
    
    if (fs.existsSync(resourcePath)) {
      const resources = JSON.parse(fs.readFileSync(resourcePath, 'utf8'));
      
      return {
        cpuUtilization: resources.cpu,
        memoryUsage: resources.memory,
        ioWait: resources.io
      };
    }
    
    return null;
  }
}

// Parse arguments
const args = process.argv.slice(2);
let workspace = '';
let duration = '';
let workers = '';

for (let i = 0; i < args.length; i += 2) {
  const key = args[i].replace('--', '');
  const value = args[i + 1];
  
  switch (key) {
    case 'workspace': workspace = value; break;
    case 'duration': duration = value; break;
    case 'workers': workers = value; break;
  }
}

if (!workspace || !duration || !workers) {
  console.error('Error: --workspace, --duration, and --workers are required');
  process.exit(1);
}

// Run analyzer
const analyzer = new EfficiencyAnalyzer(workspace, duration, workers);
analyzer.analyze();