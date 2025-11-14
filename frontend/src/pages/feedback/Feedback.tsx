import React, { useEffect, useState } from 'react';
import {
  Box,
  Typography,
  Grid,
  Rating,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  MenuItem,
} from '@mui/material';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import Loader from '../../components/common/Loader';
import { feedbackApi, Feedback as FeedbackType, SubmitFeedbackRequest } from '../../api/feedbackApi';
import { buddyMatchApi } from '../../api/buddyMatchApi';

const Feedback: React.FC = () => {
  const [feedbacks, setFeedbacks] = useState<FeedbackType[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitDialogOpen, setSubmitDialogOpen] = useState(false);
  const [matches, setMatches] = useState<any[]>([]);
  const [newFeedback, setNewFeedback] = useState<SubmitFeedbackRequest>({
    matchId: 0,
    toUserId: 0,
    rating: 0,
    comments: '',
    feedbackType: 'BUDDY_TO_EMPLOYEE',
  });

  useEffect(() => {
    loadFeedbacks();
    loadMatches();
  }, []);

  const loadFeedbacks = async () => {
    try {
      const response = await feedbackApi.getAll();
      const feedbacksData = response?.data?.data || [];
      setFeedbacks(Array.isArray(feedbacksData) ? feedbacksData : []);
    } catch (error) {
      console.error('Failed to load feedbacks:', error);
      setFeedbacks([]);
    } finally {
      setLoading(false);
    }
  };

  const loadMatches = async () => {
    try {
      const response = await buddyMatchApi.getAll();
      const matchesData = response?.data?.data || [];
      setMatches(Array.isArray(matchesData) ? matchesData : []);
    } catch (error) {
      console.error('Failed to load matches:', error);
    }
  };

  const handleSubmitFeedback = async () => {
    try {
      await feedbackApi.submit(newFeedback);
      setSubmitDialogOpen(false);
      setNewFeedback({
        matchId: 0,
        toUserId: 0,
        rating: 0,
        comments: '',
        feedbackType: 'BUDDY_TO_EMPLOYEE',
      });
      loadFeedbacks();
    } catch (error) {
      console.error('Failed to submit feedback:', error);
    }
  };

  const selectedMatch = matches.find(m => m.id === newFeedback.matchId);

  if (loading) {
    return <Loader message="Loading feedback..." />;
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box>
          <Typography variant="h4" fontWeight={700} gutterBottom>
            ⭐ Feedback
          </Typography>
          <Typography variant="body2" color="text.secondary">
            View and submit feedback
          </Typography>
        </Box>
        <Button variant="contained" onClick={() => setSubmitDialogOpen(true)}>
          + Submit Feedback
        </Button>
      </Box>

      {feedbacks.length === 0 ? (
        <Card>
          <Box textAlign="center" py={4}>
            <Typography variant="h6" color="text.secondary">
              No feedback found
            </Typography>
            <Typography variant="body2" color="text.secondary" mt={1}>
              Submit your first feedback to get started
            </Typography>
          </Box>
        </Card>
      ) : (
        <Grid container spacing={3}>
          {feedbacks.map((feedback) => (
            <Grid item xs={12} md={6} key={feedback.id}>
              <Card>
                <Box mb={2}>
                  <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
                    <Typography variant="h6" fontWeight={600}>
                      {feedback.feedbackType.replace(/_/g, ' ')}
                    </Typography>
                    <Rating value={feedback.rating} readOnly size="small" />
                  </Box>
                  <Typography variant="caption" color="text.secondary">
                    {new Date(feedback.createdAt).toLocaleString()}
                  </Typography>
                </Box>

                <Box mb={2}>
                  <Typography variant="body2" color="text.secondary" gutterBottom>
                    <strong>From:</strong> {feedback.fromUserName}
                  </Typography>
                  <Typography variant="body2" color="text.secondary" gutterBottom>
                    <strong>To:</strong> {feedback.toUserName}
                  </Typography>
                  <Typography variant="body2" color="text.secondary" gutterBottom>
                    <strong>Match ID:</strong> #{feedback.matchId}
                  </Typography>
                </Box>

                {feedback.comments && (
                  <Box>
                    <Typography variant="body2" color="text.secondary">
                      <strong>Comments:</strong>
                    </Typography>
                    <Typography variant="body2" mt={0.5}>
                      {feedback.comments}
                    </Typography>
                  </Box>
                )}
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      {/* Submit Feedback Dialog */}
      <Dialog open={submitDialogOpen} onClose={() => setSubmitDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Submit Feedback</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} mt={1}>
            <TextField
              select
              label="Match"
              value={newFeedback.matchId}
              onChange={(e) => {
                const matchId = Number(e.target.value);
                const match = matches.find(m => m.id === matchId);
                setNewFeedback({
                  ...newFeedback,
                  matchId,
                  toUserId: match?.newEmployeeId || 0,
                });
              }}
              fullWidth
            >
              <MenuItem value={0}>Select Match</MenuItem>
              {matches.map((match) => (
                <MenuItem key={match.id} value={match.id}>
                  Match #{match.id}: {match.buddyName} → {match.employeeName}
                </MenuItem>
              ))}
            </TextField>

            <TextField
              select
              label="Feedback Type"
              value={newFeedback.feedbackType}
              onChange={(e) => setNewFeedback({ ...newFeedback, feedbackType: e.target.value as any })}
              fullWidth
            >
              <MenuItem value="BUDDY_TO_EMPLOYEE">Buddy to Employee</MenuItem>
              <MenuItem value="EMPLOYEE_TO_BUDDY">Employee to Buddy</MenuItem>
              <MenuItem value="MANAGER_REVIEW">Manager Review</MenuItem>
            </TextField>

            {selectedMatch && (
              <TextField
                select
                label="To"
                value={newFeedback.toUserId}
                onChange={(e) => setNewFeedback({ ...newFeedback, toUserId: Number(e.target.value) })}
                fullWidth
              >
                <MenuItem value={selectedMatch.buddyUserId}>
                  {selectedMatch.buddyName} (Buddy)
                </MenuItem>
                <MenuItem value={selectedMatch.newEmployeeId}>
                  {selectedMatch.employeeName} (Employee)
                </MenuItem>
              </TextField>
            )}

            <Box>
              <Typography variant="body2" gutterBottom>
                Rating
              </Typography>
              <Rating
                value={newFeedback.rating}
                onChange={(_, value) => setNewFeedback({ ...newFeedback, rating: value || 0 })}
                size="large"
              />
            </Box>

            <TextField
              label="Comments (Optional)"
              value={newFeedback.comments}
              onChange={(e) => setNewFeedback({ ...newFeedback, comments: e.target.value })}
              multiline
              rows={4}
              fullWidth
              placeholder="Share your feedback..."
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSubmitDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleSubmitFeedback}
            disabled={!newFeedback.matchId || !newFeedback.toUserId || !newFeedback.rating}
          >
            Submit
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Feedback;
