#!/usr/bin/env python3
"""
Kiro Next - A Claude Code command to process pending tasks in .kiro/specs
Usage: python kiro_next.py [--dry-run] [--spec SPEC_NAME]
"""

import os
import re
import sys
import argparse
from pathlib import Path
from typing import List, Tuple, Dict, Optional
from datetime import datetime

class KiroTaskProcessor:
    def __init__(self, specs_dir: str = ".kiro/specs", dry_run: bool = False):
        self.specs_dir = Path(specs_dir)
        self.dry_run = dry_run
        self.tasks_processed = 0
        self.tasks_found = 0
        
    def find_pending_tasks(self, spec_name: Optional[str] = None) -> Dict[str, List[Tuple[str, str, int]]]:
        """Find all pending tasks across specs or in a specific spec."""
        pending_tasks = {}
        
        # Get list of specs to process
        if spec_name:
            spec_dirs = [self.specs_dir / spec_name] if (self.specs_dir / spec_name).exists() else []
        else:
            spec_dirs = [d for d in self.specs_dir.iterdir() if d.is_dir()]
        
        for spec_dir in spec_dirs:
            tasks_file = spec_dir / "tasks.md"
            if not tasks_file.exists():
                continue
                
            spec_tasks = []
            with open(tasks_file, 'r') as f:
                lines = f.readlines()
                
            for i, line in enumerate(lines):
                # Match pending tasks: - [ ] followed by task number
                match = re.match(r'^(\s*)- \[ \] ([\d\.]+)\. (.+)$', line)
                if match:
                    indent = len(match.group(1))
                    task_num = match.group(2)
                    task_desc = match.group(3)
                    spec_tasks.append((task_num, task_desc.strip(), i))
                    self.tasks_found += 1
                    
            if spec_tasks:
                pending_tasks[spec_dir.name] = spec_tasks
                
        return pending_tasks
    
    def mark_task_complete(self, file_path: Path, line_num: int) -> bool:
        """Mark a specific task as complete in the file."""
        if self.dry_run:
            print(f"  [DRY RUN] Would mark task complete at line {line_num + 1}")
            return True
            
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
                
            # Replace [ ] with [x]
            lines[line_num] = lines[line_num].replace('- [ ]', '- [x]', 1)
            
            with open(file_path, 'w') as f:
                f.writelines(lines)
                
            self.tasks_processed += 1
            return True
        except Exception as e:
            print(f"  ERROR: Failed to update task: {e}")
            return False
    
    def process_tasks(self, spec_name: Optional[str] = None, task_numbers: Optional[List[str]] = None):
        """Process pending tasks, optionally filtering by spec and task numbers."""
        print(f"\n🎯 Kiro Next - Processing pending tasks")
        print(f"{'='*50}\n")
        
        pending_tasks = self.find_pending_tasks(spec_name)
        
        if not pending_tasks:
            print("✅ No pending tasks found!")
            return
            
        print(f"📋 Found {self.tasks_found} pending tasks across {len(pending_tasks)} specs\n")
        
        for spec, tasks in pending_tasks.items():
            print(f"\n📁 Spec: {spec}")
            print(f"   Tasks file: {self.specs_dir / spec / 'tasks.md'}")
            
            tasks_file = self.specs_dir / spec / "tasks.md"
            
            for task_num, task_desc, line_num in tasks:
                # Filter by task numbers if specified
                if task_numbers and not any(task_num.startswith(tn) for tn in task_numbers):
                    continue
                    
                print(f"\n   ⏳ Task {task_num}: {task_desc}")
                
                # Here you would implement the actual task processing logic
                # For now, we'll simulate task completion
                print(f"   🔧 Processing task {task_num}...")
                
                # Mark task as complete
                if self.mark_task_complete(tasks_file, line_num):
                    print(f"   ✅ Task {task_num} marked as complete")
                else:
                    print(f"   ❌ Failed to mark task {task_num} as complete")
        
        print(f"\n{'='*50}")
        print(f"📊 Summary: Processed {self.tasks_processed} of {self.tasks_found} tasks")
        if self.dry_run:
            print("   (DRY RUN - no files were actually modified)")

def main():
    parser = argparse.ArgumentParser(description='Process pending Kiro tasks')
    parser.add_argument('--dry-run', action='store_true', help='Show what would be done without making changes')
    parser.add_argument('--spec', type=str, help='Process tasks only in specific spec')
    parser.add_argument('--tasks', type=str, help='Comma-separated list of task numbers to process (e.g., "13,15")')
    parser.add_argument('--list', action='store_true', help='Only list pending tasks without processing')
    
    args = parser.parse_args()
    
    # Parse task numbers if provided
    task_numbers = None
    if args.tasks:
        task_numbers = [t.strip() for t in args.tasks.split(',')]
    
    processor = KiroTaskProcessor(dry_run=args.dry_run)
    
    if args.list:
        # Just list pending tasks
        pending_tasks = processor.find_pending_tasks(args.spec)
        for spec, tasks in pending_tasks.items():
            print(f"\n📁 {spec}:")
            for task_num, task_desc, _ in tasks:
                print(f"   - [ ] {task_num}. {task_desc}")
    else:
        # Process tasks
        processor.process_tasks(args.spec, task_numbers)

if __name__ == "__main__":
    main()