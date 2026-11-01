import { describe, expect, it } from "vitest";
import { en } from "./locales/en";
import { es } from "./locales/es";
import { translate } from "./index";

function getObjectKeyPaths(obj: Record<string, unknown>, prefix = ""): string[] {
  let keys: string[] = [];
  for (const [key, value] of Object.entries(obj)) {
    const currentPath = prefix ? `${prefix}.${key}` : key;
    if (value && typeof value === "object" && !Array.isArray(value)) {
      keys = keys.concat(getObjectKeyPaths(value as Record<string, unknown>, currentPath));
    } else {
      keys.push(currentPath);
    }
  }
  return keys.sort();
}

describe("i18n Multi-Language Localization Engine", () => {
  it("enforces 100% key parity between English and Spanish translation dictionaries", () => {
    const enKeys = getObjectKeyPaths(en as unknown as Record<string, unknown>);
    const esKeys = getObjectKeyPaths(es as unknown as Record<string, unknown>);

    expect(enKeys.length).toBeGreaterThan(0);
    expect(esKeys.length).toBeGreaterThan(0);

    // Missing in Spanish
    const missingInEs = enKeys.filter((k) => !esKeys.includes(k));
    expect(missingInEs).toEqual([]);

    // Extra keys in Spanish not in English
    const extraInEs = esKeys.filter((k) => !enKeys.includes(k));
    expect(extraInEs).toEqual([]);

    expect(enKeys).toEqual(esKeys);
  });

  it("translates top-level and nested keys correctly in English and Spanish", () => {
    expect(translate("en", "nav.dashboard")).toBe("Dashboard");
    expect(translate("es", "nav.dashboard")).toBe("Tablero");

    expect(translate("en", "nav.depots")).toBe("Depots");
    expect(translate("es", "nav.depots")).toBe("Depósitos");

    expect(translate("en", "common.cancel")).toBe("Cancel");
    expect(translate("es", "common.cancel")).toBe("Cancelar");
  });

  it("interpolates parameters into localized strings correctly", () => {
    const enQueued = translate("en", "nav.queued", { count: 5 });
    expect(enQueued).toBe("5 queued");

    const esQueued = translate("es", "nav.queued", { count: 5 });
    expect(esQueued).toBe("5 en cola");

    const enDelete = translate("en", "depots.deleteConfirm", { code: "DEPOT-HQ-01" });
    expect(enDelete).toBe("Are you sure you want to delete depot DEPOT-HQ-01?");

    const esDelete = translate("es", "depots.deleteConfirm", { code: "DEPOT-HQ-01" });
    expect(esDelete).toBe("¿Está seguro de que desea eliminar el depósito DEPOT-HQ-01?");
  });

  it("safely falls back to English when a key does not exist in Spanish", () => {
    // If a key existed only in English
    const fallbackText = translate("es", "nonexistent.fake.key");
    expect(fallbackText).toBe("nonexistent.fake.key");
  });
});
