#!/usr/bin/env python3
"""
Download SonarCloud issues for the project and save them in multiple formats
"""

import json
import csv
import os
import requests
from datetime import datetime
from pathlib import Path
import yaml

class SonarCloudIssueDownloader:
    def __init__(self, config_file="sonarqube_config.yaml"):
        """Initialize with configuration"""
        # Load configuration
        with open(config_file, 'r') as f:
            self.config = yaml.safe_load(f)
        
        self.project_key = self.config['sonarqube']['project_key']
        self.organization = self.config['sonarqube']['organization']
        self.branch = self.config['sonarqube'].get('branch', 'main')
        self.base_url = self.config['sonarqube']['url']
        self.token = os.environ.get('SONAR_TOKEN')
        
        if not self.token:
            raise ValueError("SONAR_TOKEN environment variable not set")
        
        self.headers = {
            "Authorization": f"Bearer {self.token}"
        }
        
    def download_issues(self):
        """Download all issues from SonarCloud"""
        all_issues = []
        page = 1
        page_size = 500  # Maximum allowed
        
        print(f"Downloading issues for project: {self.project_key}")
        
        while True:
            params = {
                'componentKeys': self.project_key,
                'organization': self.organization,
                'branch': self.branch,
                'resolved': 'false',
                'ps': page_size,
                'p': page
            }
            
            try:
                response = requests.get(
                    f"{self.base_url}/api/issues/search",
                    headers=self.headers,
                    params=params
                )
                response.raise_for_status()
                data = response.json()
                
                issues = data.get('issues', [])
                all_issues.extend(issues)
                
                print(f"Downloaded page {page}: {len(issues)} issues")
                
                # Check if there are more pages
                total = data.get('total', 0)
                if len(all_issues) >= total or len(issues) < page_size:
                    break
                    
                page += 1
                
            except requests.exceptions.HTTPError as e:
                if e.response.status_code == 401:
                    print("Error: Invalid or expired SONAR_TOKEN")
                    print("Please generate a new token at: https://sonarcloud.io/account/security")
                else:
                    print(f"HTTP Error: {e}")
                return []
            except Exception as e:
                print(f"Error downloading issues: {e}")
                return []
        
        print(f"Total issues downloaded: {len(all_issues)}")
        return all_issues
    
    def download_hotspots(self):
        """Download security hotspots from SonarCloud"""
        all_hotspots = []
        page = 1
        page_size = 500
        
        print(f"Downloading security hotspots for project: {self.project_key}")
        
        while True:
            params = {
                'projectKey': self.project_key,
                'organization': self.organization,
                'branch': self.branch,
                'status': 'TO_REVIEW',
                'ps': page_size,
                'p': page
            }
            
            try:
                response = requests.get(
                    f"{self.base_url}/api/hotspots/search",
                    headers=self.headers,
                    params=params
                )
                response.raise_for_status()
                data = response.json()
                
                hotspots = data.get('hotspots', [])
                all_hotspots.extend(hotspots)
                
                print(f"Downloaded page {page}: {len(hotspots)} hotspots")
                
                # Check if there are more pages
                if 'paging' in data:
                    total = data['paging'].get('total', 0)
                    if len(all_hotspots) >= total or len(hotspots) < page_size:
                        break
                else:
                    break
                    
                page += 1
                
            except Exception as e:
                print(f"Error downloading hotspots: {e}")
                break
        
        print(f"Total hotspots downloaded: {len(all_hotspots)}")
        return all_hotspots
    
    def download_measures(self):
        """Download project measures/metrics"""
        metrics = [
            'bugs', 'vulnerabilities', 'security_hotspots', 'code_smells',
            'coverage', 'duplicated_lines_density', 'ncloc', 'sqale_index',
            'reliability_rating', 'security_rating', 'sqale_rating',
            'alert_status', 'quality_gate_details'
        ]
        
        params = {
            'component': self.project_key,
            'organization': self.organization,
            'branch': self.branch,
            'metricKeys': ','.join(metrics)
        }
        
        try:
            response = requests.get(
                f"{self.base_url}/api/measures/component",
                headers=self.headers,
                params=params
            )
            response.raise_for_status()
            return response.json()
        except Exception as e:
            print(f"Error downloading measures: {e}")
            return {}
    
    def save_to_json(self, data, filename):
        """Save data to JSON file"""
        output_dir = Path("sonar-reports")
        output_dir.mkdir(exist_ok=True)
        
        filepath = output_dir / filename
        with open(filepath, 'w') as f:
            json.dump(data, f, indent=2)
        
        print(f"Saved to: {filepath}")
        return filepath
    
    def save_issues_to_csv(self, issues, filename):
        """Save issues to CSV file with enhanced information"""
        if not issues:
            print("No issues to save")
            return
        
        output_dir = Path("sonar-reports")
        output_dir.mkdir(exist_ok=True)
        
        filepath = output_dir / filename
        
        # Define CSV columns including file path
        fieldnames = [
            'key', 'rule', 'severity', 'type', 'component', 'file_path', 'line',
            'startLine', 'endLine', 'startOffset', 'endOffset',
            'message', 'effort', 'debt', 'tags', 'creationDate', 'updateDate'
        ]
        
        with open(filepath, 'w', newline='', encoding='utf-8') as f:
            writer = csv.DictWriter(f, fieldnames=fieldnames)
            writer.writeheader()
            
            for issue in issues:
                # Extract file path from component
                component = issue.get('component', '')
                file_path = component.replace(f"{self.project_key}:", '') if component else ''
                
                # Get text range information if available
                text_range = issue.get('textRange', {})
                
                row = {
                    'key': issue.get('key', ''),
                    'rule': issue.get('rule', ''),
                    'severity': issue.get('severity', ''),
                    'type': issue.get('type', ''),
                    'component': component,
                    'file_path': file_path,
                    'line': issue.get('line', ''),
                    'startLine': text_range.get('startLine', issue.get('line', '')),
                    'endLine': text_range.get('endLine', issue.get('line', '')),
                    'startOffset': text_range.get('startOffset', ''),
                    'endOffset': text_range.get('endOffset', ''),
                    'message': issue.get('message', ''),
                    'effort': issue.get('effort', ''),
                    'debt': issue.get('debt', ''),
                    'tags': ','.join(issue.get('tags', [])),
                    'creationDate': issue.get('creationDate', ''),
                    'updateDate': issue.get('updateDate', '')
                }
                writer.writerow(row)
        
        print(f"Saved {len(issues)} issues to: {filepath}")
        return filepath
    
    def generate_summary_report(self, issues, hotspots, measures):
        """Generate a summary report"""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        
        # Count issues by severity
        severity_counts = {}
        type_counts = {}
        rule_counts = {}
        
        for issue in issues:
            severity = issue.get('severity', 'UNKNOWN')
            issue_type = issue.get('type', 'UNKNOWN')
            rule = issue.get('rule', 'UNKNOWN')
            
            severity_counts[severity] = severity_counts.get(severity, 0) + 1
            type_counts[issue_type] = type_counts.get(issue_type, 0) + 1
            rule_counts[rule] = rule_counts.get(rule, 0) + 1
        
        # Sort rules by count
        top_rules = sorted(rule_counts.items(), key=lambda x: x[1], reverse=True)[:10]
        
        # Extract measures
        measure_values = {}
        if 'component' in measures and 'measures' in measures['component']:
            for measure in measures['component']['measures']:
                measure_values[measure['metric']] = measure.get('value', 'N/A')
        
        report = f"""# SonarCloud Analysis Summary Report
Generated: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}

## Project Information
- **Project Key**: {self.project_key}
- **Organization**: {self.organization}
- **Branch**: {self.branch}

## Metrics Overview
- **Bugs**: {measure_values.get('bugs', 'N/A')}
- **Vulnerabilities**: {measure_values.get('vulnerabilities', 'N/A')}
- **Code Smells**: {measure_values.get('code_smells', 'N/A')}
- **Security Hotspots**: {len(hotspots)}
- **Coverage**: {measure_values.get('coverage', 'N/A')}%
- **Duplications**: {measure_values.get('duplicated_lines_density', 'N/A')}%
- **Lines of Code**: {measure_values.get('ncloc', 'N/A')}

## Issues Summary
**Total Issues**: {len(issues)}

### By Severity
"""
        for severity in ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO']:
            count = severity_counts.get(severity, 0)
            if count > 0:
                report += f"- **{severity}**: {count}\n"
        
        report += "\n### By Type\n"
        for issue_type, count in sorted(type_counts.items(), key=lambda x: x[1], reverse=True):
            report += f"- **{issue_type}**: {count}\n"
        
        report += "\n### Top 10 Rules\n"
        for rule, count in top_rules:
            report += f"- `{rule}`: {count} occurrences\n"
        
        report += f"\n## Security Hotspots\n**Total**: {len(hotspots)}\n"
        
        # Save report
        report_path = self.save_to_json({
            'timestamp': timestamp,
            'summary': {
                'total_issues': len(issues),
                'total_hotspots': len(hotspots),
                'severity_counts': severity_counts,
                'type_counts': type_counts,
                'top_rules': dict(top_rules),
                'measures': measure_values
            },
            'issues': issues,
            'hotspots': hotspots,
            'measures': measures
        }, f"sonar_full_report_{timestamp}.json")
        
        # Save markdown report
        output_dir = Path("sonar-reports")
        md_path = output_dir / f"sonar_summary_{timestamp}.md"
        with open(md_path, 'w') as f:
            f.write(report)
        
        print(f"Summary report saved to: {md_path}")
        
        return report
    
    def generate_issues_by_file_report(self, issues):
        """Generate a report grouped by file with line numbers"""
        if not issues:
            return {}
        
        # Group issues by file
        issues_by_file = {}
        for issue in issues:
            component = issue.get('component', '')
            file_path = component.replace(f"{self.project_key}:", '') if component else 'unknown'
            
            if file_path not in issues_by_file:
                issues_by_file[file_path] = []
            
            issues_by_file[file_path].append({
                'line': issue.get('line', 0),
                'severity': issue.get('severity', 'UNKNOWN'),
                'type': issue.get('type', 'UNKNOWN'),
                'rule': issue.get('rule', 'UNKNOWN'),
                'message': issue.get('message', ''),
                'textRange': issue.get('textRange', {})
            })
        
        # Sort issues within each file by line number
        for file_path in issues_by_file:
            issues_by_file[file_path].sort(key=lambda x: x['line'] or 0)
        
        # Generate markdown report
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        report = f"""# SonarCloud Issues by File Report
Generated: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}

Total Files with Issues: {len(issues_by_file)}
Total Issues: {len(issues)}

"""
        
        for file_path in sorted(issues_by_file.keys()):
            file_issues = issues_by_file[file_path]
            report += f"\n## {file_path}\n"
            report += f"**Issues**: {len(file_issues)}\n\n"
            
            for issue in file_issues:
                line_info = f"Line {issue['line']}" if issue['line'] else "No line info"
                report += f"- **{line_info}** [{issue['severity']}] `{issue['rule']}`: {issue['message']}\n"
        
        # Save report
        output_dir = Path("sonar-reports")
        report_path = output_dir / f"issues_by_file_{timestamp}.md"
        with open(report_path, 'w') as f:
            f.write(report)
        
        print(f"Issues by file report saved to: {report_path}")
        
        # Also save as JSON for programmatic access
        json_path = output_dir / f"issues_by_file_{timestamp}.json"
        with open(json_path, 'w') as f:
            json.dump(issues_by_file, f, indent=2)
        
        return issues_by_file

def main():
    """Main function"""
    print("SonarCloud Issue Downloader")
    print("=" * 50)
    
    downloader = SonarCloudIssueDownloader()
    
    # Download issues
    issues = downloader.download_issues()
    
    # Download hotspots
    hotspots = downloader.download_hotspots()
    
    # Download measures
    measures = downloader.download_measures()
    
    if issues:
        # Save to different formats
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        
        # Save JSON
        downloader.save_to_json({
            'issues': issues,
            'hotspots': hotspots,
            'measures': measures
        }, f"sonar_issues_{timestamp}.json")
        
        # Save CSV
        downloader.save_issues_to_csv(issues, f"sonar_issues_{timestamp}.csv")
        
        # Generate summary report
        downloader.generate_summary_report(issues, hotspots, measures)
        
        # Generate issues by file report
        downloader.generate_issues_by_file_report(issues)
    else:
        print("\nNo issues could be downloaded. Please check:")
        print("1. SONAR_TOKEN is valid")
        print("2. Project exists in SonarCloud")
        print("3. You have appropriate permissions")

if __name__ == "__main__":
    main()