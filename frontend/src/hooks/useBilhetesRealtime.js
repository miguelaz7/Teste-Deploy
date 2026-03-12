import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { fetchBilhetes, subscribeBilhetesUpdates } from "../services/bilhetesService";

export default function useBilhetesRealtime(filters = {}) {
  const [bilhetes, setBilhetes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [isLive, setIsLive] = useState(false);
  const [lastUpdatedAt, setLastUpdatedAt] = useState("");
  const reconnectTimerRef = useRef(null);

  // Sempre aponta para os filtros mais recentes sem precisar de deps no useEffect do SSE
  const filtersRef = useRef(filters);
  filtersRef.current = filters;

  // loadBilhetes é estável (sem deps) — recebe filtros como argumento
  const loadBilhetes = useCallback(async (filtersToUse) => {
    try {
      const data = await fetchBilhetes(filtersToUse);
      const safeData = Array.isArray(data) ? data : [];
      setBilhetes(safeData);
      setLastUpdatedAt(new Date().toISOString());
      setError("");
    } catch (exception) {
      setError("Nao foi possivel carregar os bilhetes do backend.");
    } finally {
      setLoading(false);
    }
  }, []);

  // Recarrega dados sempre que os filtros mudam
  useEffect(() => {
    setLoading(true);
    loadBilhetes(filters);
  }, [loadBilhetes]);

  // Ligação SSE estável — não reinicia quando os filtros mudam
  useEffect(() => {
    let unmounted = false;
    let cleanupSubscription = () => {};
    const reconnectDelayMs = 2500;

    const connect = () => {
      cleanupSubscription = subscribeBilhetesUpdates({
        onConnected: () => {
          if (!unmounted) {
            setIsLive(true);
          }
        },
        onUpdate: () => {
          if (!unmounted) {
            loadBilhetes(filtersRef.current);
          }
        },
        onError: () => {
          if (unmounted) {
            return;
          }
          setIsLive(false);
          cleanupSubscription();
          reconnectTimerRef.current = window.setTimeout(connect, reconnectDelayMs);
        },
      });
    };

    connect();

    return () => {
      unmounted = true;
      cleanupSubscription();
      if (reconnectTimerRef.current) {
        window.clearTimeout(reconnectTimerRef.current);
      }
    };
  }, [loadBilhetes]); // loadBilhetes é estável — este effect só corre uma vez

  const totalStock = useMemo(
    () => bilhetes.reduce((sum, item) => sum + (Number(item.quantidade) || 0), 0),
    [bilhetes]
  );

  return {
    bilhetes,
    loading,
    error,
    isLive,
    totalStock,
    lastUpdatedAt,
    refresh: () => loadBilhetes(filtersRef.current),
  };
}
