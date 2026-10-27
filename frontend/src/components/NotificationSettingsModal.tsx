import CheckCircleOutlineRounded from "@mui/icons-material/CheckCircleOutlineRounded";
import CloseRounded from "@mui/icons-material/CloseRounded";
import DeleteOutlineRounded from "@mui/icons-material/DeleteOutlineRounded";
import ErrorOutlineRounded from "@mui/icons-material/ErrorOutlineRounded";
import KeyRounded from "@mui/icons-material/KeyRounded";
import NotificationsActiveRounded from "@mui/icons-material/NotificationsActiveRounded";
import SendRounded from "@mui/icons-material/SendRounded";
import WebhookRounded from "@mui/icons-material/WebhookRounded";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import CircularProgress from "@mui/material/CircularProgress";
import Dialog from "@mui/material/Dialog";
import DialogContent from "@mui/material/DialogContent";
import DialogTitle from "@mui/material/DialogTitle";
import FormControl from "@mui/material/FormControl";
import IconButton from "@mui/material/IconButton";
import InputLabel from "@mui/material/InputLabel";
import MenuItem from "@mui/material/MenuItem";
import Select from "@mui/material/Select";
import Stack from "@mui/material/Stack";
import Tab from "@mui/material/Tab";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import Tabs from "@mui/material/Tabs";
import TextField from "@mui/material/TextField";
import Tooltip from "@mui/material/Tooltip";
import Typography from "@mui/material/Typography";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { neepoApi } from "../api";
import type {
  CreateNotificationChannelRequest,
  DisruptionSeverity,
  NotificationChannel,
  NotificationChannelType,
  NotificationTestResult
} from "../api/types";
import { useAuthStore } from "../auth/authStore";

interface NotificationSettingsModalProps {
  open: boolean;
  onClose: () => void;
}

const severityColors: Record<DisruptionSeverity, { bg: string; color: string }> = {
  LOW: { bg: "rgba(16, 185, 129, 0.12)", color: "#10b981" },
  MEDIUM: { bg: "rgba(245, 158, 11, 0.12)", color: "#f59e0b" },
  HIGH: { bg: "rgba(249, 115, 22, 0.12)", color: "#f97316" },
  CRITICAL: { bg: "rgba(239, 68, 68, 0.15)", color: "#ef4444" }
};

export function NotificationSettingsModal({ open, onClose }: NotificationSettingsModalProps) {
  const session = useAuthStore((state) => state.session);
  const token = session?.accessToken ?? "";
  const queryClient = useQueryClient();

  const [activeTab, setActiveTab] = useState<number>(0);
  const [testResult, setTestResult] = useState<NotificationTestResult | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  // New Channel Form State
  const [name, setName] = useState("");
  const [channelType, setChannelType] = useState<NotificationChannelType>("WEBHOOK");
  const [targetEndpoint, setTargetEndpoint] = useState("");
  const [secretKey, setSecretKey] = useState("");
  const [minimumSeverity, setMinimumSeverity] = useState<DisruptionSeverity>("MEDIUM");

  const channelsQuery = useQuery({
    queryKey: ["notificationChannels"],
    queryFn: () => neepoApi.listNotificationChannels(token),
    enabled: open && !!token
  });

  const logsQuery = useQuery({
    queryKey: ["notificationLogs"],
    queryFn: () => neepoApi.listNotificationLogs(token, 50),
    enabled: open && !!token
  });

  const createMutation = useMutation({
    mutationFn: (req: CreateNotificationChannelRequest) =>
      neepoApi.createNotificationChannel(req, token),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["notificationChannels"] });
      setName("");
      setTargetEndpoint("");
      setSecretKey("");
      setActiveTab(0);
      setActionError(null);
    },
    onError: (err: Error) => {
      setActionError(err.message || "Failed to create channel");
    }
  });

  const deleteMutation = useMutation({
    mutationFn: (channelId: string) => neepoApi.deleteNotificationChannel(channelId, token),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["notificationChannels"] });
      setActionError(null);
    },
    onError: (err: Error) => {
      setActionError(err.message || "Failed to delete channel");
    }
  });

  const testMutation = useMutation({
    mutationFn: (channelId: string) => neepoApi.testNotificationChannel(channelId, token),
    onSuccess: (data) => {
      setTestResult(data);
      queryClient.invalidateQueries({ queryKey: ["notificationLogs"] });
      setActionError(null);
    },
    onError: (err: Error) => {
      setActionError(err.message || "Test dispatch failed");
    }
  });

  const handleCreateSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !targetEndpoint.trim()) return;
    createMutation.mutate({
      name: name.trim(),
      channelType,
      targetEndpoint: targetEndpoint.trim(),
      secretKey: secretKey.trim() || undefined,
      minimumSeverity,
      active: true
    });
  };

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="md"
      fullWidth
      PaperProps={{
        sx: {
          borderRadius: 3,
          bgcolor: "background.paper",
          backgroundImage: "none",
          boxShadow: "0 24px 48px -12px rgba(0,0,0,0.3)"
        }
      }}
    >
      <DialogTitle sx={{ p: 2.5, pb: 1.5, display: "flex", alignItems: "center", justifyContent: "space-between" }}>
        <Stack direction="row" alignItems="center" spacing={1.5}>
          <Box
            sx={{
              width: 36,
              height: 36,
              borderRadius: "9px",
              bgcolor: "rgba(13, 91, 99, 0.12)",
              display: "grid",
              placeItems: "center",
              color: "#0d5b63"
            }}
          >
            <NotificationsActiveRounded sx={{ fontSize: 20 }} />
          </Box>
          <Box>
            <Typography variant="h6" fontWeight={700} sx={{ lineHeight: 1.2 }}>
              Alert & Webhook Engine
            </Typography>
            <Typography variant="caption" color="text.secondary">
              Real-time multi-channel incident dispatch with HMAC-SHA256 signatures
            </Typography>
          </Box>
        </Stack>
        <IconButton id="notification-modal-close" onClick={onClose} size="small">
          <CloseRounded fontSize="small" />
        </IconButton>
      </DialogTitle>

      <Box sx={{ borderBottom: 1, borderColor: "divider", px: 2.5 }}>
        <Tabs value={activeTab} onChange={(_e, v) => { setActiveTab(v); setActionError(null); setTestResult(null); }}>
          <Tab label={`Channels (${channelsQuery.data?.length ?? 0})`} id="tab-channels" sx={{ textTransform: "none", fontWeight: 600 }} />
          <Tab label="Add Channel" id="tab-add-channel" sx={{ textTransform: "none", fontWeight: 600 }} />
          <Tab label={`Delivery Logs (${logsQuery.data?.length ?? 0})`} id="tab-delivery-logs" sx={{ textTransform: "none", fontWeight: 600 }} />
        </Tabs>
      </Box>

      <DialogContent sx={{ p: 2.5 }}>
        {actionError && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => setActionError(null)}>
            {actionError}
          </Alert>
        )}

        {testResult && (
          <Alert
            severity={testResult.status === "DELIVERED" ? "success" : "warning"}
            sx={{ mb: 2 }}
            onClose={() => setTestResult(null)}
          >
            <Typography variant="subtitle2" fontWeight={700}>
              {testResult.message}
            </Typography>
            <Typography variant="caption" sx={{ display: "block", mt: 0.5, fontFamily: "monospace", wordBreak: "break-all" }}>
              X-Neepo-Signature: {testResult.signature}
            </Typography>
          </Alert>
        )}

        {/* Tab 0: Configured Channels */}
        {activeTab === 0 && (
          <Stack spacing={2}>
            {channelsQuery.isLoading ? (
              <Box sx={{ display: "grid", placeItems: "center", py: 5 }}>
                <CircularProgress size={32} />
              </Box>
            ) : channelsQuery.data?.length === 0 ? (
              <Box sx={{ textAlign: "center", py: 4, bgcolor: "rgba(0,0,0,0.02)", borderRadius: 2 }}>
                <WebhookRounded sx={{ fontSize: 44, color: "text.disabled", mb: 1 }} />
                <Typography variant="subtitle1" fontWeight={600} color="text.secondary">
                  No notification channels configured
                </Typography>
                <Typography variant="caption" color="text.secondary" display="block" mb={2}>
                  Configure a webhook endpoint to receive real-time disruption events.
                </Typography>
                <Button variant="contained" size="small" onClick={() => setActiveTab(1)}>
                  Add Channel
                </Button>
              </Box>
            ) : (
              <Stack spacing={1.5}>
                {channelsQuery.data?.map((channel: NotificationChannel) => {
                  const sevStyle = severityColors[channel.minimumSeverity];
                  const isTesting = testMutation.isPending && testMutation.variables === channel.id;
                  const isDeleting = deleteMutation.isPending && deleteMutation.variables === channel.id;

                  return (
                    <Box
                      key={channel.id}
                      sx={{
                        p: 2,
                        borderRadius: 2.5,
                        border: "1px solid",
                        borderColor: "divider",
                        bgcolor: "background.paper",
                        boxShadow: "0 2px 8px rgba(0,0,0,0.03)",
                        transition: "all 0.2s ease"
                      }}
                    >
                      <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" alignItems={{ sm: "center" }} spacing={1.5}>
                        <Stack spacing={0.5}>
                          <Stack direction="row" alignItems="center" spacing={1} flexWrap="wrap">
                            <Typography variant="subtitle1" fontWeight={700}>
                              {channel.name}
                            </Typography>
                            <Chip label={channel.channelType} size="small" sx={{ fontWeight: 700, fontSize: "0.7rem", height: 20 }} />
                            <Chip
                              label={`Min: ${channel.minimumSeverity}`}
                              size="small"
                              sx={{
                                bgcolor: sevStyle.bg,
                                color: sevStyle.color,
                                fontWeight: 700,
                                fontSize: "0.7rem",
                                height: 20
                              }}
                            />
                            {channel.active && (
                              <Chip
                                label="ACTIVE"
                                size="small"
                                sx={{ bgcolor: "rgba(16, 185, 129, 0.1)", color: "#10b981", fontWeight: 700, fontSize: "0.65rem", height: 18 }}
                              />
                            )}
                          </Stack>
                          <Typography variant="caption" sx={{ fontFamily: "monospace", color: "text.secondary", wordBreak: "break-all" }}>
                            {channel.targetEndpoint}
                          </Typography>
                          <Stack direction="row" alignItems="center" spacing={0.5}>
                            <KeyRounded sx={{ fontSize: 13, color: "text.disabled" }} />
                            <Typography variant="caption" sx={{ fontFamily: "monospace", color: "text.disabled", fontSize: "0.7rem" }}>
                              Secret: {channel.secretKey}
                            </Typography>
                          </Stack>
                        </Stack>

                        <Stack direction="row" spacing={1} alignItems="center">
                          <Tooltip title="Send test ping with HMAC-SHA256 signature">
                            <Button
                              id={`btn-test-channel-${channel.id}`}
                              size="small"
                              variant="outlined"
                              startIcon={isTesting ? <CircularProgress size={14} /> : <SendRounded sx={{ fontSize: 14 }} />}
                              disabled={isTesting || isDeleting}
                              onClick={() => testMutation.mutate(channel.id)}
                              sx={{ textTransform: "none", fontWeight: 600 }}
                            >
                              Test Alert
                            </Button>
                          </Tooltip>

                          <Tooltip title="Delete channel">
                            <IconButton
                              id={`btn-delete-channel-${channel.id}`}
                              size="small"
                              color="error"
                              disabled={isTesting || isDeleting}
                              onClick={() => deleteMutation.mutate(channel.id)}
                            >
                              <DeleteOutlineRounded fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        </Stack>
                      </Stack>
                    </Box>
                  );
                })}
              </Stack>
            )}
          </Stack>
        )}

        {/* Tab 1: Add New Channel */}
        {activeTab === 1 && (
          <Box component="form" onSubmit={handleCreateSubmit}>
            <Stack spacing={2.5}>
              <TextField
                id="input-channel-name"
                label="Channel Name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. Operations Slack Webhook"
                required
                fullWidth
                size="small"
              />

              <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
                <FormControl fullWidth size="small">
                  <InputLabel id="label-channel-type">Channel Protocol</InputLabel>
                  <Select
                    labelId="label-channel-type"
                    id="select-channel-type"
                    value={channelType}
                    label="Channel Protocol"
                    onChange={(e) => setChannelType(e.target.value as NotificationChannelType)}
                  >
                    <MenuItem value="WEBHOOK">Webhook (HTTP POST)</MenuItem>
                    <MenuItem value="SMS">SMS Gateway</MenuItem>
                    <MenuItem value="WEBPUSH">Web Push Notification</MenuItem>
                  </Select>
                </FormControl>

                <FormControl fullWidth size="small">
                  <InputLabel id="label-min-severity">Minimum Severity Threshold</InputLabel>
                  <Select
                    labelId="label-min-severity"
                    id="select-min-severity"
                    value={minimumSeverity}
                    label="Minimum Severity Threshold"
                    onChange={(e) => setMinimumSeverity(e.target.value as DisruptionSeverity)}
                  >
                    <MenuItem value="LOW">LOW (All incidents)</MenuItem>
                    <MenuItem value="MEDIUM">MEDIUM (Standard disruptions)</MenuItem>
                    <MenuItem value="HIGH">HIGH (Urgent operational events)</MenuItem>
                    <MenuItem value="CRITICAL">CRITICAL (Safety & cargo threats only)</MenuItem>
                  </Select>
                </FormControl>
              </Stack>

              <TextField
                id="input-target-endpoint"
                label="Target Endpoint URL / Gateway Identifier"
                value={targetEndpoint}
                onChange={(e) => setTargetEndpoint(e.target.value)}
                placeholder="https://hooks.slack.com/services/... or https://alerts.yourcompany.com/webhook"
                required
                fullWidth
                size="small"
                helperText="NEEPO-CARE will deliver signed JSON payloads via HTTP POST to this endpoint."
              />

              <TextField
                id="input-secret-key"
                label="HMAC-SHA256 Signing Secret (Optional)"
                value={secretKey}
                onChange={(e) => setSecretKey(e.target.value)}
                placeholder="Leave blank to auto-generate a secure 32-byte secret"
                fullWidth
                size="small"
                helperText="Payloads include header X-Neepo-Signature calculated using this secret."
              />

              <Box sx={{ display: "flex", justifyContent: "flex-end", gap: 1.5, pt: 1 }}>
                <Button variant="text" onClick={() => setActiveTab(0)}>
                  Cancel
                </Button>
                <Button
                  id="btn-submit-channel"
                  type="submit"
                  variant="contained"
                  disabled={createMutation.isPending || !name.trim() || !targetEndpoint.trim()}
                  startIcon={createMutation.isPending && <CircularProgress size={16} />}
                >
                  Create Channel
                </Button>
              </Box>
            </Stack>
          </Box>
        )}

        {/* Tab 2: Delivery Logs & Audit */}
        {activeTab === 2 && (
          <Box>
            {logsQuery.isLoading ? (
              <Box sx={{ display: "grid", placeItems: "center", py: 5 }}>
                <CircularProgress size={32} />
              </Box>
            ) : logsQuery.data?.length === 0 ? (
              <Box sx={{ textAlign: "center", py: 4, color: "text.secondary" }}>
                <Typography variant="body2">No delivery attempts recorded yet.</Typography>
              </Box>
            ) : (
              <TableContainer sx={{ maxHeight: 380 }}>
                <Table size="small" stickyHeader>
                  <TableHead>
                    <TableRow>
                      <TableCell sx={{ fontWeight: 700, fontSize: "0.75rem" }}>Event / Disruption</TableCell>
                      <TableCell sx={{ fontWeight: 700, fontSize: "0.75rem" }}>Severity</TableCell>
                      <TableCell sx={{ fontWeight: 700, fontSize: "0.75rem" }}>Status</TableCell>
                      <TableCell sx={{ fontWeight: 700, fontSize: "0.75rem" }}>Signature / Details</TableCell>
                      <TableCell sx={{ fontWeight: 700, fontSize: "0.75rem" }} align="right">Delivered At</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {logsQuery.data?.map((log) => {
                      const sev = severityColors[log.severity];
                      const isSuccess = log.status === "DELIVERED";
                      const isSuppressed = log.status === "SUPPRESSED_BY_SEVERITY";

                      return (
                        <TableRow key={log.id} hover>
                          <TableCell sx={{ fontWeight: 600, fontSize: "0.8rem" }}>
                            {log.disruptionType}
                          </TableCell>
                          <TableCell>
                            <Chip
                              label={log.severity}
                              size="small"
                              sx={{
                                bgcolor: sev.bg,
                                color: sev.color,
                                fontWeight: 700,
                                fontSize: "0.65rem",
                                height: 18
                              }}
                            />
                          </TableCell>
                          <TableCell>
                            <Chip
                              icon={isSuccess ? <CheckCircleOutlineRounded sx={{ fontSize: "12px !important" }} /> : isSuppressed ? undefined : <ErrorOutlineRounded sx={{ fontSize: "12px !important" }} />}
                              label={log.status}
                              size="small"
                              sx={{
                                bgcolor: isSuccess
                                  ? "rgba(16, 185, 129, 0.12)"
                                  : isSuppressed
                                  ? "rgba(245, 158, 11, 0.12)"
                                  : "rgba(239, 68, 68, 0.12)",
                                color: isSuccess ? "#10b981" : isSuppressed ? "#f59e0b" : "#ef4444",
                                fontWeight: 700,
                                fontSize: "0.65rem",
                                height: 18
                              }}
                            />
                          </TableCell>
                          <TableCell sx={{ fontFamily: "monospace", fontSize: "0.7rem", color: "text.secondary" }}>
                            {log.signature ? `${log.signature.substring(0, 16)}...` : log.errorMessage || "—"}
                          </TableCell>
                          <TableCell align="right" sx={{ fontSize: "0.75rem", color: "text.secondary" }}>
                            {new Date(log.deliveredAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" })}
                          </TableCell>
                        </TableRow>
                      );
                    })}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </Box>
        )}
      </DialogContent>
    </Dialog>
  );
}
