import { usePreferencesStore, type Locale } from "../accessibility/preferencesStore";
import { en } from "./locales/en";
import { es } from "./locales/es";
import type { TranslationDictionary } from "./types";

const dictionaries: Record<Locale, TranslationDictionary> = {
  en,
  es
};

function getNestedValue(obj: unknown, path: string): string | undefined {
  const parts = path.split(".");
  let current: unknown = obj;
  for (const part of parts) {
    if (current && typeof current === "object" && part in current) {
      current = (current as Record<string, unknown>)[part];
    } else {
      return undefined;
    }
  }
  return typeof current === "string" ? current : undefined;
}

export function translate(
  locale: Locale,
  key: string,
  params?: Record<string, string | number>
): string {
  const currentDict = dictionaries[locale] || dictionaries.en;
  let text = getNestedValue(currentDict, key);

  // Fallback to English if not found
  if (text === undefined && locale !== "en") {
    text = getNestedValue(dictionaries.en, key);
  }

  // Fallback to the key itself if still not found
  if (text === undefined) {
    return key;
  }

  // Interpolate parameters: {paramName}
  if (params) {
    for (const [paramKey, paramValue] of Object.entries(params)) {
      text = text.replaceAll(`{${paramKey}}`, String(paramValue));
    }
  }

  return text;
}

export function useTranslation() {
  const locale = usePreferencesStore((s) => s.locale);
  const setLocale = usePreferencesStore((s) => s.setLocale);

  const t = (key: string, params?: Record<string, string | number>): string => {
    return translate(locale, key, params);
  };

  return {
    t,
    locale,
    setLocale,
    isSpanish: locale === "es",
    isEnglish: locale === "en"
  };
}

export * from "./types";
