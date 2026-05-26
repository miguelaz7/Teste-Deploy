import React, { useState, useEffect, useCallback } from 'react';
import { getAlertasAtivos } from '../../logica_do_sistema/services/alertasService';
import './Alertas.css';

function PainelAlertas() {
  const [resumo, setResumo] = useState(null);
  const [loading, setLoading] = useState(true);

  const fetchAlertas = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getAlertasAtivos();
      setResumo(data && typeof data === 'object' ? data : null);
    } catch {
      setResumo(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAlertas();
    const id = setInterval(fetchAlertas, 30000);
    return () => clearInterval(id);
  }, [fetchAlertas]);

  const renderSeveridade = (s) => {
    const v = String(s || '').toUpperCase();
    if (v === 'CRITICO') return <span className="badge-severidade badge-alta">Crítico</span>;
    if (v === 'AVISO')   return <span className="badge-severidade badge-media">Aviso</span>;
    return <span className="badge-severidade badge-baixa">Normal</span>;
  };

  return (
    <div className="alertas-card">
      <div className="alertas-card-header">
        <h2>Alertas Ativos</h2>
        <button className="btn-resolver" onClick={fetchAlertas}>
          <svg style={{ marginRight: '6px' }} width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
            <path d="M23 4v6h-6"></path>
            <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path>
          </svg>
          Atualizar
        </button>
      </div>
      <div className="alertas-card-body">
        {loading ? (
          <div className="alertas-empty-state-container">
            <div className="empty-state-icon-wrapper loading">
              <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#3b82f6" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" className="spinning-icon">
                <line x1="12" y1="2" x2="12" y2="6"></line>
                <line x1="12" y1="18" x2="12" y2="22"></line>
                <line x1="4.93" y1="4.93" x2="7.76" y2="7.76"></line>
                <line x1="16.24" y1="16.24" x2="19.07" y2="19.07"></line>
                <line x1="2" y1="12" x2="6" y2="12"></line>
                <line x1="18" y1="12" x2="22" y2="12"></line>
                <line x1="4.93" y1="19.07" x2="7.76" y2="16.24"></line>
                <line x1="16.24" y1="7.76" x2="19.07" y2="4.93"></line>
              </svg>
            </div>
            <h4 className="empty-state-title">A carregar alertas...</h4>
            <p className="empty-state-description">Por favor aguarde enquanto sincronizamos com o sistema de monitorização.</p>
          </div>
        ) : !resumo ? (
          <div className="alertas-empty-state-container">
            <div className="empty-state-icon-wrapper" style={{ backgroundColor: '#fee2e2' }}>
              <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#ef4444" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="8" x2="12" y2="12"></line>
                <line x1="12" y1="16" x2="12.01" y2="16"></line>
              </svg>
            </div>
            <h4 className="empty-state-title">Sem dados disponíveis</h4>
            <p className="empty-state-description">Não foi possível carregar os dados de alertas neste momento.</p>
          </div>
        ) : (
          <>
            {/* Resumo geral */}
            <div className="alertas-metrics-grid">
              <div className="alertas-metric-box">
                <span className="alertas-metric-label">Total Alertas</span>
                <span className="alertas-metric-value">{resumo.totalAlertas ?? 0}</span>
              </div>
              <div className="alertas-metric-box">
                <span className="alertas-metric-label">Severidade</span>
                <span className="alertas-metric-value">{renderSeveridade(resumo.severidade)}</span>
              </div>
              <div className="alertas-metric-box">
                <span className="alertas-metric-label">Janela</span>
                <span className="alertas-metric-value">Últimas {resumo.janelaHoras}h</span>
              </div>
            </div>

            {/* Por motivo */}
            {resumo.porMotivo && Object.keys(resumo.porMotivo).length > 0 ? (
              <div>
                <h3 style={{ marginBottom: '1rem', fontSize: '1rem', fontWeight: 600 }}>Distribuição por Motivo</h3>
                <div className="alertas-list">
                  {Object.entries(resumo.porMotivo).map(([motivo, count]) => (
                    <div key={motivo} className="alerta-item">
                      <div className="alerta-info">
                        <span className="alerta-title">{motivo}</span>
                        <span className="alerta-subtitle">Ocorrências: {count}</span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            ) : (
              <div className="alertas-empty-state-container">
                <div className="empty-state-icon-wrapper">
                  <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#10b981" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
                    <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
                    <path d="m9 11 2 2 4-4" strokeWidth="2"></path>
                  </svg>
                </div>
                <h4 className="empty-state-title">Sem Alertas Ativos</h4>
                <p className="empty-state-description">
                  {resumo.totalAlertas === 0
                    ? 'Não existem alertas ativos nas últimas 24 horas. O teu sistema está a funcionar corretamente.'
                    : 'Sem detalhes de motivos disponíveis.'}
                </p>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}

export default PainelAlertas;