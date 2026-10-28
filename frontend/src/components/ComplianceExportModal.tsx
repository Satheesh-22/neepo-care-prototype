import CheckCircleRounded from "@mui/icons-material/CheckCircleRounded";
import CloseRounded from "@mui/icons-material/CloseRounded";
import ContentCopyRounded from "@mui/icons-material/ContentCopyRounded";
import DownloadRounded from "@mui/icons-material/DownloadRounded";
import FactCheckRounded from "@mui/icons-material/FactCheckRounded";
import FileCopyRounded from "@mui/icons-material/FileCopyRounded";
import RefreshRounded from "@mui/icons-material/RefreshRounded";
import ShieldRounded from "@mui/icons-material/ShieldRounded";
import VerifiedUserRounded from "@mui/icons-material/VerifiedUserRounded";
import WarningAmberRounded from "@mui/icons-material/WarningAmberRounded";
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
import Paper from "@mui/material/Paper";
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
  ComplianceExportFormat,
  ComplianceExportPackage,
  ComplianceExportType,
  ComplianceVerificationResult
} from "../api/types";
import { useAuthStore } from "../auth/authStore";

export interface ComplianceExportModalProps {
  open: boolean;
  onClose: () => void;
  tripId: string;
  tripReferenceCode?: string;
}

export function ComplianceExportModal({
  open,
  onClose,
  tripId,
  tripReferenceCode
}: ComplianceExportModalProps) {
  const session = useAuthStore((state) => state.session)!;
  const queryClient = useQueryClient();

  const [activeTab, setActiveTab] = useState(0);
  const [exportType, setExportType] = useState<ComplianceExportType>("FULL_TRIP_DOSSIER");
  const [exportFormat, setExportFormat] = useState<ComplianceExportFormat>("PRINTABLE_HTML");
  const [exportedPackage, setExportedPackage] = useState<ComplianceExportPackage | null>(null);
  const [copiedDigest, setCopiedDigest] = useState(false);

  // Verifier states
  const [verifyManifestId, setVerifyManifestId] = useState("");
  const [verifyPayload, setVerifyPayload] = useState("");
  const [verificationResult, setVerificationResult] = useState<ComplianceVerificationResult | null>(null);

  // Query manifests ledger
  const manifestsQuery = useQuery({
    queryKey: ["compliance-manifests"],
    queryFn: () => neepoApi.listComplianceExports(50, session.accessToken),
    enabled: open
  });

  // Mutation for generating export
  const generateExport = useMutation({
    mutationFn: () =>
      neepoApi.generateComplianceExport(tripId, exportType, exportFormat, session.accessToken),
    onSuccess: (pkg) => {
      setExportedPackage(pkg);
      queryClient.invalidateQueries({ queryKey: ["compliance-manifests"] });
      // Pre-populate verifier tab with generated details
      setVerifyManifestId(pkg.manifest.id);
      setVerifyPayload(pkg.rawPayload);
    }
  });

  // Mutation for verifying export
  const verifyExport = useMutation({
    mutationFn: () =>
      neepoApi.verifyComplianceExport(
        verifyManifestId,
        { payload: verifyPayload },
        session.accessToken
      ),
    onSuccess: (res) => {
      setVerificationResult(res);
    }
  });

  const handleCopyDigest = (digest: string) => {
    if (navigator?.clipboard?.writeText) {
      navigator.clipboard.writeText(digest);
    }
    setCopiedDigest(true);
    setTimeout(() => setCopiedDigest(false), 2000);
  };

  const handleDownloadFile = () => {
    if (!exportedPackage) return;
    const blob = new Blob([exportedPackage.rawPayload], { type: exportedPackage.mediaType });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = exportedPackage.filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  const loadManifestIntoVerifier = (manifestId: string, digest: string) => {
    setVerifyManifestId(manifestId);
    if (exportedPackage && exportedPackage.manifest.id === manifestId) {
      setVerifyPayload(exportedPackage.rawPayload);
    } else {
      setVerifyPayload(`/* Paste payload for manifest ${digest.substring(0, 8)}... to verify */`);
    }
    setVerificationResult(null);
    setActiveTab(1);
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
          boxShadow: "0 20px 40px rgba(15, 23, 42, 0.2)",
          overflow: "hidden"
        }
      }}
    >
      <DialogTitle
        sx={{
          p: 2.5,
          bgcolor: "#0f172a",
          color: "#ffffff",
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center"
        }}
      >
        <Stack direction="row" spacing={1.5} alignItems="center">
          <ShieldRounded sx={{ color: "#38bdf8", fontSize: 28 }} />
          <Box>
            <Typography variant="h6" fontWeight={700} lineHeight={1.2}>
              Regulatory Compliance &amp; Audit Evidence
            </Typography>
            <Typography variant="caption" sx={{ color: "#94a3b8" }}>
              FDA 21 CFR Part 11 &amp; EU GDP · Tamper-Evident Cryptographic Ledger
            </Typography>
          </Box>
        </Stack>
        <IconButton onClick={onClose} size="small" sx={{ color: "#94a3b8", "&:hover": { color: "#ffffff" } }}>
          <CloseRounded />
        </IconButton>
      </DialogTitle>

      <Box sx={{ borderBottom: 1, borderColor: "divider", bgcolor: "#f8fafc" }}>
        <Tabs
          value={activeTab}
          onChange={(_e, v) => setActiveTab(v)}
          textColor="primary"
          indicatorColor="primary"
          sx={{ px: 2 }}
        >
          <Tab
            icon={<VerifiedUserRounded sx={{ fontSize: 18 }} />}
            iconPosition="start"
            label="Generate Export"
            sx={{ fontWeight: 650, fontSize: "0.875rem" }}
          />
          <Tab
            icon={<FactCheckRounded sx={{ fontSize: 18 }} />}
            iconPosition="start"
            label="Tamper Verifier"
            sx={{ fontWeight: 650, fontSize: "0.875rem" }}
          />
          <Tab
            icon={<FileCopyRounded sx={{ fontSize: 18 }} />}
            iconPosition="start"
            label="Audit Ledger"
            sx={{ fontWeight: 650, fontSize: "0.875rem" }}
          />
        </Tabs>
      </Box>

      <DialogContent sx={{ p: 3 }}>
        {/* Tab 0: Generate Export */}
        {activeTab === 0 && (
          <Stack spacing={3}>
            <Box
              sx={{
                p: 2,
                borderRadius: 2,
                bgcolor: "#f0f9ff",
                border: "1px solid #bae6fd"
              }}
            >
              <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" alignItems={{ xs: "flex-start", sm: "center" }} spacing={1}>
                <Box>
                  <Typography variant="subtitle2" fontWeight={700} color="#0369a1">
                    Trip Context: {tripReferenceCode || tripId}
                  </Typography>
                  <Typography variant="caption" color="#0c4a6e">
                    Export package includes verifiable digital audit trail, observations, and CARE decisions.
                  </Typography>
                </Box>
                <Chip
                  label="Audit-Ready"
                  color="info"
                  size="small"
                  sx={{ fontWeight: 700 }}
                />
              </Stack>
            </Box>

            <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
              <FormControl fullWidth size="small">
                <InputLabel id="export-type-label">Export Scope</InputLabel>
                <Select
                  labelId="export-type-label"
                  value={exportType}
                  label="Export Scope"
                  onChange={(e) => setExportType(e.target.value as ComplianceExportType)}
                >
                  <MenuItem value="FULL_TRIP_DOSSIER">
                    Full Trip Dossier (Telemetry, Decisions, Constraints)
                  </MenuItem>
                  <MenuItem value="COLD_CHAIN_EXCURSION">
                    Cold-Chain Excursion Report (Thermal Chain-of-Custody)
                  </MenuItem>
                  <MenuItem value="CARE_DECISION_CHAIN">
                    CARE Decision Chain (Algorithmic Selection &amp; Tie-Breaks)
                  </MenuItem>
                </Select>
              </FormControl>

              <FormControl fullWidth size="small">
                <InputLabel id="export-format-label">Package Format</InputLabel>
                <Select
                  labelId="export-format-label"
                  value={exportFormat}
                  label="Package Format"
                  onChange={(e) => setExportFormat(e.target.value as ComplianceExportFormat)}
                >
                  <MenuItem value="PRINTABLE_HTML">Printable HTML Dossier (Human-Readable)</MenuItem>
                  <MenuItem value="JSON_LD">JSON-LD Linked Data (Enterprise QMS)</MenuItem>
                  <MenuItem value="CSV">RFC-4180 CSV (Analytics &amp; Archive)</MenuItem>
                </Select>
              </FormControl>
            </Stack>

            <Box>
              <Button
                variant="contained"
                size="large"
                fullWidth
                startIcon={
                  generateExport.isPending ? (
                    <CircularProgress size={20} color="inherit" />
                  ) : (
                    <ShieldRounded />
                  )
                }
                onClick={() => generateExport.mutate()}
                disabled={generateExport.isPending}
                sx={{
                  py: 1.3,
                  bgcolor: "#0369a1",
                  "&:hover": { bgcolor: "#0284c7" },
                  borderRadius: 2,
                  fontWeight: 700
                }}
              >
                {generateExport.isPending
                  ? "Generating Cryptographic Package…"
                  : "Generate & Seal Compliance Package"}
              </Button>
            </Box>

            {generateExport.isError && (
              <Alert severity="error">
                {generateExport.error instanceof Error
                  ? generateExport.error.message
                  : "Failed to generate compliance export package"}
              </Alert>
            )}

            {exportedPackage && (
              <Paper
                variant="outlined"
                sx={{
                  p: 2.5,
                  borderRadius: 2,
                  bgcolor: "#fafafa",
                  borderColor: "#cbd5e1"
                }}
              >
                <Stack spacing={2}>
                  <Stack direction="row" justifyContent="space-between" alignItems="center">
                    <Stack direction="row" spacing={1} alignItems="center">
                      <CheckCircleRounded sx={{ color: "#16a34a", fontSize: 22 }} />
                      <Typography variant="subtitle2" fontWeight={700} color="#15803d">
                        Evidence Package Certified &amp; Registered in Ledger
                      </Typography>
                    </Stack>
                    <Button
                      variant="contained"
                      size="small"
                      color="success"
                      startIcon={<DownloadRounded />}
                      onClick={handleDownloadFile}
                      sx={{ fontWeight: 700 }}
                    >
                      Download {exportedPackage.manifest.format}
                    </Button>
                  </Stack>

                  <Box
                    sx={{
                      p: 1.5,
                      borderRadius: 1.5,
                      bgcolor: "#0f172a",
                      color: "#e2e8f0"
                    }}
                  >
                    <Stack direction="row" justifyContent="space-between" alignItems="center" mb={0.5}>
                      <Typography variant="caption" sx={{ color: "#38bdf8", fontWeight: 700, letterSpacing: "0.05em" }}>
                        SHA-256 TAMPER-EVIDENT DIGEST
                      </Typography>
                      <Tooltip title={copiedDigest ? "Copied!" : "Copy SHA-256 Digest"}>
                        <IconButton
                          size="small"
                          onClick={() => handleCopyDigest(exportedPackage.manifest.sha256Digest)}
                          sx={{ color: "#94a3b8", "&:hover": { color: "#ffffff" } }}
                        >
                          <ContentCopyRounded sx={{ fontSize: 16 }} />
                        </IconButton>
                      </Tooltip>
                    </Stack>
                    <Typography
                      variant="body2"
                      sx={{
                        fontFamily: "monospace",
                        wordBreak: "break-all",
                        fontSize: "0.85rem",
                        color: "#a7f3d0"
                      }}
                    >
                      {exportedPackage.manifest.sha256Digest}
                    </Typography>
                  </Box>

                  <Stack direction={{ xs: "column", sm: "row" }} spacing={2} fontSize="0.8rem" color="text.secondary">
                    <Box><strong>Records:</strong> {exportedPackage.manifest.recordCount}</Box>
                    <Box><strong>Filename:</strong> {exportedPackage.filename}</Box>
                    <Box><strong>Standard:</strong> FDA 21 CFR Part 11 / EU GDP</Box>
                  </Stack>

                  <Box>
                    <Typography variant="caption" fontWeight={700} color="text.secondary" display="block" mb={0.5}>
                      Package Payload Preview
                    </Typography>
                    <Box
                      component="pre"
                      sx={{
                        maxHeight: 180,
                        overflow: "auto",
                        p: 1.5,
                        bgcolor: "#f1f5f9",
                        borderRadius: 1,
                        fontSize: "0.75rem",
                        fontFamily: "monospace",
                        m: 0,
                        border: "1px solid #e2e8f0"
                      }}
                    >
                      {exportedPackage.rawPayload.substring(0, 1000)}
                      {exportedPackage.rawPayload.length > 1000 ? "\n... (truncated for preview)" : ""}
                    </Box>
                  </Box>
                </Stack>
              </Paper>
            )}
          </Stack>
        )}

        {/* Tab 1: Tamper Verifier */}
        {activeTab === 1 && (
          <Stack spacing={2.5}>
            <Alert severity="info" sx={{ "& .MuiAlert-message": { fontSize: "0.875rem" } }}>
              Cryptographically verify that an exported compliance report or data extract matches the immutable SHA-256 manifest recorded in the database.
            </Alert>

            <TextField
              label="Manifest ID (UUID)"
              fullWidth
              size="small"
              value={verifyManifestId}
              onChange={(e) => setVerifyManifestId(e.target.value)}
              placeholder="e.g. 00000000-0000-4000-8900-000000000001"
              helperText="The unique manifest identifier assigned at export generation"
            />

            <TextField
              label="Payload Content to Verify"
              fullWidth
              multiline
              rows={6}
              value={verifyPayload}
              onChange={(e) => setVerifyPayload(e.target.value)}
              placeholder="Paste raw JSON-LD, CSV, or HTML content here..."
              InputProps={{
                sx: { fontFamily: "monospace", fontSize: "0.8rem" }
              }}
            />

            <Button
              variant="contained"
              size="large"
              fullWidth
              startIcon={
                verifyExport.isPending ? (
                  <CircularProgress size={20} color="inherit" />
                ) : (
                  <FactCheckRounded />
                )
              }
              onClick={() => verifyExport.mutate()}
              disabled={verifyExport.isPending || !verifyManifestId.trim() || !verifyPayload.trim()}
              sx={{
                py: 1.2,
                bgcolor: "#0f766e",
                "&:hover": { bgcolor: "#0d9488" },
                fontWeight: 700
              }}
            >
              {verifyExport.isPending ? "Computing Cryptographic Digest…" : "Verify Cryptographic Integrity"}
            </Button>

            {verifyExport.isError && (
              <Alert severity="error">
                {verifyExport.error instanceof Error
                  ? verifyExport.error.message
                  : "Verification request failed"}
              </Alert>
            )}

            {verificationResult && (
              <Paper
                variant="outlined"
                sx={{
                  p: 2.5,
                  borderRadius: 2,
                  borderColor: verificationResult.valid ? "#86efac" : "#fca5a5",
                  bgcolor: verificationResult.valid ? "#f0fdf4" : "#fef2f2"
                }}
              >
                <Stack spacing={1.5}>
                  <Stack direction="row" spacing={1.5} alignItems="center">
                    {verificationResult.valid ? (
                      <CheckCircleRounded sx={{ color: "#16a34a", fontSize: 28 }} />
                    ) : (
                      <WarningAmberRounded sx={{ color: "#dc2626", fontSize: 28 }} />
                    )}
                    <Box>
                      <Typography
                        variant="subtitle1"
                        fontWeight={800}
                        color={verificationResult.valid ? "#15803d" : "#b91c1c"}
                      >
                        {verificationResult.valid
                          ? "INTEGRITY VERIFIED: NO TAMPERING DETECTED"
                          : "TAMPER DETECTED: DIGEST MISMATCH"}
                      </Typography>
                      <Typography variant="body2" color="text.secondary">
                        {verificationResult.message}
                      </Typography>
                    </Box>
                  </Stack>

                  <Box sx={{ mt: 1, p: 1.5, bgcolor: "#ffffff", borderRadius: 1.5, border: "1px solid #e2e8f0" }}>
                    <Typography variant="caption" display="block" color="text.secondary" fontWeight={700}>
                      Expected Ledger Digest:
                    </Typography>
                    <Typography variant="body2" sx={{ fontFamily: "monospace", wordBreak: "break-all", color: "#334155" }}>
                      {verificationResult.expectedDigest}
                    </Typography>

                    <Typography variant="caption" display="block" color="text.secondary" fontWeight={700} sx={{ mt: 1 }}>
                      Computed Payload Digest:
                    </Typography>
                    <Typography
                      variant="body2"
                      sx={{
                        fontFamily: "monospace",
                        wordBreak: "break-all",
                        color: verificationResult.valid ? "#16a34a" : "#dc2626",
                        fontWeight: 700
                      }}
                    >
                      {verificationResult.computedDigest}
                    </Typography>
                  </Box>
                </Stack>
              </Paper>
            )}
          </Stack>
        )}

        {/* Tab 2: Manifest Ledger */}
        {activeTab === 2 && (
          <Stack spacing={2}>
            <Stack direction="row" justifyContent="space-between" alignItems="center">
              <Typography variant="body2" color="text.secondary">
                Audited evidence exports registered for company under FDA 21 CFR Part 11 / EU GDP.
              </Typography>
              <Button
                size="small"
                variant="outlined"
                startIcon={<RefreshRounded />}
                onClick={() => manifestsQuery.refetch()}
                disabled={manifestsQuery.isFetching}
              >
                Refresh
              </Button>
            </Stack>

            {manifestsQuery.isPending && (
              <Stack alignItems="center" py={4}>
                <CircularProgress size={32} />
              </Stack>
            )}

            {manifestsQuery.isError && (
              <Alert severity="error">
                {manifestsQuery.error instanceof Error
                  ? manifestsQuery.error.message
                  : "Failed to load manifests"}
              </Alert>
            )}

            {manifestsQuery.data && (
              <TableContainer component={Paper} variant="outlined" sx={{ borderRadius: 2, maxHeight: 380 }}>
                <Table size="small" stickyHeader>
                  <TableHead>
                    <TableRow>
                      <TableCell sx={{ fontWeight: 700 }}>Exported At</TableCell>
                      <TableCell sx={{ fontWeight: 700 }}>Scope</TableCell>
                      <TableCell sx={{ fontWeight: 700 }}>Format</TableCell>
                      <TableCell sx={{ fontWeight: 700 }}>Records</TableCell>
                      <TableCell sx={{ fontWeight: 700 }}>SHA-256 Digest</TableCell>
                      <TableCell align="right" sx={{ fontWeight: 700 }}>Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {manifestsQuery.data.length === 0 ? (
                      <TableRow>
                        <TableCell colSpan={6} align="center" sx={{ py: 3, color: "text.secondary" }}>
                          No compliance manifests generated yet.
                        </TableCell>
                      </TableRow>
                    ) : (
                      manifestsQuery.data.map((m) => (
                        <TableRow key={m.id} hover>
                          <TableCell sx={{ whiteSpace: "nowrap", fontSize: "0.8rem" }}>
                            {new Date(m.createdAt).toLocaleString()}
                          </TableCell>
                          <TableCell>
                            <Chip
                              label={m.exportType.replace(/_/g, " ")}
                              size="small"
                              sx={{ fontSize: "0.7rem", fontWeight: 600 }}
                            />
                          </TableCell>
                          <TableCell>
                            <Chip
                              label={m.format}
                              size="small"
                              variant="outlined"
                              sx={{ fontSize: "0.7rem", fontWeight: 600 }}
                            />
                          </TableCell>
                          <TableCell sx={{ fontSize: "0.8rem" }}>{m.recordCount}</TableCell>
                          <TableCell sx={{ fontFamily: "monospace", fontSize: "0.75rem", color: "#475569" }}>
                            {m.sha256Digest.substring(0, 12)}…{m.sha256Digest.substring(m.sha256Digest.length - 6)}
                          </TableCell>
                          <TableCell align="right">
                            <Stack direction="row" spacing={0.5} justifyContent="flex-end">
                              <Tooltip title="Copy Digest">
                                <IconButton size="small" onClick={() => handleCopyDigest(m.sha256Digest)}>
                                  <ContentCopyRounded sx={{ fontSize: 16 }} />
                                </IconButton>
                              </Tooltip>
                              <Button
                                size="small"
                                variant="text"
                                onClick={() => loadManifestIntoVerifier(m.id, m.sha256Digest)}
                                sx={{ fontSize: "0.75rem", minWidth: "auto", px: 1 }}
                              >
                                Verify
                              </Button>
                            </Stack>
                          </TableCell>
                        </TableRow>
                      ))
                    )}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </Stack>
        )}
      </DialogContent>
    </Dialog>
  );
}
