import LockRounded from "@mui/icons-material/LockRounded";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import Container from "@mui/material/Container";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { Link } from "react-router-dom";
import { useAuthStore } from "../auth/authStore";

export function ForbiddenPage() {
  const session = useAuthStore((state) => state.session);

  return (
    <Container maxWidth="sm">
      <Box minHeight="60vh" display="flex" alignItems="center" justifyContent="center">
        <Card variant="outlined" sx={{ borderRadius: 3, textAlign: "center", p: 2 }}>
          <CardContent sx={{ p: { xs: 3, sm: 4.5 } }}>
            <Box
              sx={{
                width: 64,
                height: 64,
                borderRadius: "50%",
                bgcolor: "rgba(239, 68, 68, 0.1)",
                color: "error.main",
                display: "grid",
                placeItems: "center",
                mx: "auto",
                mb: 2
              }}
            >
              <LockRounded sx={{ fontSize: 36 }} />
            </Box>

            <Typography variant="h5" fontWeight={800} gutterBottom>
              403 — Access Forbidden
            </Typography>

            <Typography variant="body2" color="text.secondary" mb={3}>
              Your authenticated security role (<strong>{session?.user.role ?? "UNAUTHORIZED"}</strong>) does not have permission to access this administrative resource.
            </Typography>

            <Stack direction="row" spacing={2} justifyContent="center">
              <Button component={Link} to="/" variant="contained" sx={{ fontWeight: 700 }}>
                Return to Dashboard
              </Button>
            </Stack>
          </CardContent>
        </Card>
      </Box>
    </Container>
  );
}
