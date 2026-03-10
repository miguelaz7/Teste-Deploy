import { useEffect, useMemo, useState } from "react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import {
  fetchAnomalies,
  fetchRevenueByRoute,
  fetchTicketTypes,
  fetchTripsByDay,
  importTicketingFixedFile,
} from "../services/ticketingService";

const PIE_COLORS = ["#1d6588", "#6bb14f", "#ef7d00", "#cf222e", "#7a5af8", "#00a3a3"];

function TicketingDashboardPage({ onBack }) {
  const [revenueData, setRevenueData] = useState([]);
  const [tripsData, setTripsData] = useState([]);
  const [ticketTypeData, setTicketTypeData] = useState([]);
  const [anomalies, setAnomalies] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");

  const hasCharts = revenueData.length > 0 || tripsData.length > 0 || ticketTypeData.length > 0;

  const pieData = useMemo(
    () => ticketTypeData.map((row) => ({ name: row.ticketType, value: row.count })),
    [ticketTypeData]
  );

  const loadAnalytics = async () => {
    // Keep all widgets in sync by refreshing all analytics endpoints together.
    const [revenueRes, tripsRes, ticketTypeRes, anomaliesRes] = await Promise.all([
      fetchRevenueByRoute(),
      fetchTripsByDay(),
      fetchTicketTypes(),
      fetchAnomalies(),
    ]);

    if (!revenueRes.ok || !tripsRes.ok || !ticketTypeRes.ok || !anomaliesRes.ok) {
      throw new Error("Erro ao carregar analises de bilhetica.");
    }

    setRevenueData(revenueRes.data);
    setTripsData(tripsRes.data);
    setTicketTypeData(ticketTypeRes.data);
    setAnomalies(anomaliesRes.data);
  };

  useEffect(() => {
    async function loadAllOnOpen() {
      setIsLoading(true);
      setError("");

      try {
        const importRes = await importTicketingFixedFile();
        if (!importRes.ok) {
          throw new Error("Falha ao importar o ficheiro configurado no backend.");
        }

        await loadAnalytics();
      } catch (requestError) {
        setError(requestError.message || "Erro inesperado durante importacao.");
      } finally {
        setIsLoading(false);
      }
    }

    loadAllOnOpen();
  }, []);

  return (
    <div className="dashboard-shell">
      <div className="dashboard-card">
        <div className="team-header">
          <div>
            <p className="eyebrow">TUBInsight</p>
            <h1>Analise de Bilhetica</h1>
            <p className="subtitle">Visualizacao automatica de estatisticas.</p>
          </div>
          <button type="button" className="back-button" onClick={onBack}>
            Voltar ao dashboard
          </button>
        </div>

        {isLoading && <p className="message">A carregar analise...</p>}
        {error && <p className="message">{error}</p>}

        {hasCharts && (
          <div className="charts-grid">
            <article className="chart-card">
              <h3>Receita por Rota</h3>
              <ResponsiveContainer width="100%" height={260}>
                <BarChart data={revenueData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="route" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Bar dataKey="totalRevenue" fill="#1d6588" name="Receita" />
                </BarChart>
              </ResponsiveContainer>
            </article>

            <article className="chart-card">
              <h3>Viagens por Dia</h3>
              <ResponsiveContainer width="100%" height={260}>
                <BarChart data={tripsData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="day" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Bar dataKey="trips" fill="#6bb14f" name="Viagens" />
                </BarChart>
              </ResponsiveContainer>
            </article>

            <article className="chart-card">
              <h3>Distribuicao de Tipos de Bilhete</h3>
              <ResponsiveContainer width="100%" height={260}>
                <PieChart>
                  <Tooltip />
                  <Legend />
                  <Pie data={pieData} dataKey="value" nameKey="name" outerRadius={90}>
                    {pieData.map((entry, index) => (
                      <Cell key={`slice-${entry.name}`} fill={PIE_COLORS[index % PIE_COLORS.length]} />
                    ))}
                  </Pie>
                </PieChart>
              </ResponsiveContainer>
            </article>

            <article className="chart-card">
              <h3>Anomalias Simples</h3>
              {anomalies.length === 0 ? (
                <p className="message">Sem anomalias detetadas.</p>
              ) : (
                <div className="anomaly-list">
                  {anomalies.map((item) => (
                    <p key={`${item.viagemId}-${item.dateTime}`}>
                      <strong>{item.viagemId}</strong> ({item.route}) - {item.reason} - valor {item.paidValue}
                    </p>
                  ))}
                </div>
              )}
            </article>
          </div>
        )}
      </div>
    </div>
  );
}

export default TicketingDashboardPage;
