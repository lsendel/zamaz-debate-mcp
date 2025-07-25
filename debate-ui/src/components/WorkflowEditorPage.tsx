import React from 'react';
import { Box, Typography, Paper } from '@mui/material';

const WorkflowEditorPage: React.FC = () => {
  return (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Typography variant='h4' component='h1' gutterBottom>
        Workflow Editor
      </Typography>

      <Paper
        sx={{
          flex: 1,
          p: 0,
          overflow: 'hidden',
          border: '1px solid #e0e0e0',
        }}
      >
        <iframe
          src='/workflow-ui/'
          style={{
            width: '100%',
            height: '100%',
            border: 'none',
            borderRadius: '4px',
          }}
          title='Workflow Editor'
        />
      </Paper>
    </Box>
  );
};

export default WorkflowEditorPage;
