import { expect, test } from "@playwright/test";

test.describe("Onboarding and Administration Workflows", () => {
  test("1. Administrator manages company profile", async ({ page }) => {
    await page.goto("/login");
    await page.getByLabel("Demo role").click();
    await page.getByRole("option", { name: "Administrator" }).click();
    await page.getByRole("button", { name: "Sign in" }).click();

    await page.getByRole("link", { name: "Company" }).click();
    await expect(page.getByRole("heading", { name: "Company Profile" })).toBeVisible();
    await expect(page.getByLabel("Legal Name *")).toHaveValue(/Neepo Cold Chain Logistics/i);
    await page.getByRole("button", { name: "Save Company Profile" }).click();
    await expect(page.getByText("Company profile updated successfully.")).toBeVisible();
  });

  test("2. Administrator invites new team member", async ({ page }) => {
    await page.goto("/login");
    await page.getByLabel("Demo role").click();
    await page.getByRole("option", { name: "Administrator" }).click();
    await page.getByRole("button", { name: "Sign in" }).click();

    await page.getByRole("link", { name: "Users" }).click();
    await expect(page.getByRole("heading", { name: "User Administration" })).toBeVisible();
    await page.getByRole("button", { name: "Invite User" }).click();
    await expect(page.getByText("Invite Organization Member")).toBeVisible();

    await page.getByLabel("Full Name *").fill("Jane Dispatcher");
    await page.getByLabel("Business Email *").fill("jane.disp@example.com");
    await page.getByLabel("Phone Number *").fill("+1-555-0199");
    await page.getByRole("button", { name: "Generate Invitation" }).click();
    await expect(page.getByRole("heading", { name: "Invitation Link Generated" })).toBeVisible();
  });

  test("3. Administrator onboard new driver", async ({ page }) => {
    await page.goto("/login");
    await page.getByLabel("Demo role").click();
    await page.getByRole("option", { name: "Administrator" }).click();
    await page.getByRole("button", { name: "Sign in" }).click();

    await page.getByRole("link", { name: "Drivers" }).click();
    await expect(page.getByRole("heading", { name: "Driver Directory" })).toBeVisible();
    await page.getByRole("link", { name: "Onboard Driver" }).click();

    await expect(page.getByRole("heading", { name: "Onboard Commercial Driver" })).toBeVisible();
    await page.getByLabel("First Name *").fill("Robert");
    await page.getByLabel("Last Name *").fill("Miller");
    await page.getByLabel("Work Email *").fill("robert.miller@example.com");
    await page.getByLabel("Mobile Phone *").fill("+1-555-0188");
    await page.getByLabel("Employee ID *").fill("EMP-DRV-999");
    await page.getByLabel("Commercial License Number *").fill("DL-CA-99281");
    await page.getByLabel("License Expiry Date *").fill("2028-12-31");
    await page.getByLabel("Emergency Contact Name").fill("Sarah Miller");
    await page.getByLabel("Emergency Contact Phone").fill("+1-555-0177");

    await page.getByRole("button", { name: "Register Driver" }).click();
    await expect(page.getByText(/Driver Successfully Registered!/i)).toBeVisible();
  });

  test("4. Administrator registers new refrigerated fleet vehicle", async ({ page }) => {
    await page.goto("/login");
    await page.getByLabel("Demo role").click();
    await page.getByRole("option", { name: "Administrator" }).click();
    await page.getByRole("button", { name: "Sign in" }).click();

    await page.getByRole("link", { name: "Vehicles" }).click();
    await expect(page.getByRole("heading", { name: "Fleet & Vehicles" })).toBeVisible();
    await page.getByRole("link", { name: "Register Vehicle" }).click();

    await expect(page.getByRole("heading", { name: "Register Fleet Vehicle" })).toBeVisible();
    await page.getByLabel("Registration Number *").fill("CA-NEEPO-77");
    await page.getByLabel("VIN (Vehicle Identification Number)").fill("1HGCR2F83HA007788");
    await page.getByLabel("Make").fill("Volvo");
    await page.getByLabel("Model").fill("FH-16 Heavy Reefer");
    await page.getByLabel("Payload Capacity (kg) *").fill("12000");

    await page.getByRole("button", { name: "Register Vehicle" }).click();
    await expect(page.getByText(/Vehicle Successfully Registered!/i)).toBeVisible();
  });

  test("5. Dispatcher assigns vehicle to driver", async ({ page }) => {
    await page.goto("/login");
    await page.getByLabel("Demo role").click();
    await page.getByRole("option", { name: "Dispatcher" }).click();
    await page.getByRole("button", { name: "Sign in" }).click();

    await page.getByRole("link", { name: "Drivers" }).click();
    await expect(page.getByRole("heading", { name: "Driver Directory" })).toBeVisible();

    await page.goto("/admin/assignments");
    await expect(page.getByText(/Driver-to-Vehicle Allocations/i)).toBeVisible();
    await page.getByRole("button", { name: "New Assignment" }).click();
    await expect(page.getByRole("heading", { name: "Allocate Driver to Vehicle" })).toBeVisible();
  });

  test("6. Driver accesses driver profile", async ({ page }) => {
    await page.goto("/login");
    await page.getByLabel("Demo role").click();
    await page.getByRole("option", { name: "Driver" }).click();
    await page.getByRole("button", { name: "Sign in" }).click();

    await page.getByRole("link", { name: "Profile" }).click();
    await expect(page.getByText("Driver Operational Profile")).toBeVisible();
    await expect(page.getByText("Driving Time Budget")).toBeVisible();
  });

  test("7. Unauthorized access is blocked by role guard", async ({ page }) => {
    await page.goto("/login");
    await page.getByLabel("Demo role").click();
    await page.getByRole("option", { name: "Driver" }).click();
    await page.getByRole("button", { name: "Sign in" }).click();
    await expect(page.getByText("Priority trip")).toBeVisible();

    await page.goto("/forbidden");
    await expect(page.getByText("403 — Access Forbidden")).toBeVisible();
  });

  test("8. Driver accepts invitation portal page", async ({ page }) => {
    await page.goto("/invite/accept?token=demo-test-token");
    await expect(page.getByRole("heading", { name: "Complete Account Setup" })).toBeVisible();
    await expect(page.getByLabel("New Password *")).toBeVisible();
    await expect(page.getByLabel("Confirm Password *")).toBeVisible();
  });
});
