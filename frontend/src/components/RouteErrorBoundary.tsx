import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import RefreshRounded from "@mui/icons-material/RefreshRounded";
import HomeRounded from "@mui/icons-material/HomeRounded";
import WarningAmberRounded from "@mui/icons-material/WarningAmberRounded";
import { isRouteErrorResponse, useRouteError } from "react-router-dom";

export function RouteErrorBoundary() {
  const error = useRouteError();
  const isChunkError =
    error instanceof Error &&
    (error.message.includes("dynamically imported module") ||
      error.message.includes("Failed to fetch") ||
      error.name === "TypeError");

  const title = isRouteErrorResponse(error)
    ? `${error.status} ${error.statusText}`
    : isChunkError
    ? "Application Update Available"
    : "Unexpected Workspace Error";

  const description = isChunkError
    ? "A new version of the NEEPO-CARE frontend has been deployed. Refresh your browser to load the latest components."
    : error instanceof Error
    ? error.message
    : "An unexpected error occurred while rendering this workspace route.";

  return (
    <Box
      display="flex"
      alignItems="center"
      justifyContent="center"
      minHeight="60vh"
      p={3}
    >
      <Card
        sx={{
          maxWidth: 540,
          width: "100%",
          textAlign: "center",
          p: 2,
          boxShadow: (theme) => theme.shadows[4]
        }}
      >
        <CardContent>
          <Stack spacing={2} alignItems="center">
            <Box
              sx={{
                width: 56,
                height: 56,
                borderRadius: "50%",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                bgcolor: "warning.light",
                color: "warning.dark"
              }}
            >
              <WarningAmberRounded sx={{ fontSize: 32 }} />
            </Box>

            <Typography variant="h5" fontWeight={700}>
              {title}
            </Typography>

            <Typography variant="body2" color="text.secondary" sx={{ maxWidth: 440 }}>
              {description}
            </Typography>

            <Stack direction="row" spacing={2} pt={2}>
              <Button
                variant="contained"
                startIcon={<RefreshRounded />}
                onClick={() => window.location.reload()}
              >
                Refresh Application
              </Button>
              <Button
                variant="outlined"
                startIcon={<HomeRounded />}
                onClick={() => {
                  window.location.href = "/";
                }}
              >
                Go to Dashboard
              </Button>
            </Stack>
          </Stack>
        </CardContent>
      </Card>
    </Box>
  );
}
