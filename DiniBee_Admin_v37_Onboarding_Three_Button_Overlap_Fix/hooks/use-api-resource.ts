"use client";

import { useEffect, useEffectEvent, useState } from "react";

export function useApiResource<T>(
  loader: () => Promise<T>,
  dependencies: readonly unknown[],
  initialValue: T,
) {
  const [data, setData] = useState<T>(initialValue);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const dependencyKey = JSON.stringify(dependencies);

  function errorMessage(caught: unknown) {
    return caught instanceof Error
      ? caught.message
      : typeof caught === "object" && caught && "message" in caught
        ? String(caught.message)
        : "Unable to load this page.";
  }

  async function reload() {
    try {
      setLoading(true);
      setError("");
      setData(await loader());
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setLoading(false);
    }
  }

  const loadForEffect = useEffectEvent(loader);

  useEffect(() => {
    let active = true;
    void Promise.resolve().then(async () => {
      if (!active) return;
      setLoading(true);
      setError("");
      try {
        const nextData = await loadForEffect();
        if (active) setData(nextData);
      } catch (caught) {
        if (active) setError(errorMessage(caught));
      } finally {
        if (active) setLoading(false);
      }
    });
    return () => { active = false; };
  }, [dependencyKey]);

  return { data, setData, loading, error, reload };
}
