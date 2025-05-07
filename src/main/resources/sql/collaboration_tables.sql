-- Project Messages Table
CREATE TABLE IF NOT EXISTS project_messages (
    id INT AUTO_INCREMENT PRIMARY KEY,
    project_id INT NOT NULL,
    sender_email VARCHAR(255) NOT NULL,
    message_text TEXT NOT NULL,
    sent_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projet(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_email) REFERENCES postuler(email)
);

-- Project Tasks Table
CREATE TABLE IF NOT EXISTS project_tasks (
    id INT AUTO_INCREMENT PRIMARY KEY,
    project_id INT NOT NULL,
    task_description TEXT NOT NULL,
    status VARCHAR(50) DEFAULT 'Pending',
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    FOREIGN KEY (project_id) REFERENCES projet(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES postuler(email)
);

-- Project Files Table
CREATE TABLE IF NOT EXISTS project_files (
    id INT AUTO_INCREMENT PRIMARY KEY,
    project_id INT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(1000),
    uploaded_by VARCHAR(255) NOT NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projet(id) ON DELETE CASCADE,
    FOREIGN KEY (uploaded_by) REFERENCES postuler(email)
); 