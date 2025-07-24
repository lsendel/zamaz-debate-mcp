// MCP Test Dashboard JavaScript

class TestDashboard {
    constructor() {
        this.charts = {};
        this.data = null;
        this.filters = {
            timeRange: '7d',
            service: 'all',
            suite: 'all'
        };
        
        this.initializeEventListeners();
        this.loadData();
    }

    initializeEventListeners() {
        document.getElementById('timeRange').addEventListener('change', (e) => {
            this.filters.timeRange = e.target.value;
            this.updateDashboard();
        });

        document.getElementById('serviceFilter').addEventListener('change', (e) => {
            this.filters.service = e.target.value;
            this.updateDashboard();
        });

        document.getElementById('suiteFilter').addEventListener('change', (e) => {
            this.filters.suite = e.target.value;
            this.updateDashboard();
        });
    }

    async loadData() {
        try {
            // In a real implementation, this would fetch from an API
            // For now, we'll use mock data
            this.data = this.generateMockData();
            this.updateDashboard();
        } catch (error) {
            console.error('Failed to load test data:', error);
        }
    }

    generateMockData() {
        const services = ['mcp-gateway', 'mcp-organization', 'mcp-controller', 'debate-ui'];
        const suites = ['unit', 'integration', 'e2e', 'performance'];
        const now = dayjs();
        
        // Generate historical data
        const historicalData = [];
        for (let i = 90; i >= 0; i--) {
            const date = now.subtract(i, 'day');
            
            services.forEach(service => {
                suites.forEach(suite => {
                    const baseTests = suite === 'unit' ? 500 : suite === 'integration' ? 200 : 50;
                    const tests = baseTests + Math.floor(Math.random() * 50);
                    const passed = Math.floor(tests * (0.85 + Math.random() * 0.13));
                    const failed = tests - passed;
                    
                    historicalData.push({
                        date: date.format('YYYY-MM-DD'),
                        service,
                        suite,
                        total: tests,
                        passed,
                        failed,
                        duration: (suite === 'unit' ? 60 : suite === 'integration' ? 300 : 600) + Math.random() * 60,
                        coverage: 75 + Math.random() * 20
                    });
                });
            });
        }

        // Generate flaky tests
        const flakyTests = [
            { name: 'UserServiceTest.testConcurrentUpdate', service: 'mcp-organization', failureRate: 15 },
            { name: 'DebateControllerIT.testWebSocketConnection', service: 'mcp-controller', failureRate: 22 },
            { name: 'AuthenticationTest.testTokenExpiry', service: 'mcp-gateway', failureRate: 8 },
            { name: 'DebateListE2E.testInfiniteScroll', service: 'debate-ui', failureRate: 12 },
            { name: 'OrganizationCacheTest.testCacheEviction', service: 'mcp-organization', failureRate: 18 }
        ];

        return {
            historical: historicalData,
            flaky: flakyTests
        };
    }

    updateDashboard() {
        const filteredData = this.filterData();
        
        this.updateMetrics(filteredData);
        this.updateCoverageChart(filteredData);
        this.updateExecutionChart(filteredData);
        this.updateFlakyTestsList();
    }

    filterData() {
        let filtered = [...this.data.historical];
        
        // Apply time range filter
        const now = dayjs();
        const timeRanges = {
            '24h': 1,
            '7d': 7,
            '30d': 30,
            '90d': 90
        };
        const daysToInclude = timeRanges[this.filters.timeRange];
        const cutoffDate = now.subtract(daysToInclude, 'day').format('YYYY-MM-DD');
        
        filtered = filtered.filter(d => d.date >= cutoffDate);
        
        // Apply service filter
        if (this.filters.service !== 'all') {
            filtered = filtered.filter(d => d.service === this.filters.service);
        }
        
        // Apply suite filter
        if (this.filters.suite !== 'all') {
            filtered = filtered.filter(d => d.suite === this.filters.suite);
        }
        
        return filtered;
    }

    updateMetrics(data) {
        // Calculate current metrics
        const latestDate = dayjs().format('YYYY-MM-DD');
        const todayData = data.filter(d => d.date === latestDate);
        const yesterdayData = data.filter(d => d.date === dayjs().subtract(1, 'day').format('YYYY-MM-DD'));
        
        // Total tests
        const totalToday = todayData.reduce((sum, d) => sum + d.total, 0);
        const totalYesterday = yesterdayData.reduce((sum, d) => sum + d.total, 0);
        const totalChange = totalYesterday ? ((totalToday - totalYesterday) / totalYesterday * 100).toFixed(1) : 0;
        
        document.getElementById('totalTests').textContent = totalToday.toLocaleString();
        this.updateTrend('totalTestsTrend', totalChange);
        
        // Pass rate
        const passedToday = todayData.reduce((sum, d) => sum + d.passed, 0);
        const passRateToday = totalToday ? (passedToday / totalToday * 100).toFixed(1) : 0;
        const passedYesterday = yesterdayData.reduce((sum, d) => sum + d.passed, 0);
        const passRateYesterday = totalYesterday ? (passedYesterday / totalYesterday * 100).toFixed(1) : 0;
        const passRateChange = passRateYesterday ? (passRateToday - passRateYesterday).toFixed(1) : 0;
        
        document.getElementById('passRate').textContent = `${passRateToday}%`;
        this.updateTrend('passRateTrend', passRateChange, true);
        
        // Average duration
        const avgDurationToday = todayData.length ? 
            (todayData.reduce((sum, d) => sum + d.duration, 0) / todayData.length).toFixed(0) : 0;
        const avgDurationYesterday = yesterdayData.length ?
            (yesterdayData.reduce((sum, d) => sum + d.duration, 0) / yesterdayData.length).toFixed(0) : 0;
        const durationChange = avgDurationYesterday ? 
            ((avgDurationToday - avgDurationYesterday) / avgDurationYesterday * 100).toFixed(1) : 0;
        
        document.getElementById('avgDuration').textContent = `${avgDurationToday}s`;
        this.updateTrend('durationTrend', -durationChange); // Negative because lower is better
        
        // Flaky tests
        document.getElementById('flakyTests').textContent = this.data.flaky.length;
        document.getElementById('flakyTrend').textContent = '+2 from last week';
        document.getElementById('flakyTrend').className = 'metric-trend trend-down';
    }

    updateTrend(elementId, change, higherIsBetter = true) {
        const element = document.getElementById(elementId);
        const isPositive = parseFloat(change) > 0;
        const isGood = higherIsBetter ? isPositive : !isPositive;
        
        element.textContent = `${isPositive ? '+' : ''}${change}%`;
        element.className = `metric-trend ${isGood ? 'trend-up' : 'trend-down'}`;
    }

    updateCoverageChart(data) {
        // Group by date and calculate average coverage
        const coverageByDate = {};
        data.forEach(d => {
            if (!coverageByDate[d.date]) {
                coverageByDate[d.date] = [];
            }
            coverageByDate[d.date].push(d.coverage);
        });
        
        const dates = Object.keys(coverageByDate).sort();
        const coverageData = dates.map(date => {
            const coverages = coverageByDate[date];
            return (coverages.reduce((sum, c) => sum + c, 0) / coverages.length).toFixed(1);
        });
        
        const ctx = document.getElementById('coverageChart').getContext('2d');
        
        if (this.charts.coverage) {
            this.charts.coverage.destroy();
        }
        
        this.charts.coverage = new Chart(ctx, {
            type: 'line',
            data: {
                labels: dates.map(d => dayjs(d).format('MMM DD')),
                datasets: [{
                    label: 'Test Coverage %',
                    data: coverageData,
                    borderColor: '#3498db',
                    backgroundColor: 'rgba(52, 152, 219, 0.1)',
                    tension: 0.3,
                    fill: true
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: false
                    }
                },
                scales: {
                    y: {
                        beginAtZero: false,
                        min: 60,
                        max: 100,
                        ticks: {
                            callback: function(value) {
                                return value + '%';
                            }
                        }
                    }
                }
            }
        });
    }

    updateExecutionChart(data) {
        // Group by service and calculate average duration
        const services = ['mcp-gateway', 'mcp-organization', 'mcp-controller', 'debate-ui'];
        const suites = ['unit', 'integration', 'e2e'];
        
        const datasets = suites.map((suite, index) => {
            const colors = ['#3498db', '#e74c3c', '#f39c12'];
            
            return {
                label: suite.charAt(0).toUpperCase() + suite.slice(1),
                data: services.map(service => {
                    const serviceData = data.filter(d => d.service === service && d.suite === suite);
                    if (serviceData.length === 0) return 0;
                    return (serviceData.reduce((sum, d) => sum + d.duration, 0) / serviceData.length).toFixed(0);
                }),
                backgroundColor: colors[index]
            };
        });
        
        const ctx = document.getElementById('executionChart').getContext('2d');
        
        if (this.charts.execution) {
            this.charts.execution.destroy();
        }
        
        this.charts.execution = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: services.map(s => s.replace('mcp-', '').toUpperCase()),
                datasets: datasets
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    x: {
                        stacked: true
                    },
                    y: {
                        stacked: true,
                        beginAtZero: true,
                        ticks: {
                            callback: function(value) {
                                return value + 's';
                            }
                        }
                    }
                }
            }
        });
    }

    updateFlakyTestsList() {
        const listElement = document.getElementById('flakyTestsList');
        
        if (this.data.flaky.length === 0) {
            listElement.innerHTML = '<div class="test-item">No flaky tests detected!</div>';
            return;
        }
        
        const html = this.data.flaky
            .filter(test => this.filters.service === 'all' || test.service === this.filters.service)
            .map(test => `
                <div class="test-item">
                    <div>
                        <div class="test-name">${test.name}</div>
                        <div style="font-size: 12px; color: #7f8c8d; margin-top: 4px;">
                            ${test.service} • ${test.failureRate}% failure rate
                        </div>
                    </div>
                    <div class="test-status status-flaky">FLAKY</div>
                </div>
            `).join('');
        
        listElement.innerHTML = html || '<div class="test-item">No flaky tests in selected filters</div>';
    }
}

// Initialize dashboard when page loads
document.addEventListener('DOMContentLoaded', () => {
    new TestDashboard();
});