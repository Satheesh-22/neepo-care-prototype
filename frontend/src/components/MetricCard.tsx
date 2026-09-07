import Box from "@mui/material/Box";
import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import type { ReactNode } from "react";

export interface MetricCardProps {
  label: string;
  value: string | number;
  detail: string;
  icon: ReactNode;
  iconBg?: string;
  statusColor?: string;
  sparklineData?: number[];
  onClick?: () => void;
}

export function MetricCard({
  label,
  value,
  detail,
  icon,
  iconBg = "rgba(13, 91, 99, 0.08)",
  statusColor = "#0d5b63",
  sparklineData,
  onClick
}: MetricCardProps) {
  return (
    <Card
      className="hover-lift"
      onClick={onClick}
      sx={{
        flex: 1,
        minWidth: { xs: "100%", sm: 220 },
        cursor: onClick ? "pointer" : "default",
        border: "1px solid #e2e8f0",
        bgcolor: "#ffffff"
      }}
    >
      <CardContent sx={{ p: 2.5, "&:last-child": { pb: 2.5 } }}>
        <Stack direction="row" justifyContent="space-between" alignItems="flex-start" spacing={1.5}>
          <Box flex={1}>
            <Typography
              variant="caption"
              color="text.secondary"
              fontWeight={700}
              textTransform="uppercase"
              letterSpacing="0.05em"
            >
              {label}
            </Typography>

            <Typography
              variant="h2"
              sx={{
                mt: 0.5,
                fontWeight: 800,
                color: "#0f172a",
                lineHeight: 1.15
              }}
            >
              {value}
            </Typography>

            <Typography variant="body2" color="text.secondary" mt={0.5} fontSize="0.82rem">
              {detail}
            </Typography>
          </Box>

          <Box
            sx={{
              width: 44,
              height: 44,
              borderRadius: 2.5,
              bgcolor: iconBg,
              display: "grid",
              placeItems: "center",
              flexShrink: 0
            }}
          >
            {icon}
          </Box>
        </Stack>

        {sparklineData && sparklineData.length > 1 && (
          <Box sx={{ mt: 2, height: 26, width: "100%" }} aria-hidden="true">
            <svg width="100%" height="26" viewBox="0 0 100 26" preserveAspectRatio="none">
              <path
                d={generateSparklinePath(sparklineData, 100, 26)}
                fill="none"
                stroke={statusColor}
                strokeWidth="2.2"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
          </Box>
        )}
      </CardContent>
    </Card>
  );
}

function generateSparklinePath(data: number[], width: number, height: number): string {
  if (!data.length) return "";
  const min = Math.min(...data);
  const max = Math.max(...data);
  const range = max - min || 1;
  const padding = 3;
  const usableHeight = height - padding * 2;

  const points = data.map((val, idx) => {
    const x = (idx / (data.length - 1)) * width;
    const y = height - padding - ((val - min) / range) * usableHeight;
    return `${x.toFixed(1)},${y.toFixed(1)}`;
  });

  return `M ${points.join(" L ")}`;
}
