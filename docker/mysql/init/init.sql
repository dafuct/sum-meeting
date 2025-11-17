-- Initialize database schema for Zoom Transcriber
-- This file is executed when MySQL container starts for the first time

USE zoom_transcriber;

-- Create tables if they don't exist
CREATE TABLE IF NOT EXISTS meetings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    meeting_id VARCHAR(255) UNIQUE NOT NULL,
    title VARCHAR(500),
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    duration_minutes INT,
    status VARCHAR(50) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS transcripts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    meeting_id VARCHAR(255) NOT NULL,
    content TEXT,
    confidence_score DECIMAL(3,2),
    language VARCHAR(10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (meeting_id) REFERENCES meetings(meeting_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS summaries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    meeting_id VARCHAR(255) NOT NULL,
    summary_content TEXT,
    key_points TEXT,
    action_items TEXT,
    model_used VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (meeting_id) REFERENCES meetings(meeting_id) ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX idx_meetings_meeting_id ON meetings(meeting_id);
CREATE INDEX idx_meetings_status ON meetings(status);
CREATE INDEX idx_meetings_start_time ON meetings(start_time);
CREATE INDEX idx_transcripts_meeting_id ON transcripts(meeting_id);
CREATE INDEX idx_summaries_meeting_id ON summaries(meeting_id);

-- Insert sample data (optional)
INSERT IGNORE INTO meetings (meeting_id, title, start_time, status) VALUES 
('sample-meeting-1', 'Sample Team Meeting', NOW(), 'completed'),
('sample-meeting-2', 'Project Discussion', NOW(), 'active');