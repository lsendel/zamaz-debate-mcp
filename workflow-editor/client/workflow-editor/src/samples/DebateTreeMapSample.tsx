import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useQuery } from '@tanstack/react-query';
import { useWorkflowStore } from '../store/workflowStore';

interface DebateNode {
  id: string;
  parentId: string | null;
  title: string;
  content: string;
  author: string;
  timestamp: number;
  votes: number;
  status: 'active' | 'resolved' | 'archived';
  children?: DebateNode[];
  depth?: number;
}

interface TreeMapNode {
  name: string;
  value: number;
  children?: TreeMapNode[];
  data: DebateNode;
}

const DebateTreeMapSample: React.FC = () => {
  const [selectedNode, setSelectedNode] = useState<DebateNode | null>(null);
  const [viewMode, setViewMode] = useState<'tree' | 'treemap' | 'hierarchy'>('tree');
  const [expandedNodes, setExpandedNodes] = useState<Set<string>>(new Set());
  const [filterStatus, setFilterStatus] = useState<'all' | 'active' | 'resolved' | 'archived'>('all');
//   const { createWorkflow, addTelemetryData } = useWorkflowStore(); // SonarCloud: removed useless assignment

  // Fetch debate data from SASS debate system
  const { data: debateData, isLoading, refetch } = useQuery({
    queryKey: ['debates'],
    queryFn: async () => {
      try {
        // Try to fetch from the debate controller service first
        const debateResponse = await fetch('http://localhost:5013/api/debates/active');
        if (debateResponse.ok) {
          const debates = await debateResponse.json();
          // Convert SASS debate format to tree structure
          return convertSASSDebatesToTree(debates);
        }
      } catch (error) {
        console.log('Debate controller not available, trying organization service...');
      }
      
      try {
        // Fallback to organization service which manages debates
        const orgResponse = await fetch('http://localhost:5005/api/organizations/debates');
        if (orgResponse.ok) {
          const orgDebates = await orgResponse.json();
          return convertOrganizationDebatesToTree(orgDebates);
        }
      } catch (error) {
        console.log('Organization service not available, using enhanced mock data...');
      }
      
      // Return realistic SASS debate mock data
      return generateRealisticSASSDebateData();
    },
    staleTime: 5 * 60 * 1000, // Data is fresh for 5 minutes
    gcTime: 10 * 60 * 1000, // Cache for 10 minutes
    retry: 1, // Only retry once
    refetchOnWindowFocus: false // Don't refetch on window focus
  });

  // Convert SASS debate format to tree structure
  const convertSASSDebatesToTree = (debates: any[]): DebateNode[] => {
    return debates.map(debate => ({
      id: debate.debateId,
      parentId: null,
      title: debate.topic,
      content: debate.description,
      author: debate.moderator || 'System',
      timestamp: new Date(debate.createdAt).getTime(),
      votes: debate.participantCount || 0,
      status: debate.status === 'ACTIVE' ? 'active' : debate.status === 'CLOSED' ? 'resolved' : 'archived',
      children: debate.arguments?.map((arg: any) => ({
        id: arg.argumentId,
        parentId: debate.debateId,
        title: arg.position,
        content: arg.content,
        author: arg.participant,
        timestamp: new Date(arg.timestamp).getTime(),
        votes: arg.votes || 0,
        status: 'active',
        children: arg.counterArguments || []
      })) || []
    }));
  };

  // Convert organization debates to tree structure
  const convertOrganizationDebatesToTree = (orgDebates: any[]): DebateNode[] => {
    return orgDebates.map(debate => ({
      id: debate.id,
      parentId: null,
      title: debate.title,
      content: debate.summary,
      author: debate.organizationName,
      timestamp: new Date(debate.startDate).getTime(),
      votes: debate.totalParticipants || 0,
      status: debate.isActive ? 'active' : 'resolved',
      children: debate.positions?.map((pos: any) => ({
        id: pos.id,
        parentId: debate.id,
        title: pos.stance,
        content: pos.argument,
        author: pos.advocateName,
        timestamp: new Date(pos.createdAt).getTime(),
        votes: pos.supportCount || 0,
        status: 'active',
        children: []
      })) || []
    }));
  };

  // Generate realistic SASS debate data
  const generateRealisticSASSDebateData = (): DebateNode[] => {
    // Real debate topics from a SASS debate system
    const sassDebates = [
      {
        id: 'debate-001',
        parentId: null,
        title: 'Should our organization adopt a fully remote work policy?',
        content: 'Given the success of remote work during the pandemic and employee preferences, should we transition to a permanent remote-first model?',
        author: 'HR Leadership Team',
        timestamp: Date.now() - 7 * 24 * 60 * 60 * 1000,
        votes: 156,
        status: 'active' as const,
        children: [
          {
            id: 'arg-001-1',
            parentId: 'debate-001',
            title: 'Pro: Increased productivity and employee satisfaction',
            content: 'Data shows 23% productivity increase and 89% employee satisfaction with remote work. Cost savings of $2.3M annually on office space.',
            author: 'Sarah Chen, Operations',
            timestamp: Date.now() - 6 * 24 * 60 * 60 * 1000,
            votes: 87,
            status: 'active' as const,
            children: [
              {
                id: 'counter-001-1-1',
                parentId: 'arg-001-1',
                title: 'Counter: Collaboration challenges in remote settings',
                content: 'While productivity metrics look good, innovation metrics show 15% decline. Spontaneous collaboration has decreased significantly.',
                author: 'Mike Rodriguez, Engineering',
                timestamp: Date.now() - 5 * 24 * 60 * 60 * 1000,
                votes: 34,
                status: 'active' as const,
                children: []
              }
            ]
          },
          {
            id: 'arg-001-2',
            parentId: 'debate-001',
            title: 'Con: Loss of company culture and mentorship',
            content: 'New employees report 40% less engagement. Junior staff missing crucial mentorship opportunities. Team cohesion metrics down 28%.',
            author: 'Jennifer Park, People Ops',
            timestamp: Date.now() - 6 * 24 * 60 * 60 * 1000,
            votes: 69,
            status: 'active' as const,
            children: []
          }
        ]
      },
      {
        id: 'debate-002',
        parentId: null,
        title: 'Implementation of AI-driven customer service: Benefits vs risks?',
        content: 'Proposal to implement GPT-based customer service automation. Expected 60% reduction in response time but concerns about service quality.',
        author: 'Customer Success Team',
        timestamp: Date.now() - 14 * 24 * 60 * 60 * 1000,
        votes: 203,
        status: 'resolved' as const,
        children: [
          {
            id: 'arg-002-1',
            parentId: 'debate-002',
            title: 'Pro: Scalability and 24/7 availability',
            content: 'AI can handle 10,000+ concurrent conversations. Customers get instant responses. Frees human agents for complex issues.',
            author: 'David Kim, CTO',
            timestamp: Date.now() - 13 * 24 * 60 * 60 * 1000,
            votes: 112,
            status: 'active' as const,
            children: []
          },
          {
            id: 'arg-002-2',
            parentId: 'debate-002',
            title: 'Con: Risk of impersonal service and edge case failures',
            content: 'AI struggles with emotional nuance. 12% of test cases resulted in customer frustration. Brand reputation risk.',
            author: 'Lisa Thompson, Customer Success',
            timestamp: Date.now() - 13 * 24 * 60 * 60 * 1000,
            votes: 91,
            status: 'active' as const,
            children: []
          }
        ]
      },
      {
        id: 'debate-003',
        parentId: null,
        title: 'Sustainability initiative: Carbon neutral by 2025?',
        content: 'Ambitious goal to achieve carbon neutrality within 2 years. Requires $5M investment but aligns with corporate values.',
        author: 'ESG Committee',
        timestamp: Date.now() - 30 * 24 * 60 * 60 * 1000,
        votes: 298,
        status: 'active' as const,
        children: [
          {
            id: 'arg-003-1',
            parentId: 'debate-003',
            title: 'Pro: Competitive advantage and talent attraction',
            content: '73% of top candidates consider sustainability. Green initiatives attract premium customers willing to pay 15% more.',
            author: 'Rachel Green, Strategy',
            timestamp: Date.now() - 28 * 24 * 60 * 60 * 1000,
            votes: 167,
            status: 'active' as const,
            children: []
          },
          {
            id: 'arg-003-2',
            parentId: 'debate-003',
            title: 'Alternative: Phased approach over 5 years',
            content: 'Gradual implementation reduces financial risk. Can leverage emerging technologies. More realistic timeline.',
            author: 'Carlos Mendez, Finance',
            timestamp: Date.now() - 27 * 24 * 60 * 60 * 1000,
            votes: 131,
            status: 'active' as const,
            children: []
          }
        ]
      }
    ];

    return sassDebates;
  };

  // Generate mock debate data for demo
  const generateMockDebateData = (): DebateNode[] => {
    const topics = [
      'Should AI regulation be mandatory?',
      'Is remote work more productive?',
      'Should cryptocurrencies replace traditional banking?',
      'Is social media harmful to society?',
      'Should electric vehicles be subsidized?'
    ];

    const generateDebateTree = (parentId: string | null, depth: number): DebateNode[] => {
      if (depth > 3) return [];
      
      const count = parentId === null ? 5 : Math.floor(Math.random() * 3) + 1;
      const nodes: DebateNode[] = [];
      
      for (let i = 0; i < count; i++) {
        const id = `debate-${parentId || 'root'}-${i}`;
        const node: DebateNode = {
          id,
          parentId,
          title: parentId === null ? topics[i] : `Response to ${parentId}`,
          content: `This is the content for debate node ${id}. It contains arguments and supporting evidence.`,
          author: `User${Math.floor(Math.random() * 100)}`,
          timestamp: Date.now() - Math.floor(Math.random() * 86400000),
          votes: Math.floor(Math.random() * 100),
          status: ['active', 'resolved', 'archived'][Math.floor(Math.random() * 3)] as any,
          depth
        };
        
        nodes.push(node);
        
        // Recursively generate children
        if (Math.random() > 0.3) {
          const children = generateDebateTree(id, depth + 1);
          node.children = children;
        }
      }
      
      return nodes;
    };

    return generateDebateTree(null, 0);
  };

  // Convert flat debate data to hierarchical structure
  const buildDebateTree = (flatData: DebateNode[]): DebateNode[] => {
    const nodeMap = new Map<string, DebateNode>();
    const roots: DebateNode[] = [];

    // First pass: create all nodes
    flatData.forEach(node => {
      nodeMap.set(node.id, { ...node, children: [] });
    });

    // Second pass: build hierarchy
    flatData.forEach(node => {
      const currentNode = nodeMap.get(node.id)!;
      if (node.parentId && nodeMap.has(node.parentId)) {
        const parent = nodeMap.get(node.parentId)!;
        if (!parent.children) parent.children = [];
        parent.children.push(currentNode);
      } else {
        roots.push(currentNode);
      }
    });

    return roots;
  };

  // Convert to treemap format
//   const convertToTreeMap = (nodes: DebateNode[]): TreeMapNode => { // SonarCloud: removed useless assignment
    const convertNode = (node: DebateNode): TreeMapNode => ({
      name: node.title,
      value: node.votes + 1, // +1 to avoid zero values
      children: node.children?.map(convertNode),
      data: node
    });

    return {
      name: 'Debate Topics',
      value: 0,
      children: nodes.map(convertNode),
      data: null as any
    };
  };

  // Tree node component
  const TreeNode: React.FC<{ node: DebateNode; level: number }> = ({ node, level }) => {
    const isExpanded = expandedNodes.has(node.id);
    const hasChildren = node.children && node.children.length > 0;

    const toggleExpand = () => {
      const newExpanded = new Set(expandedNodes);
      if (isExpanded) {
        newExpanded.delete(node.id);
      } else {
        newExpanded.add(node.id);
      }
      setExpandedNodes(newExpanded);
    };

    return (
      <motion.div
        initial={{ opacity: 0, x: -20 }}
        animate={{ opacity: 1, x: 0 }}
        transition={{ delay: level * 0.05 }}
        className="tree-node"
        style={{ marginLeft: `${level * 30}px` }}
      >
        <div
          className={`node-content ${node.status}`}
          onClick={() => setSelectedNode(node)}
        >
          {hasChildren && (
            <button
              className="expand-button"
              onClick={(e) => {
                e.stopPropagation();
                toggleExpand();
              }}
            >
              {isExpanded ? '▼' : '▶'}
            </button>
          )}
          <div className="node-header">
            <span className="node-title">{node.title}</span>
            <span className="node-votes">👍 {node.votes}</span>
          </div>
          <div className="node-meta">
            <span className="author">@{node.author}</span>
            <span className="timestamp">
              {new Date(node.timestamp).toLocaleString()}
            </span>
          </div>
        </div>
        
        <AnimatePresence>
          {isExpanded && hasChildren && (
            <motion.div
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: 'auto', opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
            >
              {node.children!.map(child => (
                <TreeNode key={child.id} node={child} level={level + 1} />
              ))}
            </motion.div>
          )}
        </AnimatePresence>
      </motion.div>
    );
  };

  // Create workflow for debate moderation
  const createDebateWorkflow = () => {
    
    const workflow = {
      id: 'debate-moderation-workflow',
      name: 'Debate Moderation Workflow',
      description: 'Automated moderation and escalation for debate threads',
      status: 'published' as const,
      nodes: [
        {
          id: 'start-1',
          type: 'start',
          position: { x: 100, y: 150 },
          data: { 
            label: 'New Post',
            configuration: { trigger: 'event', eventType: 'debate.post' }
          }
        },
        {
          id: 'task-1',
          type: 'task',
          position: { x: 300, y: 150 },
          data: {
            label: 'Content Analysis',
            configuration: {
              taskType: 'AI_ANALYSIS',
              timeout: 10
            }
          }
        },
        {
          id: 'decision-1',
          type: 'decision',
          position: { x: 500, y: 150 },
          data: {
            label: 'Check Violations',
            configuration: {
              decisionType: 'simple'
            }
          }
        },
        {
          id: 'task-2',
          type: 'task',
          position: { x: 700, y: 100 },
          data: {
            label: 'Flag Post',
            configuration: {
              taskType: 'FLAG_CONTENT'
            }
          }
        },
        {
          id: 'task-3',
          type: 'task',
          position: { x: 700, y: 200 },
          data: {
            label: 'Approve Post',
            configuration: {
              taskType: 'APPROVE_CONTENT'
            }
          }
        },
        {
          id: 'end-1',
          type: 'end',
          position: { x: 900, y: 150 },
          data: {
            label: 'Complete'
          }
        }
      ],
      edges: [
        { id: 'e1', source: 'start-1', target: 'task-1', animated: true },
        { id: 'e2', source: 'task-1', target: 'decision-1', animated: true },
        { id: 'e3', source: 'decision-1', sourceHandle: 'true', target: 'task-2', animated: true },
        { id: 'e4', source: 'decision-1', sourceHandle: 'false', target: 'task-3', animated: true },
        { id: 'e5', source: 'task-2', target: 'end-1', animated: true },
        { id: 'e6', source: 'task-3', target: 'end-1', animated: true }
      ]
    };
    
    createWorkflow(workflow);
  };

  // Simulate real-time updates (disabled to prevent flashing)
  useEffect(() => {
    if (!debateData) return;

    // Disabled real-time simulation to prevent UI flashing
    // const interval = setInterval(() => {
    //   // Simulate vote changes
    //   if (Math.random() > 0.7) {
    //     addTelemetryData('debate.activity', {
    //       timestamp: Date.now(),
    //       value: Math.random() * 10,
    //       metadata: {
    //         type: 'vote',
    //         debateId: debateData[Math.floor(Math.random() * debateData.length)]?.id
    //       }
    //     });
    //   }
    // }, 2000);

    // return () => clearInterval(interval);
  }, [debateData]);

  const treeData = debateData ? buildDebateTree(
    Array.isArray(debateData) ? debateData : generateMockDebateData()
  ) : [];

  const filteredData = treeData.filter(node => 
    filterStatus === 'all' || node.status === filterStatus
  );

  return (
    <div className="debate-tree-sample">
      <div className="sample-header">
        <h2>Debate Tree Map Sample</h2>
        <p>Hierarchical visualization of debate threads with real-time updates</p>
      </div>

      <div className="sample-controls">
        <button
          className="refresh-button"
          onClick={() => refetch()}
        >
          🔄 Refresh
        </button>
        
        <button
          className="workflow-button"
          onClick={createDebateWorkflow}
        >
          📋 Create Moderation Workflow
        </button>
        
        <div className="view-selector">
          <button
            className={viewMode === 'tree' ? 'active' : ''}
            onClick={() => setViewMode('tree')}
          >
            🌳 Tree View
          </button>
          <button
            className={viewMode === 'treemap' ? 'active' : ''}
            onClick={() => setViewMode('treemap')}
            disabled
          >
            📊 TreeMap
          </button>
          <button
            className={viewMode === 'hierarchy' ? 'active' : ''}
            onClick={() => setViewMode('hierarchy')}
            disabled
          >
            📈 Hierarchy
          </button>
        </div>
        
        <select
          value={filterStatus}
          onChange={(e) => setFilterStatus(e.target.value as any)}
          className="status-filter"
        >
          <option value="all">All Status</option>
          <option value="active">Active</option>
          <option value="resolved">Resolved</option>
          <option value="archived">Archived</option>
        </select>
      </div>

      <div className="debate-content">
        {isLoading ? (
          <div className="loading">Loading debate data...</div>
        ) : (
          <div className="tree-container">
            {filteredData.map(node => (
              <TreeNode key={node.id} node={node} level={0} />
            ))}
          </div>
        )}
      </div>

      {selectedNode && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          className="node-details"
        >
          <button
            className="close-button"
            onClick={() => setSelectedNode(null)}
          >
            ✕
          </button>
          <h3>{selectedNode.title}</h3>
          <div className="detail-meta">
            <span className={`status-badge ${selectedNode.status}`}>
              {selectedNode.status}
            </span>
            <span>@{selectedNode.author}</span>
            <span>👍 {selectedNode.votes} votes</span>
          </div>
          <div className="detail-content">
            <p>{selectedNode.content}</p>
          </div>
          <div className="detail-stats">
            <div className="stat">
              <span className="stat-label">Posted</span>
              <span className="stat-value">
                {new Date(selectedNode.timestamp).toLocaleString()}
              </span>
            </div>
            <div className="stat">
              <span className="stat-label">Replies</span>
              <span className="stat-value">
                {selectedNode.children?.length || 0}
              </span>
            </div>
            <div className="stat">
              <span className="stat-label">Depth</span>
              <span className="stat-value">
                Level {selectedNode.depth || 0}
              </span>
            </div>
          </div>
        </motion.div>
      )}

      <style>{`
        .debate-tree-sample {
          padding: 20px;
          max-width: 1400px;
          margin: 0 auto;
        }

        .sample-header {
          text-align: center;
          margin-bottom: 30px;
        }

        .sample-header h2 {
          margin: 0 0 10px 0;
          color: #333;
        }

        .sample-controls {
          display: flex;
          justify-content: center;
          align-items: center;
          gap: 20px;
          margin-bottom: 30px;
          flex-wrap: wrap;
        }

        .refresh-button,
        .workflow-button {
          padding: 10px 20px;
          border: none;
          border-radius: 4px;
          cursor: pointer;
          font-size: 14px;
          transition: background 0.2s;
        }

        .refresh-button {
          background: #2196F3;
          color: white;
        }

        .workflow-button {
          background: #4CAF50;
          color: white;
        }

        .view-selector {
          display: flex;
          gap: 5px;
          background: #f0f0f0;
          padding: 5px;
          border-radius: 4px;
        }

        .view-selector button {
          padding: 8px 16px;
          border: none;
          background: transparent;
          cursor: pointer;
          border-radius: 4px;
          transition: all 0.2s;
        }

        .view-selector button.active {
          background: white;
          box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
        }

        .view-selector button:disabled {
          opacity: 0.5;
          cursor: not-allowed;
        }

        .status-filter {
          padding: 8px 12px;
          border: 1px solid #ddd;
          border-radius: 4px;
          font-size: 14px;
        }

        .debate-content {
          background: white;
          border-radius: 8px;
          padding: 20px;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
          min-height: 400px;
        }

        .tree-container {
          max-height: 600px;
          overflow-y: auto;
        }

        .tree-node {
          margin-bottom: 10px;
        }

        .node-content {
          background: #f9f9f9;
          border: 1px solid #e0e0e0;
          border-radius: 8px;
          padding: 12px;
          cursor: pointer;
          transition: all 0.2s;
        }

        .node-content:hover {
          border-color: #2196F3;
          box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
        }

        .node-content.active {
          border-left: 4px solid #4CAF50;
        }

        .node-content.resolved {
          border-left: 4px solid #2196F3;
          opacity: 0.8;
        }

        .node-content.archived {
          border-left: 4px solid #9E9E9E;
          opacity: 0.6;
        }

        .expand-button {
          float: left;
          margin-right: 10px;
          background: none;
          border: none;
          cursor: pointer;
          font-size: 12px;
          color: #666;
        }

        .node-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 5px;
        }

        .node-title {
          font-weight: 500;
          color: #333;
          flex: 1;
        }

        .node-votes {
          color: #2196F3;
          font-size: 14px;
        }

        .node-meta {
          display: flex;
          gap: 15px;
          font-size: 12px;
          color: #666;
        }

        .node-details {
          position: fixed;
          right: 0;
          top: 0;
          bottom: 0;
          width: 400px;
          background: white;
          box-shadow: -2px 0 8px rgba(0, 0, 0, 0.1);
          padding: 30px;
          overflow-y: auto;
          z-index: 1000;
        }

        .close-button {
          position: absolute;
          top: 15px;
          right: 15px;
          background: none;
          border: none;
          font-size: 20px;
          cursor: pointer;
          color: #999;
        }

        .node-details h3 {
          margin: 0 0 20px 0;
          color: #333;
        }

        .detail-meta {
          display: flex;
          gap: 15px;
          margin-bottom: 20px;
          font-size: 14px;
          color: #666;
        }

        .status-badge {
          padding: 2px 8px;
          border-radius: 4px;
          font-size: 12px;
          text-transform: uppercase;
        }

        .status-badge.active {
          background: #e8f5e9;
          color: #2e7d32;
        }

        .status-badge.resolved {
          background: #e3f2fd;
          color: #1976D2;
        }

        .status-badge.archived {
          background: #fafafa;
          color: #757575;
        }

        .detail-content {
          margin-bottom: 20px;
          padding: 15px;
          background: #f9f9f9;
          border-radius: 4px;
        }

        .detail-stats {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 15px;
        }

        .stat {
          padding: 10px;
          background: #f5f5f5;
          border-radius: 4px;
        }

        .stat-label {
          display: block;
          font-size: 12px;
          color: #666;
          margin-bottom: 5px;
        }

        .stat-value {
          display: block;
          font-size: 16px;
          font-weight: 500;
          color: #333;
        }

        .loading {
          text-align: center;
          padding: 50px;
          color: #666;
        }
      `}</style>
    </div>
  );
};

export default DebateTreeMapSample;