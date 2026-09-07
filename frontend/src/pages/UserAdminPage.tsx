import AddRounded from "@mui/icons-material/AddRounded";
import BlockRounded from "@mui/icons-material/BlockRounded";
import CheckCircleRounded from "@mui/icons-material/CheckCircleRounded";
import ContentCopyRounded from "@mui/icons-material/ContentCopyRounded";
import EditRounded from "@mui/icons-material/EditRounded";
import MailRounded from "@mui/icons-material/MailRounded";
import PeopleRounded from "@mui/icons-material/PeopleRounded";
import RefreshRounded from "@mui/icons-material/RefreshRounded";
import SearchRounded from "@mui/icons-material/SearchRounded";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import Chip from "@mui/material/Chip";
import CircularProgress from "@mui/material/CircularProgress";
import Dialog from "@mui/material/Dialog";
import DialogActions from "@mui/material/DialogActions";
import DialogContent from "@mui/material/DialogContent";
import DialogTitle from "@mui/material/DialogTitle";
import IconButton from "@mui/material/IconButton";
import InputAdornment from "@mui/material/InputAdornment";
import MenuItem from "@mui/material/MenuItem";
import Pagination from "@mui/material/Pagination";
import Stack from "@mui/material/Stack";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import TextField from "@mui/material/TextField";
import Tooltip from "@mui/material/Tooltip";
import Typography from "@mui/material/Typography";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useState, type FormEvent } from "react";
import { api } from "../api";
import type { InviteUserInput, UpdateUserInput, UserAdminItem } from "../api/types";
import { useAuthStore } from "../auth/authStore";

export function UserAdminPage() {
  const session = useAuthStore((state) => state.session)!;
  const queryClient = useQueryClient();
  const isAdmin = session.user.role === "ADMIN";

  const [page, setPage] = useState(1);
  const [pageSize] = useState(10);
  const [search, setSearch] = useState("");
  const [roleFilter, setRoleFilter] = useState<string>("ALL");
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const { data: userPageData, isPending: loading, error: queryError } = useQuery({
    queryKey: ["users", page, pageSize],
    queryFn: () => api.listUsers(page - 1, pageSize, session.accessToken)
  });

  const users = userPageData?.items ?? [];
  const total = userPageData?.total ?? 0;

  // Invite Modal State
  const [inviteOpen, setInviteOpen] = useState(false);
  const [inviteForm, setInviteForm] = useState<InviteUserInput>({
    name: "",
    email: "",
    phone: "",
    role: "DISPATCHER"
  });
  const [inviting, setInviting] = useState(false);
  const [generatedInvite, setGeneratedInvite] = useState<{ token: string; email: string } | null>(null);

  // Edit Modal State
  const [editOpen, setEditOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState<UserAdminItem | null>(null);
  const [editForm, setEditForm] = useState<UpdateUserInput>({ name: "", phone: "", role: "DISPATCHER" });
  const [editing, setEditing] = useState(false);

  // Suspend/Reactivate Dialog State
  const [actionUser, setActionUser] = useState<UserAdminItem | null>(null);
  const [actionType, setActionType] = useState<"suspend" | "reactivate" | null>(null);
  const [actionLoading, setActionLoading] = useState(false);

  const handleInviteSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setInviting(true);
    setError(null);
    try {
      const res = await api.inviteUser(inviteForm, session.accessToken);
      setGeneratedInvite({ token: res.token, email: res.email });
      setSuccess(`Invitation created for ${res.email}`);
      await queryClient.invalidateQueries({ queryKey: ["users"] });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to send invitation");
    } finally {
      setInviting(false);
    }
  };

  const handleEditSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!selectedUser) return;
    setEditing(true);
    setError(null);
    try {
      await api.updateUser(selectedUser.id, editForm, session.accessToken);
      setSuccess(`User ${selectedUser.email} updated successfully.`);
      setEditOpen(false);
      await queryClient.invalidateQueries({ queryKey: ["users"] });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to update user");
    } finally {
      setEditing(false);
    }
  };

  const handleStatusAction = async () => {
    if (!actionUser || !actionType) return;
    setActionLoading(true);
    setError(null);
    try {
      if (actionType === "suspend") {
        await api.suspendUser(actionUser.id, session.accessToken);
        setSuccess(`User ${actionUser.email} suspended.`);
      } else {
        await api.reactivateUser(actionUser.id, session.accessToken);
        setSuccess(`User ${actionUser.email} reactivated.`);
      }
      setActionUser(null);
      setActionType(null);
      await queryClient.invalidateQueries({ queryKey: ["users"] });
    } catch (err) {
      setError(err instanceof Error ? err.message : `Failed to ${actionType} user`);
    } finally {
      setActionLoading(false);
    }
  };

  const filteredUsers = users.filter((u) => {
    const matchSearch =
      u.name.toLowerCase().includes(search.toLowerCase()) ||
      u.email.toLowerCase().includes(search.toLowerCase()) ||
      (u.phone && u.phone.includes(search));
    const matchRole = roleFilter === "ALL" || u.role === roleFilter;
    return matchSearch && matchRole;
  });

  const inviteUrl = generatedInvite
    ? `${window.location.origin}/invite/accept?token=${generatedInvite.token}`
    : "";

  return (
    <Box>
      {/* Header */}
      <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" alignItems={{ sm: "center" }} spacing={2} mb={3}>
        <Box>
          <Stack direction="row" alignItems="center" spacing={1.5}>
            <PeopleRounded sx={{ fontSize: 28, color: "primary.main" }} />
            <Typography variant="h5" fontWeight={800}>
              User Administration
            </Typography>
          </Stack>
          <Typography variant="body2" color="text.secondary">
            Manage organization members, security roles, one-time invitations, and authentication statuses.
          </Typography>
        </Box>

        <Stack direction="row" spacing={1.5}>
          <Button
            variant="outlined"
            startIcon={loading ? <CircularProgress size={16} /> : <RefreshRounded />}
            onClick={() => void queryClient.invalidateQueries({ queryKey: ["users"] })}
            disabled={loading}
          >
            Refresh
          </Button>
          <Button
            variant="contained"
            startIcon={<AddRounded />}
            onClick={() => {
              setGeneratedInvite(null);
              setInviteForm({ name: "", email: "", phone: "", role: "DISPATCHER" });
              setInviteOpen(true);
            }}
            sx={{ fontWeight: 700 }}
          >
            Invite User
          </Button>
        </Stack>
      </Stack>

      {(error || queryError) && (
        <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>
          {error ?? (queryError instanceof Error ? queryError.message : "Failed to load user directory")}
        </Alert>
      )}

      {success && (
        <Alert severity="success" icon={<CheckCircleRounded />} sx={{ mb: 3 }} onClose={() => setSuccess(null)}>
          {success}
        </Alert>
      )}

      {/* Filters */}
      <Card variant="outlined" sx={{ mb: 3, borderRadius: 3 }}>
        <CardContent sx={{ p: 2 }}>
          <Stack direction={{ xs: "column", sm: "row" }} spacing={2} alignItems="center">
            <TextField
              size="small"
              placeholder="Search by name, email or phone…"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              sx={{ flex: 1 }}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchRounded fontSize="small" sx={{ color: "text.secondary" }} />
                  </InputAdornment>
                )
              }}
            />
            <TextField
              select
              size="small"
              label="Role Filter"
              value={roleFilter}
              onChange={(e) => setRoleFilter(e.target.value)}
              sx={{ minWidth: 160 }}
            >
              <MenuItem value="ALL">All Roles</MenuItem>
              <MenuItem value="ADMIN">ADMIN</MenuItem>
              <MenuItem value="DISPATCHER">DISPATCHER</MenuItem>
              <MenuItem value="DRIVER">DRIVER</MenuItem>
            </TextField>
          </Stack>
        </CardContent>
      </Card>

      {/* User Table */}
      <Card variant="outlined" sx={{ borderRadius: 3, overflow: "hidden" }}>
        <TableContainer>
          <Table>
            <TableHead sx={{ bgcolor: "action.hover" }}>
              <TableRow>
                <TableCell sx={{ fontWeight: 700 }}>User</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Role</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Status</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Phone</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Created</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Last Active</TableCell>
                <TableCell align="right" sx={{ fontWeight: 700 }}>
                  Actions
                </TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {loading ? (
                <TableRow>
                  <TableCell colSpan={7} align="center" sx={{ py: 6 }}>
                    <CircularProgress size={32} />
                  </TableCell>
                </TableRow>
              ) : filteredUsers.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} align="center" sx={{ py: 6 }}>
                    <Typography variant="body2" color="text.secondary">
                      No users match the search criteria.
                    </Typography>
                  </TableCell>
                </TableRow>
              ) : (
                filteredUsers.map((u) => {
                  const isSuspended = u.status === "SUSPENDED";
                  const isInvited = u.status === "INVITED";
                  return (
                    <TableRow key={u.id} hover>
                      <TableCell>
                        <Typography variant="subtitle2" fontWeight={700}>
                          {u.name}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {u.email}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={u.role}
                          size="small"
                          color={u.role === "ADMIN" ? "primary" : u.role === "DISPATCHER" ? "info" : "default"}
                          sx={{ fontWeight: 700 }}
                        />
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={u.status}
                          size="small"
                          color={isSuspended ? "error" : isInvited ? "warning" : "success"}
                          variant={isInvited ? "outlined" : "filled"}
                          sx={{ fontWeight: 700 }}
                        />
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2">{u.phone}</Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="caption" color="text.secondary">
                          {new Date(u.createdAt).toLocaleDateString()}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="caption" color="text.secondary">
                          {u.lastActiveAt ? new Date(u.lastActiveAt).toLocaleString() : "Never"}
                        </Typography>
                      </TableCell>
                      <TableCell align="right">
                        <Stack direction="row" spacing={0.5} justifyContent="flex-end">
                          {isAdmin && (
                            <Tooltip title="Edit User">
                              <IconButton
                                size="small"
                                onClick={() => {
                                  setSelectedUser(u);
                                  setEditForm({ name: u.name, phone: u.phone, role: u.role });
                                  setEditOpen(true);
                                }}
                              >
                                <EditRounded fontSize="small" />
                              </IconButton>
                            </Tooltip>
                          )}

                          {isAdmin && (
                            <Tooltip title={isSuspended ? "Reactivate User" : "Suspend User"}>
                              <IconButton
                                size="small"
                                color={isSuspended ? "success" : "error"}
                                onClick={() => {
                                  setActionUser(u);
                                  setActionType(isSuspended ? "reactivate" : "suspend");
                                }}
                              >
                                <BlockRounded fontSize="small" />
                              </IconButton>
                            </Tooltip>
                          )}
                        </Stack>
                      </TableCell>
                    </TableRow>
                  );
                })
              )}
            </TableBody>
          </Table>
        </TableContainer>

        {total > pageSize && (
          <Box p={2} display="flex" justifyContent="center">
            <Pagination
              count={Math.ceil(total / pageSize)}
              page={page}
              onChange={(_, p) => setPage(p)}
              color="primary"
            />
          </Box>
        )}
      </Card>

      {/* Invite User Dialog */}
      <Dialog open={inviteOpen} onClose={() => setInviteOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle fontWeight={700}>
          {generatedInvite ? "Invitation Link Generated" : "Invite Organization Member"}
        </DialogTitle>
        <DialogContent dividers>
          {generatedInvite ? (
            <Stack spacing={2.5} py={1}>
              <Alert severity="success" icon={<MailRounded />}>
                One-time invitation token generated for <strong>{generatedInvite.email}</strong>. Share this onboarding link securely with the user to allow them to set their password.
              </Alert>

              <Box>
                <Typography variant="caption" color="text.secondary" fontWeight={700} display="block" mb={0.5}>
                  Invitation Link (Valid for 7 days)
                </Typography>
                <TextField
                  fullWidth
                  size="small"
                  value={inviteUrl}
                  InputProps={{
                    readOnly: true,
                    endAdornment: (
                      <InputAdornment position="end">
                        <IconButton
                          size="small"
                          onClick={() => {
                            void navigator.clipboard.writeText(inviteUrl);
                            setSuccess("Invitation link copied to clipboard.");
                          }}
                        >
                          <ContentCopyRounded fontSize="small" />
                        </IconButton>
                      </InputAdornment>
                    )
                  }}
                />
              </Box>

              <Box>
                <Typography variant="caption" color="text.secondary" fontWeight={700} display="block" mb={0.5}>
                  Raw Token Key
                </Typography>
                <Typography variant="body2" sx={{ fontFamily: "monospace", wordBreak: "break-all", p: 1.5, bgcolor: "action.hover", borderRadius: 1.5 }}>
                  {generatedInvite.token}
                </Typography>
              </Box>
            </Stack>
          ) : (
            <Box component="form" id="invite-form" onSubmit={handleInviteSubmit} sx={{ pt: 1 }}>
              <Stack spacing={2.5}>
                <TextField
                  fullWidth
                  label="Full Name *"
                  value={inviteForm.name}
                  onChange={(e) => setInviteForm({ ...inviteForm, name: e.target.value })}
                  required
                  disabled={inviting}
                />
                <TextField
                  fullWidth
                  type="email"
                  label="Business Email *"
                  value={inviteForm.email}
                  onChange={(e) => setInviteForm({ ...inviteForm, email: e.target.value })}
                  required
                  disabled={inviting}
                  helperText="User will receive their onboarding verification on this address"
                />
                <TextField
                  fullWidth
                  label="Phone Number *"
                  value={inviteForm.phone}
                  onChange={(e) => setInviteForm({ ...inviteForm, phone: e.target.value })}
                  required
                  disabled={inviting}
                  helperText="E.164 format (+15550100000)"
                />
                <TextField
                  select
                  fullWidth
                  label="Role *"
                  value={inviteForm.role}
                  onChange={(e) => setInviteForm({ ...inviteForm, role: e.target.value as InviteUserInput["role"] })}
                  disabled={inviting}
                >
                  {isAdmin && <MenuItem value="ADMIN">ADMIN — Full organization control</MenuItem>}
                  <MenuItem value="DISPATCHER">DISPATCHER — Operational fleet management</MenuItem>
                  <MenuItem value="DRIVER">DRIVER — Mobile vehicle and route assignments</MenuItem>
                </TextField>
              </Stack>
            </Box>
          )}
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          {generatedInvite ? (
            <Button variant="contained" onClick={() => setInviteOpen(false)}>
              Done
            </Button>
          ) : (
            <>
              <Button onClick={() => setInviteOpen(false)} disabled={inviting}>
                Cancel
              </Button>
              <Button
                type="submit"
                form="invite-form"
                variant="contained"
                disabled={inviting}
                startIcon={inviting ? <CircularProgress size={16} color="inherit" /> : <MailRounded />}
              >
                {inviting ? "Generating Invitation…" : "Generate Invitation"}
              </Button>
            </>
          )}
        </DialogActions>
      </Dialog>

      {/* Edit User Dialog */}
      <Dialog open={editOpen} onClose={() => setEditOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle fontWeight={700}>Edit User Details</DialogTitle>
        <DialogContent dividers>
          <Box component="form" id="edit-user-form" onSubmit={handleEditSubmit} sx={{ pt: 1 }}>
            <Stack spacing={2.5}>
              <TextField
                fullWidth
                label="Email"
                value={selectedUser?.email ?? ""}
                disabled
                helperText="Email address cannot be changed"
              />
              <TextField
                fullWidth
                label="Full Name *"
                value={editForm.name}
                onChange={(e) => setEditForm({ ...editForm, name: e.target.value })}
                required
                disabled={editing}
              />
              <TextField
                fullWidth
                label="Phone Number *"
                value={editForm.phone}
                onChange={(e) => setEditForm({ ...editForm, phone: e.target.value })}
                required
                disabled={editing}
              />
              {isAdmin && (
                <TextField
                  select
                  fullWidth
                  label="Role"
                  value={editForm.role}
                  onChange={(e) => setEditForm({ ...editForm, role: e.target.value as UpdateUserInput["role"] })}
                  disabled={editing}
                  helperText="Demoting the final admin is protected by platform policies"
                >
                  <MenuItem value="ADMIN">ADMIN</MenuItem>
                  <MenuItem value="DISPATCHER">DISPATCHER</MenuItem>
                  <MenuItem value="DRIVER">DRIVER</MenuItem>
                </TextField>
              )}
            </Stack>
          </Box>
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setEditOpen(false)} disabled={editing}>
            Cancel
          </Button>
          <Button
            type="submit"
            form="edit-user-form"
            variant="contained"
            disabled={editing}
            startIcon={editing ? <CircularProgress size={16} color="inherit" /> : null}
          >
            {editing ? "Saving…" : "Save Changes"}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Suspend / Reactivate Confirmation Dialog */}
      <Dialog open={Boolean(actionUser && actionType)} onClose={() => setActionUser(null)} maxWidth="xs" fullWidth>
        <DialogTitle fontWeight={700}>
          {actionType === "suspend" ? "Suspend User Account?" : "Reactivate User Account?"}
        </DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary">
            {actionType === "suspend"
              ? `Are you sure you want to suspend ${actionUser?.name} (${actionUser?.email})? Suspended users cannot authenticate or access operational tools.`
              : `Reactivating ${actionUser?.name} (${actionUser?.email}) will restore their platform login and permissions.`}
          </Typography>
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setActionUser(null)} disabled={actionLoading}>
            Cancel
          </Button>
          <Button
            variant="contained"
            color={actionType === "suspend" ? "error" : "success"}
            onClick={() => void handleStatusAction()}
            disabled={actionLoading}
            startIcon={actionLoading ? <CircularProgress size={16} color="inherit" /> : null}
          >
            {actionLoading ? "Processing…" : actionType === "suspend" ? "Confirm Suspension" : "Confirm Reactivate"}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
