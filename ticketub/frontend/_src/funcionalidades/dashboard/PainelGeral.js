import React, { useEffect, useRef, useState } from 'react';
import { getDashboardMetrics } from '../../logica_do_sistema/services/metricsService';
import './PainelGeral.css';

const IconHour = () => <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" style={{color: '#3b82f6'}}><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>;
const IconDay = () => <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" style={{color: '#10b981'}}><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>;
const IconWeek = () => <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" style={{color: '#f59e0b'}}><path d="M21 10.5V6a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h7"/><path d="M16 2v4"/><path d="M8 2v4"/><path d="M3 10h18"/><path d="M16 20l2 2 4-4"/></svg>;

const MetricCard = ({ title, data, type }) => {
  if (!data) return null;

  const validPercent = data.volumeIngestao > 0 ? (data.validas / data.volumeIngestao) * 100 : 0;
  const quarantinePercent = data.volumeIngestao > 0 ? (data.quarentena / data.volumeIngestao) * 100 : 0;

  const getStatus = () => {
    if (data.taxaValidas > 80) return { label: 'Bom', class: 'status-good' };
    if (data.taxaValidas >= 60) return { label: 'Atenção', class: 'status-attention' };
    return { label: 'Crítico', class: 'status-critical' };
  };

  const status = getStatus();

  const getIcon = () => {
    if (type === 'hora') return <IconHour />;
    if (type === 'dia') return <IconDay />;
    return <IconWeek />;
  };

  return (
    <div className={`metric-card border-top-${type}`}>
      <div className="metric-header">
        <div className="header-left">
          {getIcon()}
          <span>{title}</span>
        </div>
        <span className={`status-badge ${status.class}`}>{status.label}</span>
      </div>
      <div className="metric-body">
        <div className="metric-main-val">
          <span className="main-label">Volume Ingestão</span>
          <span className="main-value">{data.volumeIngestao}</span>
        </div>
        <div className="metric-row">
          <span className="metric-label">Válidas</span>
          <span className="metric-value color-validas">
            {data.validas} <small>({data.taxaValidas}%)</small>
          </span>
        </div>
        <div className="metric-row">
          <span className="metric-label">Quarentena</span>
          <span className="metric-value color-quarentena">
            {data.quarentena} <small>({data.taxaQuarentena}%)</small>
          </span>
        </div>
        <div className="metric-row">
          <span className="metric-label">Duplicados</span>
          <span className="metric-value color-duplicados">{data.duplicados}</span>
        </div>
      </div>
      
      {/* Progress Bar Container */}
      <div className="metric-progress-container">
        <div className="progress-bar-bg">
          <div className="progress-bar-valid" style={{ width: `${validPercent}%` }}></div>
          <div className="progress-bar-quarantine" style={{ width: `${quarantinePercent}%`, left: `${validPercent}%` }}></div>
        </div>
      </div>
    </div>
  );
};

function PainelGeral() {
  const [metrics, setMetrics] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const metricsRef = useRef(null);

  useEffect(() => {
    metricsRef.current = metrics;
  }, [metrics]);

  const carregarMetricas = async (silencioso = false) => {
    if (!silencioso) {
      setLoading(true);
      setError(null);
    }
    try {
      const data = await getDashboardMetrics();
      setMetrics(data);
    } catch (err) {
      if (!metricsRef.current) {
        setError(err.message || 'Ocorreu um erro ao comunicar com a API.');
      } else {
        console.error('Falha ao atualizar dashboard metrics:', err);
      }
    } finally {
      if (!silencioso) {
        setLoading(false);
      }
    }
  };

  useEffect(() => {
    let ativo = true;

    const carregarInicial = async () => {
      if (!ativo) {
        return;
      }
      await carregarMetricas(false);
    };

    carregarInicial();

    const intervalId = setInterval(() => {
      if (ativo) {
        carregarMetricas(true);
      }
    }, 3000);

    return () => {
      ativo = false;
      clearInterval(intervalId);
    };
  }, []);

  if (loading) {
    return (
      <div className="loading-container">
        <div className="loader-ring"><div></div><div></div><div></div><div></div></div>
        <p>A carregar métricas do painel...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="error-container">
        <h3>⚠️ Falha ao carregar métricas</h3>
        <p>{error}</p>
        <button onClick={carregarMetricas}>Tentar Novamente</button>
      </div>
    );
  }

  if (!metrics) return null;

  return (
    <div className="painel-geral-container">
      <div className="metrics-grid">
        <MetricCard title="Última Hora" data={metrics.ultimaHora} type="hora" />
        <MetricCard title="Últimas 24 Horas" data={metrics.ultimas24Horas} type="dia" />
        <MetricCard title="Últimos 7 Dias" data={metrics.ultimos7Dias} type="semana" />
      </div>
    </div>
  );
}

export default PainelGeral;


