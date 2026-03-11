export const parsePostStringArray = (rawValue: unknown): string[] => {
  if (!rawValue) {
    return [];
  }
  if (Array.isArray(rawValue)) {
    return rawValue.map((item) => String(item || "").trim()).filter(Boolean);
  }
  if (typeof rawValue === "string") {
    const value = rawValue.trim();
    if (!value) {
      return [];
    }
    if (value.startsWith("[")) {
      try {
        const parsed = JSON.parse(value);
        if (Array.isArray(parsed)) {
          return parsed
            .map((item) => String(item || "").trim())
            .filter(Boolean);
        }
      } catch (error) {
        console.warn("帖子字段 JSON 解析失败", error);
      }
    }
    return [value];
  }
  return [];
};

export const normalizePost = <T extends Record<string, any>>(
  rawPost: T
): T & {
  tags: string[];
  images: string[];
} => {
  return {
    ...rawPost,
    tags: parsePostStringArray(rawPost?.tags ?? rawPost?.tagList),
    images: parsePostStringArray(
      rawPost?.images ??
        rawPost?.imageUrls ??
        rawPost?.picture ??
        rawPost?.cover
    ),
  };
};
