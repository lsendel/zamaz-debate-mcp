-- Add indexes to frequently queried fields in the debates table
CREATE INDEX IF NOT EXISTS idx_debates_organization_id ON debates(organization_id);
CREATE INDEX IF NOT EXISTS idx_debates_status ON debates(status);
CREATE INDEX IF NOT EXISTS idx_debates_created_at ON debates(created_at);
CREATE INDEX IF NOT EXISTS idx_debates_organization_id_status ON debates(organization_id, status);

-- Add indexes to frequently queried fields in the participants table
CREATE INDEX IF NOT EXISTS idx_participants_debate_id ON participants(debate_id);

-- Add indexes to frequently queried fields in the messages table
CREATE INDEX IF NOT EXISTS idx_messages_debate_id ON messages(debate_id);
CREATE INDEX IF NOT EXISTS idx_messages_participant_id ON messages(participant_id);
CREATE INDEX IF NOT EXISTS idx_messages_round ON messages(round);
CREATE INDEX IF NOT EXISTS idx_messages_created_at ON messages(created_at);
CREATE INDEX IF NOT EXISTS idx_messages_debate_id_round ON messages(debate_id, round);
CREATE INDEX IF NOT EXISTS idx_messages_debate_id_participant_id ON messages(debate_id, participant_id);

-- Add indexes to frequently queried fields in the summaries table
CREATE INDEX IF NOT EXISTS idx_summaries_debate_id ON summaries(debate_id);
CREATE INDEX IF NOT EXISTS idx_summaries_created_at ON summaries(created_at);
