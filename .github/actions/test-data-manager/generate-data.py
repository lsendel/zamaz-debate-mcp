#!/usr/bin/env python3
"""Test Data Generator for MCP Services"""

import json
import random
import argparse
from datetime import datetime, timedelta
from faker import Faker
import uuid
import os

class TestDataGenerator:
    def __init__(self, seed=42):
        self.fake = Faker()
        Faker.seed(seed)
        random.seed(seed)
        self.data_sizes = {
            'small': {'users': 10, 'orgs': 5, 'debates': 20},
            'medium': {'users': 100, 'orgs': 20, 'debates': 200},
            'large': {'users': 1000, 'orgs': 100, 'debates': 2000},
            'xlarge': {'users': 10000, 'orgs': 500, 'debates': 20000}
        }
    
    def generate_users(self, count):
        """Generate user data"""
        users = []
        for i in range(count):
            user = {
                'id': str(uuid.uuid4()),
                'username': self.fake.user_name() + str(i),
                'email': self.fake.email(),
                'full_name': self.fake.name(),
                'password_hash': self.fake.sha256(),
                'role': random.choice(['USER', 'MODERATOR', 'ADMIN']),
                'status': random.choice(['ACTIVE', 'INACTIVE', 'SUSPENDED']),
                'created_at': self.fake.date_time_between(
                    start_date='-2y', end_date='now'
                ).isoformat(),
                'last_login': self.fake.date_time_between(
                    start_date='-30d', end_date='now'
                ).isoformat(),
                'profile': {
                    'bio': self.fake.text(max_nb_chars=200),
                    'avatar_url': self.fake.image_url(),
                    'location': self.fake.city(),
                    'expertise': random.sample([
                        'Technology', 'Politics', 'Science', 'Philosophy',
                        'Economics', 'Environment', 'Education', 'Healthcare'
                    ], k=random.randint(1, 3))
                }
            }
            users.append(user)
        return users
    
    def generate_organizations(self, count):
        """Generate organization data"""
        orgs = []
        org_types = ['BUSINESS', 'NONPROFIT', 'EDUCATIONAL', 'GOVERNMENT', 'RESEARCH']
        
        for i in range(count):
            org = {
                'id': str(uuid.uuid4()),
                'name': self.fake.company() + f" {i}",
                'type': random.choice(org_types),
                'description': self.fake.catch_phrase(),
                'website': self.fake.url(),
                'email': self.fake.company_email(),
                'status': random.choice(['ACTIVE', 'PENDING', 'SUSPENDED']),
                'created_at': self.fake.date_time_between(
                    start_date='-3y', end_date='now'
                ).isoformat(),
                'settings': {
                    'allow_public_debates': random.choice([True, False]),
                    'moderation_level': random.choice(['LOW', 'MEDIUM', 'HIGH']),
                    'member_limit': random.choice([50, 100, 500, 1000, None])
                },
                'contact': {
                    'phone': self.fake.phone_number(),
                    'address': self.fake.address(),
                    'contact_person': self.fake.name()
                }
            }
            orgs.append(org)
        return orgs
    
    def generate_debates(self, count, users, orgs):
        """Generate debate data"""
        debates = []
        topics = [
            "Should AI be regulated?",
            "Is remote work the future?",
            "Universal basic income: necessary or harmful?",
            "Climate change solutions",
            "Privacy vs security in digital age",
            "Future of education",
            "Healthcare reform approaches",
            "Space exploration priorities"
        ]
        
        for i in range(count):
            creator = random.choice(users)
            org = random.choice(orgs) if random.random() > 0.3 else None
            
            debate = {
                'id': str(uuid.uuid4()),
                'title': f"{random.choice(topics)} - Debate #{i}",
                'description': self.fake.paragraph(nb_sentences=3),
                'creator_id': creator['id'],
                'organization_id': org['id'] if org else None,
                'status': random.choice(['DRAFT', 'ACTIVE', 'CLOSED', 'ARCHIVED']),
                'visibility': random.choice(['PUBLIC', 'PRIVATE', 'ORGANIZATION']),
                'created_at': self.fake.date_time_between(
                    start_date='-1y', end_date='now'
                ).isoformat(),
                'scheduled_start': self.fake.future_datetime(
                    end_date='+30d'
                ).isoformat() if random.random() > 0.5 else None,
                'settings': {
                    'max_participants': random.choice([2, 4, 8, 16, None]),
                    'time_limit_minutes': random.choice([30, 60, 90, 120, None]),
                    'voting_enabled': random.choice([True, False]),
                    'moderation_required': random.choice([True, False])
                },
                'tags': random.sample([
                    'technology', 'politics', 'science', 'ethics',
                    'economics', 'environment', 'society', 'future'
                ], k=random.randint(1, 4)),
                'metrics': {
                    'views': random.randint(0, 10000),
                    'participants': random.randint(0, 100),
                    'comments': random.randint(0, 500),
                    'votes': random.randint(0, 1000)
                }
            }
            debates.append(debate)
        return debates
    
    def generate_arguments(self, debates, users, count_per_debate=10):
        """Generate argument data for debates"""
        arguments = []
        
        for debate in debates[:len(debates)//2]:  # Only active debates
            participant_count = random.randint(2, min(8, len(users)))
            participants = random.sample(users, participant_count)
            
            for i in range(random.randint(5, count_per_debate)):
                participant = random.choice(participants)
                
                argument = {
                    'id': str(uuid.uuid4()),
                    'debate_id': debate['id'],
                    'user_id': participant['id'],
                    'position': random.choice(['FOR', 'AGAINST', 'NEUTRAL']),
                    'content': self.fake.paragraph(nb_sentences=random.randint(3, 8)),
                    'created_at': self.fake.date_time_between(
                        start_date=debate['created_at'], end_date='now'
                    ).isoformat(),
                    'updated_at': None,
                    'parent_id': random.choice([a['id'] for a in arguments if a['debate_id'] == debate['id']])
                        if arguments and random.random() > 0.6 else None,
                    'votes': {
                        'up': random.randint(0, 100),
                        'down': random.randint(0, 50)
                    },
                    'status': random.choice(['ACTIVE', 'EDITED', 'DELETED'])
                }
                arguments.append(argument)
        
        return arguments
    
    def generate_test_scenarios(self, data_type):
        """Generate specific test scenarios"""
        scenarios = []
        
        if data_type == 'unit':
            # Edge cases for unit testing
            scenarios.extend([
                {
                    'name': 'empty_user',
                    'data': {'id': '', 'username': '', 'email': ''}
                },
                {
                    'name': 'invalid_email',
                    'data': {'email': 'not-an-email'}
                },
                {
                    'name': 'sql_injection',
                    'data': {'username': "admin'; DROP TABLE users;--"}
                },
                {
                    'name': 'xss_attempt',
                    'data': {'bio': '<script>alert("XSS")</script>'}
                },
                {
                    'name': 'unicode_names',
                    'data': {'full_name': '测试用户 🎯'}
                }
            ])
        
        elif data_type == 'integration':
            # Relationship scenarios
            scenarios.extend([
                {
                    'name': 'circular_reference',
                    'description': 'User A follows B, B follows C, C follows A'
                },
                {
                    'name': 'orphaned_data',
                    'description': 'Debates without creators'
                },
                {
                    'name': 'max_relationships',
                    'description': 'User with maximum allowed connections'
                }
            ])
        
        elif data_type == 'performance':
            # Performance test scenarios
            scenarios.extend([
                {
                    'name': 'bulk_insert',
                    'description': '10000 records in single transaction'
                },
                {
                    'name': 'complex_query',
                    'description': 'Query with 10 joins and aggregations'
                },
                {
                    'name': 'concurrent_updates',
                    'description': '100 concurrent update operations'
                }
            ])
        
        return scenarios
    
    def save_data(self, data, output_dir):
        """Save generated data to files"""
        os.makedirs(output_dir, exist_ok=True)
        
        # Save each data type
        for key, value in data.items():
            filepath = os.path.join(output_dir, f'{key}.json')
            with open(filepath, 'w') as f:
                json.dump(value, f, indent=2)
        
        # Save SQL scripts
        self.generate_sql_scripts(data, output_dir)
        
        # Save statistics
        stats = {
            'generated_at': datetime.now().isoformat(),
            'counts': {k: len(v) if isinstance(v, list) else 1 for k, v in data.items()},
            'size_bytes': sum(
                os.path.getsize(os.path.join(output_dir, f))
                for f in os.listdir(output_dir)
            )
        }
        
        with open(os.path.join(output_dir, 'statistics.json'), 'w') as f:
            json.dump(stats, f, indent=2)
        
        # Save sample data
        sample = {k: v[:5] if isinstance(v, list) else v for k, v in data.items()}
        with open(os.path.join(output_dir, 'sample.json'), 'w') as f:
            json.dump(sample, f, indent=2)
    
    def generate_sql_scripts(self, data, output_dir):
        """Generate SQL insert scripts"""
        sql_file = os.path.join(output_dir, 'insert_data.sql')
        
        with open(sql_file, 'w') as f:
            # Users
            if 'users' in data:
                f.write("-- Insert Users\n")
                for user in data['users']:
                    f.write(f"""
INSERT INTO users (id, username, email, full_name, role, status, created_at)
VALUES ('{user['id']}', '{user['username']}', '{user['email']}', 
        '{user['full_name']}', '{user['role']}', '{user['status']}', 
        '{user['created_at']}');
""")
            
            # Organizations
            if 'organizations' in data:
                f.write("\n-- Insert Organizations\n")
                for org in data['organizations']:
                    f.write(f"""
INSERT INTO organizations (id, name, type, description, status, created_at)
VALUES ('{org['id']}', '{org['name']}', '{org['type']}', 
        '{org['description']}', '{org['status']}', '{org['created_at']}');
""")
            
            # Add more tables as needed

def main():
    parser = argparse.ArgumentParser(description='Generate test data')
    parser.add_argument('--type', required=True, 
                       choices=['unit', 'integration', 'e2e', 'performance'])
    parser.add_argument('--service', default='all')
    parser.add_argument('--size', default='medium',
                       choices=['small', 'medium', 'large', 'xlarge'])
    parser.add_argument('--seed', type=int, default=42)
    parser.add_argument('--output', default='test-data')
    
    args = parser.parse_args()
    
    generator = TestDataGenerator(seed=args.seed)
    
    # Generate data based on size
    sizes = generator.data_sizes[args.size]
    
    data = {}
    
    # Generate base data
    if args.service in ['all', 'mcp-organization']:
        data['users'] = generator.generate_users(sizes['users'])
        data['organizations'] = generator.generate_organizations(sizes['orgs'])
    
    if args.service in ['all', 'mcp-controller']:
        if 'users' not in data:
            data['users'] = generator.generate_users(sizes['users'])
        if 'organizations' not in data:
            data['organizations'] = generator.generate_organizations(sizes['orgs'])
        
        data['debates'] = generator.generate_debates(
            sizes['debates'], data['users'], data['organizations']
        )
        data['arguments'] = generator.generate_arguments(
            data['debates'], data['users']
        )
    
    # Add test scenarios
    data['scenarios'] = generator.generate_test_scenarios(args.type)
    
    # Save data
    generator.save_data(data, args.output)
    
    print(f"✅ Generated {args.size} test data for {args.service}")
    print(f"📁 Data saved to {args.output}/")

if __name__ == '__main__':
    main()