import React, { useEffect, useState } from 'react';
import {
  Box,
  Typography,
  TextField,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  List,
  ListItem,
  ListItemText,
  Chip,
  Tabs,
  Tab,
} from '@mui/material';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import Loader from '../../components/common/Loader';
import { messageApi, Message, SendMessageRequest } from '../../api/messageApi';
import userApi from '../../api/userApi';

const Messages: React.FC = () => {
  const [messages, setMessages] = useState<Message[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState(0);
  const [composeDialogOpen, setComposeDialogOpen] = useState(false);
  const [users, setUsers] = useState<any[]>([]);
  const [newMessage, setNewMessage] = useState<SendMessageRequest>({
    receiverId: 0,
    content: '',
  });

  useEffect(() => {
    loadMessages();
    loadUsers();
  }, [activeTab]);

  const loadMessages = async () => {
    try {
      const type = activeTab === 0 ? 'inbox' : 'sent';
      const response = await messageApi.getAll({ type });
      const messagesData = response?.data?.data || [];
      setMessages(Array.isArray(messagesData) ? messagesData : []);
    } catch (error) {
      console.error('Failed to load messages:', error);
      setMessages([]);
    } finally {
      setLoading(false);
    }
  };

  const loadUsers = async () => {
    try {
      const response = await userApi.getUsers();
      const usersData = response?.data?.data || [];
      setUsers(Array.isArray(usersData) ? usersData : []);
    } catch (error) {
      console.error('Failed to load users:', error);
    }
  };

  const handleSendMessage = async () => {
    try {
      await messageApi.send(newMessage);
      setComposeDialogOpen(false);
      setNewMessage({ receiverId: 0, content: '' });
      loadMessages();
    } catch (error) {
      console.error('Failed to send message:', error);
    }
  };

  const handleMarkAsRead = async (id: number) => {
    try {
      await messageApi.markAsRead(id);
      loadMessages();
    } catch (error) {
      console.error('Failed to mark as read:', error);
    }
  };

  if (loading) {
    return <Loader message="Loading messages..." />;
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box>
          <Typography variant="h4" fontWeight={700} gutterBottom>
            💬 Messages
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Communicate with your team
          </Typography>
        </Box>
        <Button variant="contained" onClick={() => setComposeDialogOpen(true)}>
          + Compose
        </Button>
      </Box>

      <Box mb={3}>
        <Tabs value={activeTab} onChange={(_, val) => setActiveTab(val)}>
          <Tab label="Inbox" />
          <Tab label="Sent" />
        </Tabs>
      </Box>

      {messages.length === 0 ? (
        <Card>
          <Box textAlign="center" py={4}>
            <Typography variant="h6" color="text.secondary">
              No messages found
            </Typography>
            <Typography variant="body2" color="text.secondary" mt={1}>
              {activeTab === 0 ? 'Your inbox is empty' : 'You haven\'t sent any messages yet'}
            </Typography>
          </Box>
        </Card>
      ) : (
        <Card>
          <List>
            {messages.map((message, index) => (
              <ListItem
                key={message.id}
                divider={index < messages.length - 1}
                sx={{
                  bgcolor: !message.isRead && activeTab === 0 ? 'action.hover' : 'transparent',
                  cursor: 'pointer',
                  '&:hover': { bgcolor: 'action.selected' },
                }}
                onClick={() => !message.isRead && activeTab === 0 && handleMarkAsRead(message.id)}
              >
                <ListItemText
                  primary={
                    <Box display="flex" justifyContent="space-between" alignItems="center">
                      <Typography variant="subtitle1" fontWeight={!message.isRead ? 700 : 400}>
                        {activeTab === 0 ? `From: ${message.senderName}` : `To: ${message.receiverName}`}
                      </Typography>
                      <Box display="flex" gap={1} alignItems="center">
                        {!message.isRead && activeTab === 0 && (
                          <Chip label="New" color="primary" size="small" />
                        )}
                        <Typography variant="caption" color="text.secondary">
                          {new Date(message.createdAt).toLocaleString()}
                        </Typography>
                      </Box>
                    </Box>
                  }
                  secondary={
                    <Typography
                      variant="body2"
                      color="text.secondary"
                      sx={{
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        display: '-webkit-box',
                        WebkitLineClamp: 2,
                        WebkitBoxOrient: 'vertical',
                      }}
                    >
                      {message.content}
                    </Typography>
                  }
                />
              </ListItem>
            ))}
          </List>
        </Card>
      )}

      {/* Compose Dialog */}
      <Dialog open={composeDialogOpen} onClose={() => setComposeDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Compose Message</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} mt={1}>
            <TextField
              select
              label="To"
              value={newMessage.receiverId}
              onChange={(e) => setNewMessage({ ...newMessage, receiverId: Number(e.target.value) })}
              fullWidth
              SelectProps={{ native: true }}
            >
              <option value={0}>Select Recipient</option>
              {users.map((user) => (
                <option key={user.id} value={user.id}>
                  {user.name} ({user.email})
                </option>
              ))}
            </TextField>

            <TextField
              label="Message"
              value={newMessage.content}
              onChange={(e) => setNewMessage({ ...newMessage, content: e.target.value })}
              multiline
              rows={6}
              fullWidth
              placeholder="Type your message here..."
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setComposeDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleSendMessage}
            disabled={!newMessage.receiverId || !newMessage.content.trim()}
          >
            Send
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Messages;
