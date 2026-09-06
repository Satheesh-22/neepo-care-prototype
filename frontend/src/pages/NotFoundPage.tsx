import Button from "@mui/material/Button";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { Link } from "react-router-dom";

export function NotFoundPage() {
  return <Stack minHeight="100vh" alignItems="center" justifyContent="center" spacing={2}><Typography variant="h1">Route not found</Typography><Typography color="text.secondary">The requested operations view does not exist.</Typography><Button component={Link} to="/" variant="contained">Return to overview</Button></Stack>;
}
